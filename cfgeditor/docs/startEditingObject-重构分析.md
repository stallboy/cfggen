# `startEditingObject` 渲染期副作用：问题诊断、React 原理与重构方案

> 范围：`cfgeditor` 的记录编辑管线（`Record.tsx` 的 `useMemo` → `services/editingObject.ts` → `recordEditEntityCreator.ts`）。
> 状态：**已知反模式，当前未修**（有防护注释，实际风险低）。本文是"根治"方案的探索与教学文档，不是已实施的变更记录。
> 同源问题：`select clearLayoutCache` 已于 commit `9f281d17` 根治，本文是它"在编辑链里的孪生兄弟"。

---

## TL;DR

`Record.tsx` 的 `useMemo`（第 88–129 行）在渲染期间调用 `startEditingObject(...)`，后者**直接变异模块级全局单例 `editState`**（`table / id / editingObject / originalEditingObject / fitView / isEdited` 以及两个闭包）。紧接着同一段 `useMemo` 里 `RecordEditEntityCreator.createThis()` 又**读回**这个刚被写过的全局来构建实体图。

这违反了 React 最核心的契约——**渲染必须是纯函数**。当前"恰好没炸"依赖于一连串隐式不变量（`resetEditingObject` 必须幂等、`recordResult` 在编辑期间内容必须稳定、`update` 引用必须每次变化来驱动重算）。这些不变量没有任何类型或工具把关，是脆弱的巧合。

根本原因不是"放错了钩子"，而是**把编辑会话的状态建模成了一个可变全局单例 + 一棵靠副作用驱动的 React 树**，而 React 树对全局的依赖是不声明、不响应式的。

> ⚠️ **关于"正解"的重要修正（见 §5.0）**：初稿曾把"reducer state（方案 B）"列为正解。但在"一个编辑态 record 含几十个表单、primitive 输入就地改、不引起全树重渲"这个**性能契约**下，朴素方案 B **不可行**（reducer 不可变与"值类零重渲 + 共享引用"根本冲突）。**真正的正解是方案 C（`useSyncExternalStore` + 每会话 store）**——它保留现状的就地变异和二分更新契约，只移除 render 期副作用。B 仅在需要"纯 reducer 可单测"时以其修正形态（分层 state）付出额外复杂度换取。

---

## 1. 问题代码在哪、做了什么

### 1.1 渲染期的变异点

`src/routes/record/Record.tsx:88`

```tsx
const {entityMap, editingObjectRes} = useMemo(() => {
    const entityMap = new Map<string, Entity>();
    let editingObjectRes: EditingObjectRes;
    // ...
    } else {  // isEditing 分支
        const submitEditingObject = () => { mutateRecord(editState.editingObject); };

        // ⚠️ 已知反模式（render 期 mutate 全局 editState）……
        editingObjectRes = startEditingObject(recordResult, update, submitEditingObject);
        const creator = new RecordEditEntityCreator(entityMap, schema, curTable, curId, folds, setFolds);
        creator.createThis();   // 读 editState.editingObject / editState.submitEditingObject
    }
    fillHandles(entityMap);
    return {entityMap, editingObjectRes}
}, [isEditing, curId, schema, recordResult, tauriConf, resourceDir, resMap, curTable,
    mutateRecord, update, folds, setFolds]);
```

### 1.2 被变异的全局单例

`src/services/editingObject.ts:27`

```ts
export const editState: EditState = {   // 模块级单例，import 即共享
    table: '', id: '',
    originalEditingObject: {'$type': ''},
    editingObject: {'$type': ''},        // ← 编辑会话的"活对象"，被就地变异
    fitView: EFitView.FitFull,
    update: dummyFunc,
    submitEditingObject: dummyFunc,
    isEdited: false,
    copiedObject: {'$type': ''},
};
```

`startEditingObject`（第 42 行）做的事：

1. **写闭包**：`editState.update = ...`、`editState.submitEditingObject = ...`；
2. **读旧态**：取 `{table, id, fitView, fitViewToIdPosition}` 和当前 `editState.originalEditingObject / isEdited`；
3. **就地重置或保留**：若同表同 id 且 `originalEditingObject` 与 `recordResult.object` 的克隆深度相等 → 走"保留当前编辑态"早退分支；否则调 `resetEditingObject` 把 `table/id/fitView/originalEditingObject/editingObject/isEdited` 全部覆盖；
4. **返回** `EditingObjectRes`（`fitView / fitViewToIdPosition / isEdited`）给 `useMemo` 的返回值。

注意 `resetEditingObject`（第 86 行）**故意不调** `notifyEditingState()`，注释写明了原因：

> `notifyEditingState → setEditingState` 会触发 "Cannot update component while rendering"，导致 React 丢弃本次更新（视图切换/重选已访问 id 不刷新）。isEdited 的对外通知改由 `Record.tsx` 的 `useEffect([isEditing, recordResult])` 在 render 之后负责。

于是同一份"通知 isEdited"的职责被劈成两半：reset 静默变异全局，真正的对外通知被挪到 `useEffect`（`Record.tsx:135`）。

### 1.3 跨路由也在用同一个全局

`applyNewEditingObject`（`editingObject.ts:258`）直接覆盖 `editState.editingObject` 并触发 `update()`，调用方不止 Record：

- `src/routes/add/Chat.tsx:104` —— AI 聊天生成结果后写入编辑对象；
- `src/routes/add/AddJson.tsx:61` —— JSON 导入写入编辑对象。

这两个面板**默认有一个 Record 同屏挂载并共享同一个全局**，否则 `applyNewEditingObject` 的写入对界面而言接近空操作。这是全局单例跨路由隐式耦合的典型症状。

---

## 2. 教学篇：React 渲染模型与"纯渲染"契约

要理解"为什么不能这么写"，需要先理解 React 渲染管线的几个底层事实。这一节是原理铺垫。

### 2.1 渲染（render）与提交（commit）的两阶段模型

React 把一次更新分成两个阶段：

| 阶段 | 干什么 | 能否中断/放弃 | 允许副作用吗 |
|---|---|---|---|
| **Render**（纯） | 调用组件函数、跑 `useMemo`/`useReducer` reducer，计算出"下一棵虚拟树" | **可以**：可被更高优先级的更新打断、可恢复、也可整段丢弃 | **不允许** |
| **Commit**（副作用） | 把 diff 应用到真实 DOM，然后跑 `useLayoutEffect` → `useEffect` | 同步、不可中断 | 允许（这里是副作用的合法归宿） |

`useMemo` 的函数体、组件函数体、`useReducer` 的 reducer，全部属于 **Render 阶段**。React 保留"随时丢弃一次 render 结果"的权利——这是它实现下面这些能力的前提。

### 2.2 为什么 render 必须是纯函数

"纯函数"在这里的实操定义：

- **相同输入 → 相同输出**：给同样的 props/state/依赖，计算出同样的 JSX/返回值；
- **不读不写外部世界**：不发起网络请求、不订阅、不修改模块级变量、不调用 `setTimeout`；
- **不调用其它组件的 setter**：状态变更只能通过返回新 state（reducer）或在本组件的事件回调/effect 里进行。

为什么把这条当成铁律？因为有四个机制**依赖**于它：

**(a) Reconciliation 与 memoization 的正确性**
React 靠"依赖数组"或 React Compiler 的依赖追踪来决定缓存命中与否。如果 memo 体里偷偷改了外部变量 `editState`，那么：
- 这个 `editState` 既不是 React 能看到的输入，也不会出现在依赖里；
- 当 React 认为"依赖没变，复用缓存"时，全局可能已经被事件回调改过了 → **缓存的实体图与真实编辑对象脱节**；
- 反过来，当 React 丢弃这次 render（见下）时，**全局已经被改了，但没有对应的 UI 提交** → 界面与状态不一致。

**(b) 可中断、可放弃的渲染（Concurrent Rendering）**
React 18+ 可以"开始渲染一棵树 → 被打断 → 恢复或整段丢弃"。典型场景：`useTransition`、Suspense 数据到达、更高优先级更新插队。React 甚至会在 commit 前做 **offscreen 渲染**（把子树提前渲染好放屏幕外）。如果渲染过程有副作用，那么"渲染了但没提交"会留下一个**被污染但不可见的世界**。纯 render 保证"渲染-丢弃"是安全的、无观察后果的。

**(c) StrictMode 的故意双重调用**
开发模式下，StrictMode 会**故意把 render 函数体、`useMemo`、`useReducer` initializer 各调用两次**，目的就是把潜在的"不纯"暴露成可见的 bug（双倍副作用）。一个真正纯的函数被调两次毫无影响；一个会 `editState.x = ...` 的函数被调两次，要么是幂等侥幸没事，要么直接错乱。本项目现在能过 StrictMode，纯粹是因为 `resetEditingObject` 是**幂等覆盖**（写同样的值），不是因为它写法正确。

**(d) React Compiler 的纯函数假设**
本项目 `vite.config.ts` 启用了 `babel-plugin-react-compiler`（README 称 58/58 全覆盖）。Compiler 自动插入 memo，它的**正确性前提就是 render 纯净**：它把"反应式输入"（props/state/依赖）当作缓存的 key。一个被读写的外部全局对 Compiler 是**不可见的**——它无法把 `editState.editingObject` 纳入依赖追踪。于是：
- Compiler 可能基于可见依赖缓存 `{entityMap, editingObjectRes}`，而真实输出还依赖一个不可见全局；
- Compiler 对不纯代码的行为是**未定义**的：可能恰好工作，也可能在某次 Compiler 版本升级后以隐蔽方式崩。
- 换句话说，**一旦 render 不纯，你就失去了 Compiler 给你的那条"等价性保证"**。这是最贵的隐性成本。

### 2.3 "渲染期 setState" 的合法逃生通道（以及它为什么救不了这里）

React 文档里确实有一个"在渲染期间更新状态"的逃生通道：**组件在渲染自己的过程中，可以调用自身那个 `setState`**，React 会丢弃当前渲染、立即用新 state 重渲。它常用于"props 变了，我要把派生 state 同步过来"的场景。

但它有严格边界：

1. 只能调**本组件**的 setter；
2. 必须有条件守卫，否则无限循环；
3. **不能**在渲染 A 组件时去更新 B 组件（或某个全局响应式 store）的状态。

而本项目的 `notifyEditingState → setEditingState` 改的是 **Resso 全局 store**（`store.ts:507`），那是"另一个响应式根"，不是 `RecordWithResult` 自己的 state。这会触发 React 的 **"Cannot update a component while rendering a different component"** 警告，并**丢弃这次更新**——这正是注释里描述、并且不得不用 `useEffect` 兜底的 bug。所以这个逃生通道在这里根本不适用。

### 2.4 把上面的原理映射到这段代码

`startEditingObject` 在 `useMemo`（= Render 阶段）里：

| 原理 | 它的违规 |
|---|---|
| 不写外部世界 | 直接写 `editState.*`（模块级单例，跨组件跨路由共享） |
| 不调别的组件 setter | 被 `resetEditingObject` 间接逼出 `notifyEditingState` → 全局 store（虽已被注释禁掉，但禁掉的代价是把职责劈成两半） |
| 幂等 / 可双调 | 侥幸：`resetEditingObject` 是覆盖式幂等；一旦哪天加了"历史栈 push""计数器 ++"之类非幂等副作用，StrictMode 立刻出错 |
| 依赖即真依赖 | 真实输出依赖 `editState.editingObject`（`createThis` 读它），但它不在依赖数组里；靠 `update` 引用变化这个"代理信号"驱动重算 |
| Compiler 假设纯 | 启用了 Compiler，但这段不纯；Compiler 的缓存保证对该段不成立 |

---

## 3. 当前为什么"还没炸"：靠的是一条承重不变量

整套编辑链之所以今天能工作，依赖于**一条核心不变量**和**几条配套前提**。把这条讲清楚，才知道风险点在哪：

### 承重不变量

> 在一次编辑会话内（同 table+id，`recordResult` 内容稳定），`startEditingObject` **永远走"内容未变 → 保留当前编辑态"的早退分支**，因此 `editState.editingObject`（被各 `onUpdateXxx` 就地变异的活对象）在两次 `update()` 之间**永远不会被替换**。每次 `update()` 触发重渲，`createThis()` 都从同一个活对象重建实体图。

这条不变量之所以成立，是因为：

- `recordResult` 来自 React Query 缓存的服务端原始数据，编辑期间一般不变；
- 早退分支比的是 `editState.originalEditingObject`（会话开始时的**原始快照**），不是活对象本身——所以编辑（只改活对象、不改快照）不会触发 reset。

### 配套前提（任何一个被破坏都可能出 bug）

1. **`resetEditingObject` 必须幂等**。一旦加入非幂等副作用，StrictMode 双调即错。
2. **`update` 引用必须每次都变**。`Record.tsx:75` 的 `update` 把 `updateVersion` 顶上去 → 引用变 → 进 `useMemo` 依赖 → 重算。`updateVersion` 本身**不在**依赖数组里，全靠"引用变化"这个代理。如果有人觉得 `update` "看起来是稳定回调"把它从依赖里删掉，整个重算链就断了（编辑后视图不刷新）。
3. **`recordResult` 在编辑期间内容稳定**。一旦服务端推新数据或 query 被中途 invalidate，`isDeeplyEqual` 失败 → reset → **未保存的编辑被覆盖**（这是"预期行为"还是"丢数据"，取决于产品语义，当前注释把它当预期）。
4. **同一时刻只有一个 Record 挂载**。`editState` 是单例；分屏/多面板（CLAUDE.md 提到的 split layout、`pageConf` fixed pages）若同时挂载两个 Record，两者会互相踩 `editState`。
5. **`isEdited` 的对外通知依赖 `useEffect([isEditing, recordResult])` 的依赖正确**。recordResult 身份变了但内容没变时，`notifyEditingState` 会多余地触发一次。

注意：**这些前提没有一个是类型系统或 lint 能帮你守住的**。它们是隐式约定。

### 同源问题：select clearLayoutCache

`CfgEditorApp` 里 schema/notes 的 React Query `select` 选项曾被用来在 select 回调里 `clearLayoutCache`（= 渲染期变异 query cache），借此驱动 `RecordWithResult` 重渲。这和 `startEditingObject` 是**同一种病**（render 期变异外部响应式状态来驱动渲染）。

那次的处理（commit `9f281d17`）是去掉 `RecordWithResult` 的 `memo`，让 select 回归纯函数。中间还有一次 revert（`970b768`）记录了"移到 useEffect 后 SPA navigate 视图不刷新"的坑——说明这类"靠副作用驱动渲染"的反模式一旦根除，必须把真正的数据流接上，否则会暴露原本被副作用掩盖的刷新断点。**这次重构要吸取同样的教训。**

---

## 4. 根本原因分析（不是"钩子放错了"）

很多人会把这类问题归因为"应该放进 `useEffect`"。这只是症状层面。真正的病根在状态建模：

### 4.1 编辑对象被建模成"可变全局单例"，而不是 React state

`editState.editingObject` 同时承担两个角色：

- **表单的实时可变状态**：所有 `onUpdateXxx` 就地 `obj.push / splice / [key] =` 它；
- **图的唯一真相源**：`createThis()` 从它派生实体图。

在标准 React 里，这两个角色应当是**一份 state**（`useState`/`useReducer`），图是它的**派生**（`useMemo` 依赖该 state）。这里却把它放在模块级全局，于是：

- 全局**不是响应式的**——React 不知道它变了，所以无法据此自动重算；
- 只能用 `update()` → `setUpdateVersion` → `update` 引用变 → `useMemo` 重算这条**旁路**来"假装"它响应式；
- `useMemo` 的声明依赖（`recordResult` 等）和**真实数据依赖**（`editState.editingObject`）不是一回事。

> 一句话：**把非响应式的可变全局当成 state 用，再用副作用把它"焊"回 React 树**——这就是 render 期变异的根。`useEffect` 兜底、`update` 引用代理、`notifyEditingState` 劈成两半，全是这根因派生的补丁。

### 4.2 深层实体树选择"服务定位"而非 props 下发

实体树很深（每节点一个 `EntityForm`），每个节点的编辑回调（`editOnUpdateValues / editOnUpdateFold / …`）都要触达同一份编辑对象。原作者用"模块级单例 + 模块级 `onUpdateXxx(fieldChain)` 函数"当作服务定位器，避免了逐层 prop-drilling。这是合理诉求，但服务定位器天然绕过 React 数据流，是反模式引入的**结构性诱因**。

### 4.3 `startEditingObject` 把两个职责揉在一起

它干了**性质完全不同的两件事**：

- (a) **副作用**：初始化/重置全局编辑会话（设闭包、可能 reset）；
- (b) **近纯查询**：读 `fitView / isEdited` 拼出 `EditingObjectRes` 返回。

(a) 的正确归宿是 effect（每个真实变更提交一次），(b) 的正确归宿是 render/派生。把它们揉进同一个 render 期调用，就逼出了"必须幂等、必须静默、必须 effect 兜底"的三重妥协。`createThis()` 又恰好依赖 (a) 先跑完，于是它也被拖进同一个 `useMemo`——这是第 1.1 节那段**时序耦合**的来源。

---

## 5. 重构方案

> 本节方案排序已根据 §5.0 的性能契约重新调整：**C 为正解，B 朴素形态不可行**。

### 5.0 性能契约：为什么不能无脑上 reducer（必读）

重构前必须先看清现状赖以流畅的两条性能契约。任何方案若破坏它们，几十个表单的输入体验会直接崩。

**契约 1：更新二分——值类编辑不触发重渲。** 观察 `services/editingObject.ts` 各 handler 末尾：

| 类型 | 操作 | 调 `editState.update()`? | Record 重渲? | entityMap 重建? |
|---|---|:---:|:---:|:---:|
| **值类** | `onUpdateFormValues`（primitive 键入）、`onUpdateNote`（注释） | ❌（仅 `notifyEditingState`） | ❌ | ❌ |
| **结构类** | `onUpdateFold` / `onAddItemToArray` / `onAddItemToArrayIndex` / `onDeleteItemFromArray` / `onSwapItemInArray` / `onUpdateInterfaceValue` / `onStructPaste` / `applyNewEditingObject` | ✅ | ✅ | ✅（节点拓扑变了，必须重建） |

primitive 键入时，输入框 UI 由 antd Form 自己管（`initialValue` + `form.setFieldValue`），`editState.editingObject` 被就地改但 React 看不见 → Record 不重渲 → 几十个 `EntityForm` 不重渲 → 输入流畅。这是**几十表单场景的性能生命线**。

**契约 2：就地变异共享引用——entity 闭包自动见最新值。** `recordEditEntityCreator.ts:277` 的 `edit.editObj = obj` 让 entity 闭包持有 `editingObject` 的**子对象引用**。值类更新就地改 `editingObject`，因同一引用，所有 entity 闭包自动看到最新值，提交时 `mutateRecord(editingObject)` 读到全量最新。**不需要重算 entityMap 就能让闭包见新值。**

**推论——为什么 reducer（方案 B）不是无脑正解：** reducer 的规矩是**不可变更新**（每次产出新对象引用），这与两条契约根本冲突：
- 冲突契约 1：`editSession.editingObject` 每次编辑都是新引用 → `useMemo([…, editingObject])` 必然重算 → 几十个节点重建 → 全部 `EntityForm` 重渲 → 连续键入卡顿；
- 冲突契约 2：不可变更新打破共享引用，旧 entityMap 的 `edit.editObj` 指向老子对象，值类更新后闭包读不到新值——除非重算 entityMap（又撞回冲突 1）；
- 附带 antd Form 受控坑：entityMap 若重算，`PrimitiveFormItem` 的 `useEffect([field.value])` 会 `setFieldValue` 与用户正在输入的值打架（光标跳、值被覆盖）。现状正是靠"值类不重算"回避的。

**判据：任何方案只要保留"就地变异 + 选择性重算"，就保留了两条契约；反之则要费力重造。** §C 天然保留，§B 要靠"分层 state"硬重造。

---

### 方案 A：仅把 `startEditingObject` 挪进 `useEffect`（不推荐）

把 reset 的全局变异移到 `useEffect([recordResult, isEditing])`。

- **致命问题**：同一段 `useMemo` 里的 `createThis()` 仍然读 `editState.editingObject`。effect 在 commit **之后**才跑，而 `useMemo` 在 render **期间**就已经用旧/空的全局构建了实体图。首屏渲染的是错误实体图，且 effect 跑完后没有任何依赖变化能触发 `useMemo` 重算——除非再引入一个 state bump（= 一帧延迟 + 一个 `forceUpdate`）。这正是现有防护注释里说的"引入编辑多一帧延迟"。
- **结论**：全局单例和就地变异都在，只是把脏挪了个位置，还搭进去一帧延迟。**不解决根因，不推荐。**

### 方案 B：reducer state（朴素形态 ❌ 不可行 / 修正形态 ⚠️ 可行但绕）

> 见 §5.0：朴素 reducer 破坏性能契约，**不要这么做**。下面给出"修正形态"，仅供追求"纯 reducer 可单测"时参考。

**朴素形态（拒绝）：** `editingObject` 进 `useReducer` 且 `entityMap` 依赖它 → 每次键入全树重算 + antd Form 与输入打架。

**修正形态（分层 state）：** 承认 entityMap 不该随值类更新重算，把依赖拆开——`editingObject` 仍是 reducer state，但 `entityMap` 的 `useMemo` **不依赖 `editingObject`**，而是依赖一个**结构签名**（只有结构类 action 让它变）。值类 dispatch 后 `editingObject` 新引用但结构签名不变 → entityMap 不重算 → 图不重渲（antd Form 继续外包输入）。结构类 dispatch 同时 bump 结构签名 → 重算。下方 reducer 草图对修正形态仍适用。

把"活对象 + 原始快照 + fitView + isEdited"全部变成 `RecordWithResult` 的 state，编辑动作变成纯 reducer。

**形态草图：**

```tsx
type EditAction =
    | {type: 'reset'; recordResult: RecordResult}           // 切记录/后台推新数据
    | {type: 'formValues'; fieldChain: (string|number)[]; values: Record<string, unknown>}
    | {type: 'addArrayItem'; chain: (string|number)[]; item: JSONObject}
    | {type: 'deleteArrayItem'; chain: (string|number)[]; index: number}
    | {type: 'swapArrayItem'; chain: (string|number)[]; a: number; b: number}
    | {type: 'interfaceImpl'; chain: (string|number)[]; newObj: JSONObject}
    | {type: 'note'; chain: (string|number)[]; note?: string}
    | {type: 'fold'; chain: (string|number)[]; fold: boolean; position: EntityPosition}
    | {type: 'paste'; chain: (string|number)[]; copied: JSONObject}
    | {type: 'replace'; obj: JSONObject};                    // Chat/AddJson 用

interface EditSession {
    table: string; id: string;
    editingObject: JSONObject; originalEditingObject: JSONObject;
    fitView: EFitView; fitViewToIdPosition?: EntityPosition;
    dirty: boolean;   // = !deeplyEqual(editingObject, originalEditingObject)
}

function editReducer(s: EditSession, a: EditAction): EditSession {
    // 所有分支：结构化克隆 + 不可变更新，返回新 EditSession
    // fitView/fitViewToIdPosition 随结构变更一起翻新
    // dirty 派生自 editingObject vs originalEditingObject
}
```

- `reset` 用 `useEffect` 在 `recordResult` 真实变化时 dispatch（副作用归于 effect，正确归宿）；
- `createThis()` 不再读全局，改成 `new RecordEditEntityCreator(entityMap, schema, curTable, curId, folds, setFolds, editSession.editingObject).createThis()`——**显式参数**，消除时序耦合；
- 实体图 `useMemo` 的依赖里**真的有 `editSession.editingObject`**，React 自动重算，`update` 引用代理这个脆弱补丁可以删掉；
- `isEdited` 不再需要 `notifyEditingState` 跨组件推送——派生 `editSession.dirty` 即可；若 HeaderBar 等远端组件仍需感知，用 resso store 写一份由 effect 同步过去（**单向、提交后**，合法）；
- `submit` 读 `editSession.editingObject`（state），不再读全局；
- Chat/AddJson 的 `applyNewEditingObject` 改成通过 context/dispatch 触达当前会话的 reducer（见 5 末尾的"跨路由"问题）。

**爆炸半径（blast radius）：**

| 文件 | 改动 |
|---|---|
| `services/editingObject.ts` | `editState` 单例 → 可选保留为兼容垫片，逐步删；12+ 个 `onUpdateXxx` 改写成纯 reducer 分支或 `dispatch` 包装 |
| `recordEditEntityCreator.ts` | `createThis` 改成接收 `editingObject` 参数；`:347` 的 `editState.submitEditingObject` 改成接收 `submit` 参数；`:337` 的 `applyNewEditingObject` 改 dispatch |
| `Record.tsx` | `useMemo` 依赖换真；删 `update`/`updateVersion` 代理；`reset` 进 effect；`submit` 读 state |
| `EntityForm.tsx` / `FlowNode.tsx` | 编辑回调已经走 `edit.editOnUpdateValues` 等闭包，闭包内部改 dispatch 即可，**消费侧改动小** |
| `Chat.tsx` / `AddJson.tsx` | `applyNewEditingObject` 改为往当前会话 dispatch（需一个"当前编辑会话"通道） |

**优点（修正形态）：** render 变纯；reducer 分支可单测（进 vitest）；Compiler 保证恢复；不可变更新让"结构变更"语义更清晰。
**代价（修正形态）：** 改动面较大；所有就地变异改不可变更新（深嵌套要 `structuredClone`/immer）；**额外**要维护"结构签名"的正确性，`useMemo` 故意漏 `editingObject` 依赖（lint 需 disable，是有意约定）；为让重算稳定拿到最新 editingObject，往往还要搭一个 ref——本质是用 reducer 重造方案 C 的"就地变异 + 选择性订阅"。**性价比不如 C**。

### 方案 B'：`useRef` 活对象 + state 版本号（B 的减震中间态）

如果"把所有就地变异改成不可变更新"成本太高，可退一步：

- `editingObjectRef.current` 仍是就地变异的活对象（`onUpdateXxx` 代码几乎不动）；
- 但把"何时重算"从"`update` 引用代理"换成**显式的 `const [editSeq, bumpEditSeq] = useReducer(x => x + 1, 0)`**，并**真的把 `editSeq` 放进 `useMemo` 依赖**；
- `reset` 进 `useEffect`；首帧用 `editSeq===0` 的初始化 reducer 处理；
- `createThis` 读 ref。

> 这比现状只是"把隐式代理换成显式版本号 + reset 归 effect"，**仍未达到纯渲染**（ref 在 render 期被读、被 effect 写，仍是外部可变状态），但消除了"`update` 引用代理"这条最脆弱的隐式链，且为后续 B 的落地铺路。**适合作为分阶段迁移的第一步**，不建议作为终点。

### 方案 C：`useSyncExternalStore` + 每会话 store（✅ 正解）

> 这是本场景的推荐目标架构。它**完整保留 §5.0 的两条性能契约**（就地变异 + 选择性订阅 + 共享引用），只移除 render 期副作用，且 `recordEditEntityCreator` 改动最小。

把 `editState`（模块级单例）重构成一个**每会话一个**的、带订阅能力的 store 实例：

```ts
class EditSessionStore {
    private obj: JSONObject; private orig: JSONObject;
    private version = 0; private listeners = new Set<() => void>();
    subscribe = (l: () => void) => { this.listeners.add(l); return () => this.listeners.delete(l); };
    getSnapshot = (): readonly JSONObject => this.cached;  // 必须：版本不变时返回同一引用
    // 各 mutate 方法：就地改 obj，version++，重建 cached 顶层引用（保证 snapshot 稳定），通知 listeners
}
```

- `RecordWithResult` 用 `useSyncExternalStore(store.subscribe, () => store.structureSignature)` —— **只订阅结构签名**（不是整个 editingObject），render 期只读，合法；
- 值类方法（`mutateField` / `mutateNote`）就地改对象 + bump 值版本，但**不动结构签名** → Record 不重渲 → 几十表单不重算（**契约 1 保留**）；
- 结构类方法（add/delete/swap/fold/impl/paste/replace）就地改对象 + bump 结构签名 → Record 重渲 → entityMap 重算；重算时调 `store.getEditingObject()` 拿最新（仍共享引用，**契约 2 保留**）；
- 编辑动作是 store 方法（事件回调里调）——**副作用在 handler，合法**；reset 移到 store 构造 + 响应 `recordResult` 变化的 effect；
- 每个会话独立实例 → **天然支持分屏多 Record**（解决前提 4）；
- Chat/AddJson 注入/寻址当前会话的 store 实例即可。

**优点：** 副作用彻底移出 render；就地变异代码改动小（store 方法内部仍可就地改）；多会话隔离；比 B 的 reducer 改动量小。
**难点：** 订阅"结构签名"（number/string）比订阅深对象简单得多——`getSnapshot` 返回基本类型，天然引用稳定，没有无限重渲循环风险。真正要设计的是：① store 实例的生命周期（随会话创建/销毁，最好挂在 `RecordWithResult` 的 `useRef`/context 上，卸载即销毁）；② Chat/AddJson 如何寻址"当前编辑会话的 store 实例"（一个 context 或一个"当前会话注册表"）；③ reset 的触发从 render 期移到 effect 后，首帧 entityMap 的初始化路径。

### 方案 D：分阶段落地（推荐执行方式）

不论目标定 B 还是 C，都建议**分阶段、每阶段可独立验证**，避免重蹈 `970b768` 那种"一次性移除副作用导致刷新断链"的覆辙：

1. **阶段 1（低风险，纯收益）**：`update` 引用代理 → 显式 `editSeq` 并入依赖（B' 的第一步）；把"reset 时刷 isEdited"的 `useEffect` 依赖收紧，消除多余通知。不改单例、不改就地变异。先让隐式链变显式。
2. **阶段 2**：`createThis(editingObject)` 显式参数化，去掉"render 内 `startEditingObject` 必须先于 `createThis`"的时序耦合。此时 render 仍有一次写（reset），但读已显式。
3. **阶段 3**：reset 进 `useEffect`；render 内不再有写。此时要么接受一帧延迟（用阶段 1 的 `editSeq` 在 effect 里 bump），要么切到方案 C 的订阅式（无延迟）。
4. **阶段 4**：`editState` 单例 → 每会话实例（C）或 reducer state（B），解决多面板耦合与跨路由 `applyNewEditingObject`。

每阶段完成后**必须跑第 6 节的验证清单**。

---

## 6. 验证清单（重构后必跑，参照 `9f281d17` 的实机验证口径）

编辑交互是这类重构最容易回退的地方，以下每条都要在**实机**（`pnpm tauri dev` 或 web dev）走一遍：

- [ ] 进入编辑模式（浏览 → 编辑切换）
- [ ] 切换记录（重选已访问 id、新 id）
- [ ] 原始字段键入（int/long/float/str/text/bool）
- [ ] 数组增/删/前插/上下移（list<struct>、list<interface>、list<primitive>）
- [ ] map 增删
- [ ] 折叠/展开（含内嵌 `<>` 展开）
- [ ] 切换 interface 的 impl
- [ ] 复制/粘贴结构（structCopy / structPaste）
- [ ] 保存（提交 `addOrUpdateRecord`，含 alt+s 热键，确认仅触发一次）
- [ ] 清空（setDefaultValue）
- [ ] AI 聊天生成 → 写入编辑对象（Chat.tsx）
- [ ] JSON 导入 → 写入编辑对象（AddJson.tsx）
- [ ] `isEdited` 指示（脏标记）随编辑/重置正确翻动
- [ ] StrictMode 双调无异常（dev 控制台无 "Cannot update component while rendering"）
- [ ] fitView 行为（FitFull / FitId 定位）正常
- [ ] 后台推新数据（query invalidate）时 reset 行为符合产品预期（是否丢未保存编辑）
- [ ] （若支持）分屏两 Record 互不踩状态

---

## 7. 结论与建议

- **现状判断**：`startEditingObject` 渲染期变异全局 `editState` 是真实的 React 反模式，但当前**没有已知功能 bug 驱动**（不像 select clearLayoutCache 当时直接导致 navigate 不刷新）。靠"幂等 reset + `update` 引用代理 + `useEffect` 兜底"三重妥协维持工作。
- **隐性成本**：丢失 React Compiler 的等价性保证；声明依赖 ≠ 真实依赖；多面板/跨路由耦合；核心编辑链无法单测。
- **推荐**：**不必为修而修**，但应在以下任一触发条件出现时按方案 D 分阶段推进——
  1. 出现"编辑后视图偶发不刷新 / StrictMode 报错 / 编辑被意外重置"等真实 bug；
  2. 要支持分屏多 Record 同屏编辑（前提 4 必然破裂）；
  3. 想给编辑链加单元测试（reducer/store 化是前提）；
  4. React Compiler 升级后出现疑似缓存陈旧的怪象。
- **目标架构**：方案 **C（useSyncExternalStore + 每会话 store）** 为正解——保留现状的就地变异和二分更新性能契约（§5.0），只移除 render 期副作用，`recordEditEntityCreator` 改动最小。仅当追求"纯 reducer 可单测"时才考虑方案 B 的修正形态（分层 state），并接受额外复杂度。两者都把副作用移出 render。

---

## 附录 A：相关文件与提交

- 代码：
  - `src/routes/record/Record.tsx:88`（问题 `useMemo`）、`:135`（`notifyEditingState` 兜底 effect）
  - `src/services/editingObject.ts:27`（`editState` 单例）、`:42`（`startEditingObject`）、`:86`（`resetEditingObject`）、`:258`（`applyNewEditingObject`）
  - `src/routes/record/recordEditEntityCreator.ts:49`（`createThis` 读全局）、`:347`（读 `submitEditingObject`）
  - `src/routes/add/Chat.tsx:104`、`src/routes/add/AddJson.tsx:61`（跨路由写全局）
  - `vite.config.ts:11`（React Compiler 已启用）
- 提交链：
  - `9f281d17` —— 同源问题 select clearLayoutCache 的根治（RecordWithResult 去 memo）
  - `970b768` —— 上述根治过程中误移副作用导致 navigate 不刷新的 revert 记录（教训）
  - `d8648e21` —— 标注本处 `startEditingObject` 已知反模式（防护注释来源）

## 附录 B：React 原理速查（给后续维护者）

- **两阶段模型**：Render（纯、可弃）→ Commit（副作用、同步）。
- **纯渲染契约**：相同输入相同输出；不读写外部世界；不跨组件 setState。
- **为什么纯**：可中断渲染（Concurrent）、可放弃渲染（offscreen/Suspense）、StrictMode 双调、Compiler 缓存保证——四者都以此为前提。
- **render 期 setState 逃生通道**：只能对本组件自身 state、必须有守卫；**不能**用于全局响应式 store 或别的组件。
- **`useSyncExternalStore`**：把"外部可变状态"合法接入 render 的官方接口；难点是 `getSnapshot` 的引用稳定性契约。
- **React Compiler**：自动 memo，正确性依赖 render 纯净；不纯代码 = 失去保证、行为未定义。

## 附录 C：可变全局单例的业界用例与合理性

> 回答："业界什么时候会用可变全局单例？这是合理的使用场景吗？"
> 结论先行：**可变单例本身不是反模式，反模式是"在 React 渲染期同步变异它来初始化" + "裸单例无订阅层被组件直接读"。** cfgeditor 用可变单例是合理的，要改的是它接入 React 的方式。

### C.1 业界合理使用可变单例的典型场景

这类对象都是**可变、命令式、长生命周期**的，UI 是它们的观察者/投影，而非来源。React 官方把这类归为 "escape hatches / imperative world"。

1. **命令式引擎/编辑器实例**：Monaco `editor`、CodeMirror `EditorState`、ProseMirror `EditorView`、TipTap、Three.js `WebGLRenderer`、Mapbox/Leaflet `map`、ReactFlow `reactFlowInstance`、`HTMLCanvasElement` 的 2D/WebGL context。它们存放在 `useRef`/context/模块单例里，React 组件订阅其事件/transaction。
2. **客户端状态库的底层实现**：Redux / Zustand / Jotai / React Query / SWR / Apollo——**底层都是模块级可变 store + 订阅机制**，通过 `useSyncExternalStore` 或自有 hook 以响应式方式接入 React。可变单例正是这些库的实现材料。
3. **长生命周期会话状态**：WebSocket/IM 客户端、协作编辑的 CRDT 文档（Y.js/Automerge `Doc`）、富文本文档模型——状态跨组件树、跨路由、跨重挂载，放进某个组件的 `useState` 反而不对（卸载即丢失）。
4. **领域聚合根（DDD 风格）**：把有状态领域对象建模成对象，UI 是其投影。

**共性**：它们几乎都**配一个订阅/响应式接入层**。裸可变单例 + 组件在 render 期直接读写字段，才是病。

### C.2 cfgeditor `editState` 的合理与不合理

**合理的内核（应保留）：**
- "可变文档模型 + 一组命令式编辑动作 + 深层实体树通过闭包触达" = 命令式领域模型 + UI 投影，和 Monaco/ProseMirror 同一思路；
- 就地变异 + 共享引用让几十表单零重渲（§5.0 契约 2），这是命令式模型对 reducer 的性能优势——**换成 reducer 反而失去这个优势**；
- antd Form 外包实时输入、editingObject 作提交草稿，分层合理。

**不合理的部分（真正要修的）：**
1. **模块级真·全局**（不是每会话实例）→ 跨路由（Chat/AddJson 共用它）、潜在分屏多 Record 互踩。Monaco 每个 `<Editor>` 一个实例，从不在 app 级共享一个 editor。
2. **render 期初始化**（`useMemo` 里 `reset`）→ 应发生在构造时、或 effect、或显式 `openDocument()`，不该在 render 里偷偷调。
3. **无正式订阅层** → 靠 `update()` 引用代理这个旁路通信，"谁变了、谁要重算"是隐式的。

### C.3 结论

可变单例在 cfgeditor 场景**是合理的模式选择，甚至优于 reducer**（性能契约使然）。要改的不是消灭它，而是把它从"模块级全局 + render 期变异 + 无订阅"升级成"每会话实例 + 构造/effect 期初始化 + `useSyncExternalStore` 订阅"。**这正是方案 C**——它承认可变单例的合理性，只修它接入 React 的方式。换句话说：cfgeditor 当前的痛苦不来自"用了可变单例"，而来自"用得不够彻底"——既没把它包成有订阅的 store，又让它越界在 render 期初始化。

## 附录 D：方案 C 已实施（2026-07-11）

重构按 §5 方案 D 分阶段落地，共两个 commit：

- `f6d27484` 阶段 0+1：新增 `services/editingSession.ts`（`EditingSession` 类 + 模块级活动会话 holder）、`services/clipboard.ts`（app 级剪贴板）、`editingSession.test.ts`；`Record.tsx` 用 lazy ref 创建 session + `useSyncExternalStore` 订阅 `structureVersion` + useMemo 依赖结构版本 + reset 进 effect（合并删除原 notify 补偿 effect）+ holder 注册；`recordEditEntityCreator.ts` 接收 session/editingObject 参数，闭包改 session 方法；`editingObject.ts` 的 `applyNewEditingObject` 临时改 holder 桥。
- `9f542399` 阶段 2：Chat/AddJson 直接调 `getCurrentEditingSession()?.replaceEditingObject`，删除整个 `editingObject.ts`（模块级 `editState` 单例、`startEditingObject`、`resetEditingObject`、`notifyEditingState`、所有 `onUpdateXxx` 退出）。

**实际落地的关键决策**（与 §5 设计的细化）：
- **寻址用模块级 holder**（`getCurrentEditingSession`/`setCurrentEditingSession`），非 context——子创父供场景下 context 退化为 holder 加一层（§5 决策1 验证）。
- **clearLayout 事件时同步触发**：session 持有 `onStructureChange` 回调，每个结构方法末尾同步调（组件侧 `pathnameRef` 注入读 ref 的稳定闭包）。不能用 effect 补救——React Query 不因 queryFn 闭包里 nodes 变化重执行，effect 会晚一帧布局错乱。
- **首帧靠 lazy ref 构造期 reset**（零延迟），recordResult 后台变化靠 effect `maybeReset`（幂等）。
- **剪贴板 app 级**独立模块，保住跨记录复制粘贴。
- **两条 isEdited 通道都保留**：store 通道 → HeaderBar 实时脏标记；`editingObjectRes.isEdited` 返回值 → `useEntityToGraph` layout 缓存策略。后者在纯值类编辑期间的 staleness 是**有意保留的 quirk**（`editingSession.ts` 注释标明，勿当 bug 修）。

**验证**：tsc（src）干净、oxlint 干净、vitest 202 测试绿（含 `editingSession.test.ts` 纯逻辑单测 + 确定性 fuzz）；实机通过（primitive 键入零重渲、结构编辑重渲一次、StrictMode 无 "Cannot update component while rendering" 警告、layout 重取正常、编辑全流程 + Chat/AddJson 写入 + 跨记录粘贴）。

**结果**：render 期变异消除，React Compiler 等价性保证恢复；编辑管线状态从"模块级可变单例 + render 期副作用"变为"每会话 store 实例 + useSyncExternalStore 订阅"，性能契约（值类零重渲 / 结构类共享引用）完整保留。

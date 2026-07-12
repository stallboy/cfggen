# Undo/Redo：业界原理与 cfgeditor 现状

> 本文是 [`undo-redo-设计.md`](./undo-redo-设计.md) 的**背景依赖**。设计文档里的每个决策——为什么选快照栈、为什么不能套 redux-undo、为什么值类编辑难 undo——都建立在这两部分知识上：**业界的 undo/redo 方案谱系**与**cfgeditor 编辑管线的特殊架构**。
>
> 如果你已熟悉 undo/redo 通用方案和 EditingSession，可跳到设计文档。配套阅读：[`状态管理-总结与演进.md`](./状态管理-总结与演进.md)（EditingSession / resso / useSyncExternalStore 全景）、[`perf-optimization.md`](./perf-optimization.md)（性能契约背景）。

---

## 引言：为什么 undo/redo 是个难题

表面看 undo 就是"回到上一个状态"。难点在于：**程序的当前状态只有一个，而 undo 需要"过去的状态"——过去已被覆盖。**

所以 undo 的本质是**额外存储历史信息**。而"存什么"——存全量快照？存逆操作？存差异 patch？——决定了方案，也决定了内存、正确性、实现复杂度的权衡。没有"最好的" undo 方案，只有"最适合当前架构"的。

本文先讲通用方案谱系（第一、二部分），再看成熟产品怎么做（第三部分），最后剖析 cfgeditor 的编辑管线为什么特殊、为什么通用方案不能直接套（第四部分）。

---

## 第一部分：Undo/Redo 的四大模式

### 模式 A：快照栈 / Memento

**原理**：在"检查点"（每次操作前后）存一份状态的**完整深拷贝**。undo = 弹出栈顶快照，把当前状态恢复成它。

**数据结构**：
```
Stack<Snapshot>   // Snapshot = 状态的完整深拷贝
pointer = 栈顶
undo: pointer--, 恢复 stack[pointer]
redo: pointer++, 恢复 stack[pointer]
新编辑: 截断 stack[pointer+1:], push 新快照, pointer++   // 分叉
```

**内存**：O(栈深 × 状态大小)。两份相邻快照之间即使只改一个字段，也无任何共享——每份独立深拷。

**优点**：实现最简、天然正确、**无需为每种操作写逆逻辑**。正确性几乎不依赖操作种类。

**缺点**：内存大（状态越大、栈越深越明显）。

**代表**：Excalidraw、浏览器 history、大多数"够用就行"的简单 undo。

**适用**：状态不大、操作种类多（写逆逻辑不划算）、对内存不敏感的场景。

---

### 模式 B：命令模式 / Command

**原理**：把每个操作封装成对象 `{do(), undo()}`——`do` 执行正向变更，`undo` 执行**逆变更**。undo = 调用栈顶命令的 `undo()`。

**数据结构**：
```
Stack<Command>   // Command 含"足够计算逆操作的信息"
undo: 栈顶.command.undo()
redo: 栈顶.command.do()（或 redo()）
```

**内存**：O(操作参数)——只存"做了什么"的参数（如 `{type:'delete', index:3, item: <深拷贝>}`），不存全量状态。**四模式中最省。**

**优点**：内存小；可表达复杂逆（如"选择性 undo"——撤销某一步而不影响其后）。

**缺点**：
- **每种操作都要手写逆逻辑**，操作种类多时易错。
- **依赖"地址稳定"或"针对当前态重算地址"**：如果逆操作靠"在 index 3 插回 item"，那 index 3 必须仍指向原位置。一旦中间有别操作移动了下标，逆操作会打到错误元素。

**代表**：Monaco 的 `EditStack`、ProseMirror 的 `step.invert(doc)`、大多数专业编辑器。

**适用**：状态大、操作种类有限且逆逻辑清晰、需要精细 undo 控制。

> **"地址稳定"陷阱**是命令模式最大的坑。线性 undo（双栈、无分叉）下，undo 时状态恰好回到"该操作执行后"的形态，用原参数逆推通常仍有效。但一旦涉及分支 undo 或操作间有耦合，地址就会失配。这也是为什么文本编辑器普遍用"带 position map 的 step"（ProseMirror）而非裸命令。

---

### 模式 C：JSON Patch / 结构化 diff

**原理**：不存全量、不手写逆，而是记录每次变更的**差异**。RFC 6902 定义了 6 种 op：`add` / `remove` / `replace` / `move` / `copy` / `test`，每个 op 带 JSON Pointer 路径（如 `/fields/3/name`）。undo = 应用**反向 patch**。

**数据结构**：
```
baseline: 一份全量快照（独立）
Stack<{ forward: Patch, inverse: Patch }>
undo: applyPatch(current, inverse)
redo: applyPatch(current, forward)
```
patch 由 `compare(old, new)` 自动生成，inverse 由 `createInverse(forward)` 自动生成。

**内存**：O(1 baseline + N 小 patch)——比快照栈省得多（每次只存变化的 delta）。

**优点**：**自动生成、自动逆**（不需手写逆逻辑）、省内存。

**缺点**：
- 需 diff 引擎（`fast-json-patch` / `immutable-json-patch`）。
- **路径互译**：JSON Pointer（`/a/b/0`）要和应用数据的寻址方式互译。
- **就地 vs 不可变 apply 的语义**：`fast-json-patch` 默认就地改原对象（会污染共享引用），需显式深拷或切 immutable 模式。

**代表**：`fast-json-patch`、`immer` 的 `produceWithPatches`、`immutable-json-patch`。

**适用**：树状/对象状态、操作种类多、想省内存、愿意承担 diff 引擎与路径互译成本。

---

### 模式 D：CRDT / Operation-based

**原理**：把数据建模为 CRDT 类型（`Y.Map` / `Y.Array` / `Y.Text`），所有变更都是 CRDT operation。CRDT 天然可合并、无冲突，因此天然支持**多人协同**，且每个 op 自带足够信息做选择性 undo。

**数据结构**：CRDT 文档 + `UndoManager`（按 transaction origin 分作用域，`captureTimeout` 合并连续 op）。

**内存**：每栈项几字节（极省）。

**优点**：协同、选择性 undo、内存极小。

**缺点**：**必须把整个数据模型重写为 CRDT 类型**；概念与实现复杂度极高。

**代表**：Yjs `UndoManager`、Automerge。

**适用**：多人协同编辑。**单用户场景属于过度工程**。

---

### 四模式速查

| 模式 | 存什么 | 内存 | 要写逆逻辑? | 关键风险 | 代表 |
|---|---|---|---|---|---|
| **A 快照栈** | 全量状态深拷贝 | O(深×大小) | 否 | 内存 | Excalidraw |
| **B 命令** | 操作参数 + 逆 | O(参数)，最省 | **是**（每种操作） | 地址失配 | Monaco / ProseMirror |
| **C Patch** | baseline + N 个 diff | O(1 baseline + N patch) | 否（自动逆） | 路径互译、就地 apply 污染 | fast-json-patch |
| **D CRDT** | CRDT op | 极省 | 否 | 重写数据模型 | Yjs |

---

## 第二部分：三大工程难题（无论哪种模式都要面对）

### 难题 1：粒度 / coalescing（合并）

**问题**：用户连续键入 "hello" 是 5 次按键，该是 **1 步** undo 还是 **5 步**？

逐键入栈体验恶劣（打错一个词要按 5 次 undo）。业界三种合并策略：
- **逐键**：精确，但用户体验差。
- **词边界 / 语义边界**：自然语言友好，但对结构化输入不适用。
- **时间窗（debounce）**：连续编辑在阈值内（典型 **500ms**）合并为一步；超时或被其他操作打断则关闭当前合并组。

**共识**：文本/值输入用**时间窗 + 失焦 + 换字段**合并；结构操作（增/删/移动）天然离散，1 动作 1 步。Lexical 的 text undo coalescing、ProseMirror 的 `newGroupDelay`、Yjs 的 `captureTimeout(500ms)` 都是时间窗方案。

### 难题 2：内存 / 结构共享

**问题**：快照栈对大对象每份独立深拷 → 内存爆炸。

**缓解手段**：
- **栈深封顶**（如 50），超限丢弃最旧。
- **改用 patch 路线**（模式 C）：1 份 baseline + N 个小 patch。
- **结构共享**（immutable.js / immer）：不可变数据结构下，新旧版本共享未变子树，快照成本 O(log n) 而非 O(n)。但**要求整个数据模型不可变**——与"就地变异"架构冲突。

**关键事实**：结构共享只在"不可变数据结构"下免费。如果项目用的是普通可变 JSON 对象（如 cfgeditor 的 `JSONObject`），structuredClone 出来的快照之间**没有任何共享**。

### 难题 3：提交/持久化后的 undo 语义

**问题**：已经保存到磁盘/后端的改动，能不能 undo？

**业界共识**：**已持久化的改动不靠内存 undo 回滚**——那属于版本控制 / 数据库回滚的另一层。内存 undo 只管"未提交的本地编辑"。提交 = undo 栈的天然边界：
- Monaco `setValue()` 销毁 undo 栈。
- Excalidraw 提交 = 持久化增量后栈重置。
- ProseMirror 本身无内建持久化。

### 难题 4（附）：视图状态的独立性

**问题**：undo 数据时，光标 / 选中 / 视口焦点要不要一起回滚？

- **Monaco 范式**：把 inverse selection 随 edit 入栈，undo 时恢复光标。
- **多数工具**：数据 undo，视图尽量稳定（不强制跳焦点）。

视图状态（焦点/视口/折叠）通常是"瞬时意图"，不是"数据历史"的一部分。

---

## 第三部分：成熟产品怎么做（可借鉴的范本）

### 3.1 ProseMirror —— 命令式领域模型 + 树文档（与 cfgeditor 最像）

**形态**：`EditorState`（命令式、可变）+ `doc`（树状文档）+ transactions（事件驱动的变更）。这与 cfgeditor 的 `EditingSession`（命令式）+ `editingObject`（树状 JSON）+ 编辑方法（事件驱动）**同构**。

**机制**：
- 变更的单位是 **step**，自带 **position map**，可 `step.invert(doc)` 生成逆 step。
- `HistoryState{done, undone}` **双栈**；undo = 弹栈顶 event、invert 各 step、应用、移入 undone。
- step 按 `newGroupDelay`（时间窗）+ 邻接性聚合成 **event**（一个 undo 单元）——这就是 coalescing。
- undo 后若有新编辑，丢弃 `undone`（**分叉**）。

**可借鉴**：双栈、分叉、时间窗合并、"invert 而非回滚"。
**它为什么用命令而非快照**：文档可能很大，全量快照贵；且 step 的 position map 能处理"并发位置映射"（undo 一个 step 时，其后的 step 已改变位置，map 帮助定位）。

### 3.2 CodeMirror 6 —— inverted effect + 排除控制

**机制**：history 默认只追踪 document + selection 变更；**其他 state 更新要可 undo，须用 "inverted effect"**（把副作用的逆绑进历史）。`Transaction.addToHistory = false` 可把特定事务**排除出历史**。

**对应 cfgeditor**：我们的"值类编辑不 bump"恰似 CM6 里"非默认追踪的 state"——**要 undo 必须显式登记，不能指望框架自动捕获**。

### 3.3 Monaco —— inverse selection + setValue 清栈

**机制**：`ITextModel` 内部 `EditStack` 用命令模式；`pushEditOperation` 接受 **inverse selection**（undo 时恢复光标）；`setValue()` **销毁 undo 栈**，`pushEditOperations`/`setModel` 保留。

**对应 cfgeditor**：inverse selection → undo 时恢复视口焦点（视作 P1）；setValue 清栈 → 提交后清栈。

### 3.4 Lexical / Yjs —— coalescing 与 captureTimeout

Lexical 的 text undo coalescing 把多次键入合成一个 undo step；Yjs `UndoManager.captureTimeout` 默认 500ms 时间窗合并。两者确立了"500ms 时间窗"这个业界惯例。

### 3.5 Excalidraw —— 快照栈 + 提交重置

不可变 `elements + AppState` 快照入栈；`captureUpdate` 控制何时记录；提交 = 持久化增量后栈重置。是模式 A（快照栈）的现代范本。

---

## 第四部分：cfgeditor 编辑管线现状

> 这一节讲清楚"为什么通用 undo 方案不能直接套用 cfgeditor"。设计文档的每条硬约束都源于此。

### 4.1 状态分源：editingObject 属于哪一类

cfgeditor 把应用状态按**性质**分成六类，各用最合适的工具管理（详见 [`状态管理-总结与演进.md`](./状态管理-总结与演进.md)）：

| 状态源 | 工具 | 例子 |
|---|---|---|
| 服务端状态 | React Query | 一条 record、schema、引用列表 |
| URL 状态 | react-router | 当前 table/id、是否编辑模式 |
| 客户端 app 状态 | **resso** | 主题、布局参数、isEditMode |
| **会话/表单状态** | **EditingSession** | **一个 record 的编辑草稿（editingObject）** |
| 局部 UI 状态 | useState | 输入框值、下拉开关 |
| 持久化状态 | localStorage / YAML | 设置、导航历史 |

**关键**：编辑对象 `editingObject` 属于"会话状态"，cfgeditor 为它单造了 `EditingSession`，**没有放进 resso**。原因见 4.4–4.5。

### 4.2 底座：useSyncExternalStore 与两条铁律

`resso` 和 `EditingSession` 接入 React 的底层都是 `useSyncExternalStore`——React 官方提供的"把外部可变状态以响应式方式接进 React"的接口：

```ts
const value = useSyncExternalStore(subscribe, getSnapshot);
//  subscribe: 外部变了时调 cb 通知 React
//  getSnapshot: React 用它读当前值，靠"引用是否变了"判断要不要重渲
```

**两条铁律（违反即 bug）**：
1. **`getSnapshot` 必须返回引用稳定的值**：同一份状态没变时，两次调用必须 `===` 相等，否则 React 以为变了 → 无限重渲。基本类型（number/string）天然满足；返回对象/数组则必须缓存。
2. **`subscribe` / `getSnapshot` 引用要稳定**：每次 render 传给 hook 的这两个函数应是同一引用。

> 理解铁律 1 是理解"为什么 undo 不能简单地把 editingObject 当 snapshot 返回"的关键——见 4.4。

### 4.3 resso：per-key 订阅

resso 是 cfgeditor vendored 的 ~150 行轻量库。精髓：**store 的每个 key 各自是一个独立的外部 store**。

```ts
const {server} = useMyStore();    // 只订阅 server 这一个 key
const {maxImpl} = useMyStore();   // 只订阅 maxImpl
setServer('y');                   // 只通知订阅 server 的组件重渲，不碰 maxImpl 的订阅者
```

这就是为什么 cfgeditor 敢把几十个配置字段塞进一个大 `StoreState`——改一个字段不会让全树重渲。

### 4.4 EditingSession：就地变异 + 结构版本号（undo 的主战场）

`EditingSession`（`services/editingSession.ts`）是**每条 record 编辑态一个**的可变 store 实例。它**不进 resso**，因为 resso 的"不可变 + 赋新值触发订阅"语义与编辑场景的**两条性能契约**直接冲突（4.5）。

它的接入方式：`getSnapshot` 返回一个基本类型 **`structureVersion`（number）**——满足铁律 1（number 天然引用稳定）。

```ts
class EditingSession {
    private editingObject: JSONObject;      // 树状 JSON，就地变异（不换引用）
    private structureVersion = 0;           // 快照：number
    getStructureVersion = () => this.structureVersion;   // getSnapshot
    // ...
}
// Record.tsx
const structureVersion = useSyncExternalStore(session.subscribe, session.getStructureVersion);
```

**两类编辑通道**（这是理解 undo 设计的核心）：

| | 值类编辑 | 结构类编辑 |
|---|---|---|
| 方法 | `updateFormValues` / `updateNote` | add/delete/swap/fold/impl/paste/replace |
| 改 editingObject | 就地改 | 就地改 |
| bump structureVersion | **否** | **是** |
| emit（通知 Record 重渲） | **否** | **是** |
| 结果 | Record 不重渲、entityMap 不重建 | Record 重渲、entityMap 重算 |

**为什么这样分**？因为两类编辑的"重渲代价"天差地别——见 4.5。

### 4.5 性能契约：值类零重渲 + 共享引用

这是 `EditingSession` 存在的全部理由，也是 undo 设计必须守住的红线。

**契约 1（值类零重渲）**：一个 record 可能有几十个表单字段。用户连续键入时，几十个 `EntityForm` **不能重渲**，否则卡顿。键入时输入框 UI 由 antd Form 自己管（`initialValue` + 内部 store），`editingObject` 被就地改但 React 看不见（不 bump）→ Record 不重渲 → 表单不重渲 → **输入流畅**。

**契约 2（共享引用）**：值类编辑就地改 `editingObject`，所有 entity 闭包持有它的**子对象引用**（`edit.editObj = obj`），改完闭包自动见最新值，**不必重算整棵实体图**；提交时 `submit()` 读到全量最新。

**为什么 editingObject 不能进 resso**：resso 是"赋新值触发订阅"。若 `editingObject` 进 resso，每次键入 `store.editingObject = newObj` → 引用变 → 所有依赖重算 → 几十表单重渲 → **与契约 1 直接冲突**。

`EditingSession` 的解法：**就地变异（不换引用）+ 结构版本号（选择性 bump）**。值类不 bump（契约 1）；结构类 bump → entityMap 重算时读最新 `editingObject`（共享引用，契约 2）。

> **对 undo 的含义**：undo 绝不能在"键入"路径上 bump（会破坏契约 1）。但 undo 本身是用户主动触发的离散动作（不是连续键入），付一次重算代价可接受——这是 undo 能走 bumpStructure 的前提（设计文档 §4.5/§4.6 详）。

### 4.6 一次编辑的全链路（值类 vs 结构类）

**值类（改一个 primitive 字段）**：
```
用户键入
  → antd Form 受控更新输入框（Form 自管值，React 不参与）
  → onValuesChange → session.updateFormValues(...)
  → 就地改 editingObject[key]
  → notifyEditingState() → resso editingIsEdited 变 → HeaderBar 显示脏标记
  → 【不 bump、不 emit】
  → Record 不重渲 → 几十 EntityForm 不重渲 → 键入流畅（契约 1）
```

**结构类（加一个数组项）**：
```
右键"添加" → session.addArrayItem(...)
  → obj.push(item) 就地改
  → structureChange → bumpStructure():
      → structureVersion++                          【快照变】
      → onStructureChange() → queryClient.removeQueries(['layout',...])  【清布局缓存】
      → notifyEditingState()                         【resso 脏标记】
      → emit() → Record 重渲
  → Record 的 useSyncExternalStore 被通知 → useMemo 重算 entityMap
  → 新 RecordEditEntityCreator 遍历最新 editingObject（共享引用，契约 2）
```

### 4.7 EntityForm 与 useSyncFieldValue（外部值 → Form 的同步通道）

`EntityForm`（`flow/EntityForm.tsx`）每个节点一个 `Form.useForm()`，Form 内部 store 是 source of truth，`editingObject` 只是镜像。`Form.Item` 的 `initialValue` **仅在字段首次注册时生效**——切换 impl / 同 key 复用时新 `initialValue` 被忽略 → 表单显示旧值。为修这个 antd 已知问题（[ant-design/issues/56102](https://github.com/ant-design/ant-design/issues/56102)），引入了命令式同步 hook：

```ts
// EntityForm.tsx:150
function useSyncFieldValue(form, name, value) {
    useEffect(() => { form.setFieldValue(name, value); }, [name, value, form]);
}
```

它在 Primitive / Array / Interface 字段上都挂了。

> **对 undo 的关键意义**：**结构类** undo 走 `bumpStructure` → entityMap 重算 → 新 `field.value` → `useSyncFieldValue` 自动同步 Form（无需改 EntityForm 接口）。**值类** undo 不 bump（性能契约延伸：键入不 bump，撤销键入也不 bump），需额外的 form registry 直接 `setFieldValue`（设计文档 §4.5/4.9）。Form.List 子项对 `setFieldValue` 的响应需实测（设计文档 §7）。

### 4.8 recordEditEntityCreator：editObj 子引用注入

`recordEditEntityCreator.ts` 的 `createThis()` 递归遍历 `editingObject`，把每个子对象的**引用本身**（非拷贝）塞进 `entity.edit.editObj`，并以 `fieldChain`（`(string|number)[]`，数字段是数组下标）为地址构造回调闭包。所以整棵 entityMap 的 `editObj` 都是同一棵 `editingObject` 树里的子引用——就地改任何一个 `editObj` 即改 `editingObject`。

> **对 undo 的含义**：
> - 快照**不能存 editObj 引用**——它会被后续就地变异污染，必须 `structuredClone` 深拷独立化。
> - **不能用"按 fieldChain 重放逆操作"**——数组下标会随增删/swap/前插整体平移，旧地址失效。

### 4.9 提交链路与脏基准

```
session.submit() → mutate(editingObject) → 后端 addOrUpdateRecord
  → onSuccess → invalidateAllQueries() → record refetch
  → recordResult 变 → effect 调 maybeReset
  → 提交后 server 数据 ≠ 旧 originalEditingObject → 走"真 reset"分支
      → 重置 originalEditingObject + editingObject + bumpStructure
```

- `originalEditingObject`（脏比较基准，`getIsEdited = !deepEqual(editing, original)`）**仅**在构造期与 `maybeReset` 真 reset 时赋值。
- **submit 本身不动 originalEditingObject**（只 `mutate`）——重置由异步 refetch 回来的 `maybeReset` 完成。
- 提交 = **脏基准的天然重置点**。

> **对 undo 的含义**：submit 是异步 kick-off（成败要等网络）。undo 的"清栈"语义不能挂在 submit 调用时（提交失败会丢历史），要挂在提交成功后（设计文档 §6.1）。

### 4.10 session 生命周期

`<RecordWithResult key={curTableId-curId}>`：切 record（curId 变）→ 组件 unmount → session 实例销毁 → 编辑态与（未来的）undo 栈随之销毁。切回原 record → 新 session、空栈。分屏两 Record：各自 session、互不干扰。

模块级 `currentEditingSession` 是"当前活动会话"的单值指针，供 Chat/AddJson（Splitter 兄弟路由，非 Record 子树）寻址写入——它**非响应式**，且同一时刻只指向一个 session（分屏歧义，设计文档 §4.9 处理）。

### 4.11 小结：通用 undo 库为何不适用，undo 必须内建在哪

把第一部分的四模式 + 第三部分的范本，对照 cfgeditor 现状：

- **redux-undo / zundo / use-undo**：都假定"不可变 reducer store `{past, present, future}`"。cfgeditor 的 `EditingSession` 是**就地变异 + `getSnapshot` 返回 number**，与 `{past,present,future of T}` **根本冲突**。强行套用要重写 EditingSession 为不可变 reducer，破坏性能契约。
- **immer `produceWithPatches`**：`produce` 要求不可变更新，与就地变异 + 共享引用闭包冲突。只能当 diff 引擎的替代（等价于 fast-json-patch），无额外收益。
- **纯快照栈**：可行，但必须通过 `bumpStructure` 驱动 React（`getSnapshot` 返回 number，不能返回 state 对象）。
- **命令模式**：可行，但 4.8 的"下标平移"使"按地址重放"不可行。
- **CRDT / Yjs**：需把 `editingObject` 重写为 `Y.Map`/`Y.Array`，推翻 editObj 引用语义，过度工程。

**结论**（设计文档 C1）：undo 引擎必须**内建在 `EditingSession` 内**，复用现有的 `bumpStructure`/`emit` 通道驱动 React，不能外包给不可变 store 库。`done`/`undone` 双栈 + 分叉 + coalescing 的**思路**可借鉴 ProseMirror，但不得引入其 store 抽象。

---

## 附：深入阅读
- [`状态管理-总结与演进.md`](./状态管理-总结与演进.md) —— EditingSession / resso / useSyncExternalStore 的完整教学，本文 4.2–4.10 的深度展开。
- [`perf-optimization.md`](./perf-optimization.md) —— 性能契约的背景与测量方法。
- [`undo-redo-设计.md`](./undo-redo-设计.md) —— 基于本文的设计稿。

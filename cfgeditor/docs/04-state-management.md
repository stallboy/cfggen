# 状态管理：resso / EditingSession / useSyncExternalStore 怎么分工

> 这篇讲 cfgeditor 的状态管理：状态分哪几类、每类用什么工具、resso 与 EditingSession 底层为何都是 `useSyncExternalStore`、编辑对象为什么不进 React state。通用原理 → 本项目现状 → 数据流 → 优化方向。
>
> **不讲**：URL / API / React Query 那条数据主线（→ [`05-url-api-reactquery.md`](./05-url-api-reactquery.md)）、undo 快照栈与视口语义的展开（→ [`06-undo-redo.md`](./06-undo-redo.md) / [`07-fitview.md`](./07-fitview.md)）、目录分层（→ [`02-directory-structure.md`](./02-directory-structure.md)）。
>
> **配套**：一条编辑的全程串联见 [`03-data-lifecycle.md`](./03-data-lifecycle.md)。
>
> **引用约定**：正文用「文件 + 导出符号」定位（如 `store.ts` 的 `setMaxImpl`），不用行号——行号随重构漂移，符号稳定。

---

## 一、本质问题：React 应用里"状态"到底有几种？

把"状态管理"等同于"选一个 Redux/Zustand"是窄化理解。一个真实应用的**状态来自好几个性质截然不同的源**，混用同一个工具是绝大多数 bug 的根。

按"谁拥有它、是否异步、生命周期、是否需要响应式"分类：

| 状态源 | 谁拥有 | 异步? | 生命周期 | 典型例子 |
|---|---|:---:|---|---|
| **服务端状态** | 后端 | ✅ | 与请求/缓存绑定 | 一条 record、schema、引用列表 |
| **URL 状态** | 浏览器地址栏 | ❌ | 跨刷新/跨分享 | 当前 table/id、是否编辑模式 |
| **客户端 app 状态** | app 全局 | ❌ | app 会话内 | 主题、布局参数、当前打开的面板 |
| **会话/表单状态** | 某次交互 | ❌ | 进入→离开该交互 | 一个 record 的编辑草稿 |
| **局部 UI 状态** | 单个组件 | ❌ | 组件挂载期 | 输入框值、下拉开关 |
| **持久化状态** | 磁盘/存储 | ✅(写) | 跨重启 | 上次的设置、导航历史 |

**关键判据**：服务端状态首选专门的 server-state 库（React Query/SWR），**不要**塞进全局 store；URL 能承载的状态优先放 URL（可分享、可前进后退）；只有"客户端独有的、需要跨组件响应式广播的"才进全局 store；"这次交互独有、性能敏感的"用专门的会话 store。

> 一句话：**先分清状态属于哪一类，再选工具。选错源 = 选错工具 = 后面全是补丁。**

---

## 二、主流状态方案谱系（一句话定位）

| 方案 | 本质 | 适用 | 不适用 |
|---|---|---|---|
| `useState`/`useReducer` | 组件内响应式 | 局部 UI、简单派生 | 跨组件共享 |
| Context | 跨组件"穿洞"传值 | 低频变更的依赖注入（主题/路由） | 高频变更（全树重渲） |
| Redux | 单一不可变 store + action | 大型、可时间旅行/审计的应用 | 小项目过重 |
| Zustand | 极简外部 store + hook | 通用客户端状态 | — |
| **resso** | **每 key 一个外部 store 的工厂** | **轻量客户端状态（本项目选型）** | 服务端状态 |
| Jotai/Recoil | 原子化、依赖追踪 | 细粒度派生多的场景 | 概念成本 |
| **React Query/SWR** | **服务端状态缓存 + 同步** | **任何后端数据（本项目选型）** | 纯客户端 UI 状态 |
| `useSyncExternalStore` | **把"外部可变状态"接进 React 的官方接口** | 订阅式外部 store、命令式引擎 | 需自己管订阅/snapshot |

注意最后一行：`useSyncExternalStore` 不是"又一个状态库"，而是**所有外部 store（包括 resso、Zustand、React Query 底层）接入 React 的共同底座**。看懂它就看懂了 cfgeditor 的整个响应式骨架——这是第四节的主题。

---

## 三、cfgeditor 当前状态全景

| 状态源 | 工具/文件 | 作用域 | 响应式机制 | 代表字段 |
|---|---|---|---|---|
| 服务端数据 | React Query（`services/queryClient.ts`） | app | query 缓存 + hook 订阅 | `schema`/`record`/`refs`/`prompt`/`layout` |
| 客户端 app 状态 | **resso store**（`store/store.ts`） | app 全局单例 | **useSyncExternalStore per-key** | `server`/`refIn`/`nodeShow`/`dragPanel`/`isEditMode` |
| 编辑会话 | **EditingSession**（`services/editingSession.ts`） | 每会话实例 | **useSyncExternalStore（structureVersion）** | `editingObject`/`structureVersion`/`fitView`/`undoStack` |
| URL/路由 | react-router（`useLocationData`） | URL | `useLocation` | `curPage`/`curTableId`/`curId`/`edit` |
| 持久化 | `store/storage.ts` | localStorage + Tauri YAML | 非响应式（`setPref` 同步） | resso 的可持久化子集 |
| 局部 UI | `useState`/`useRef` | 组件 | React 内置 | `folds`/`inputValue` |
| 剪贴板 | `services/clipboard.ts` | app 模块单例 | 非响应式（命令式） | `copiedObject` |
| 导航历史 | `domain/historyModel.ts`（resso 持有实例） | app | 经 resso `history` key | `History` 实例 |
| 编辑脏标记指针 | resso store | app | useSyncExternalStore per-key | `editingCurTable`/`editingCurId`/`editingIsEdited` |

注意最后两行和 EditingSession 的关系：**EditingSession 持有真正的编辑对象，resso 只持有一个"当前在编辑谁、是否脏"的指针**。这是第六节要展开的设计要点。

---

## 四、一把钥匙：`useSyncExternalStore`

cfgeditor 的两个核心状态机制（resso、EditingSession）**底层是同一个 API**。理解它，整个 app 的响应式就通了。

### 4.1 它解决什么问题

React 的 render 必须是纯函数（不读不写外部世界）。但"命令式外部状态"（WebSocket、编辑器实例、全局 store、模块单例）天然可变。`useSyncExternalStore` 是 React 官方提供的**合法逃生通道**——把外部可变状态以响应式方式接进 render：

```ts
const value = useSyncExternalStore(subscribe, getSnapshot, getServerSnapshot?);
//  subscribe: (cb) => 取消订阅   —— 外部变了时调 cb 通知 React
//  getSnapshot: () => 快照        —— React 用它读当前值，并靠"引用是否变了"判断要不要重渲
//  getServerSnapshot: SSR 时的快照 —— SPA 可不传，或传同一个 getSnapshot
```

### 4.2 两条铁律（违反即 bug）

1. **`getSnapshot` 必须返回"引用稳定"的值**：同一份状态没变时，两次调用必须返回 `===` 相等的值。否则 React 以为变了 → 无限重渲。**基本类型（number/string）天然满足**；返回对象/数组则必须缓存。
2. **`subscribe`/`getSnapshot` 引用要稳定**：每次 render 传给 `useSyncExternalStore` 的这两个函数应是同一个引用（否则每次都重新订阅，带来无谓的订阅抖动）。

> 顺带一提：resso 用的是 `use-sync-external-store/shim`（`resso.ts` 顶部 import）而非 `react` 直接导出版——shim 是给 React <18 的 polyfill，React 18+ 有内置版，行为一致；resso 作为通用库选 shim 兼容旧版，本项目用 React 19 不受影响。

### 4.3 resso = 每个 key 一个 useSyncExternalStore（`store/resso.ts`）

resso 是个 ~150 行的 vendored 库。它的精髓是：**store 的每个 key 各自是一个独立的外部 store**。

```ts
const store = resso({ server: 'x', maxImpl: 10 });

// 组件 A
const { server } = useMyStore();   // render 时访问 store.server → 订阅 server 这一个 key
// 组件 B
const { maxImpl } = useMyStore();  // 订阅 maxImpl

setServer('y');   // 只 triggerUpdate(server) → 只 A 重渲
setMaxImpl(20);   // 只 triggerUpdate(maxImpl) → 只 B 重渲
```

实现上（`resso.ts` 的 Proxy handler）：读 `store.foo` 时调 `useSyncExternalStore(state.foo.subscribe, state.foo.getSnapshot, state.foo.getSnapshot)`；写 `store.foo = v` 时更新值并 `triggerUpdate`。`getSnapshot` 返回基本类型或稳定引用 → 天然满足铁律 1。

> **这就是为什么 cfgeditor 敢把几十个配置字段塞进一个大 `StoreState`**：resso 的订阅是 per-key 的，改 `maxImpl` 不会让只读 `server` 的组件重渲。不存在"大 store = 全树重渲"的代价。

### 4.4 EditingSession = 手写的一个 useSyncExternalStore store（`services/editingSession.ts`）

```ts
class EditingSession {
    private structureVersion = 0;                    // 快照：基本类型，天然稳定
    private listeners = new Set<() => void>();
    subscribe = (l) => { this.listeners.add(l); return () => this.listeners.delete(l); };
    getStructureVersion = () => this.structureVersion;  // 引用稳定的箭头函数属性
    // ...
}
```

`Record.tsx`：
```ts
const structureVersion = useSyncExternalStore(session.subscribe, session.getStructureVersion);
```

和 resso 完全同构——只不过 resso 自动给每个 key 生成这套，EditingSession 是为"编辑会话"这一个特殊场景手写的（精简示意；真实类还含 undo/redo 与值类合并，见 §6.5）。**两者遵守同一对铁律**：`subscribe`/`getStructureVersion` 是实例箭头函数属性（绑定 `this` + 引用稳定，满足铁律 2）；`getStructureVersion` 返回 number（满足铁律 1）。

> ⚠️ 维护提示：`subscribe`/`getStructureVersion`（以及 `canUndo`/`canRedo`/`getEditingObject` 等被作为引用传出的读取器）必须保持**实例箭头函数属性**写法。若改成 prototype 普通方法，`this` 会丢、引用也不稳定，`useSyncExternalStore` 立刻崩。

---

## 五、resso store（`store/store.ts`）

### 5.1 StoreState 的三类字段

一个 `StoreState`（`store.ts` 的 `StoreState`）混装了三类内容：

| 类别 | 字段 | 是否持久化 |
|---|---|---|
| **配置** | `server`/`aiConf`/`themeConfig`/`nodeShow`/`tauriConf`/各种 depth/maxNode | `cfgeditor.yml`（共享） |
| **UI 偏好** | `isEditMode`/`dragPanel`/`pageConf`/`imageSizeScale`/`query` | `cfgeditor.yml` 或 `cfgeditorSelf.yml` |
| **运行时指针** | `editingCurTable`/`editingCurId`/`editingIsEdited`/`history`/`resMap`/`resourceDir` | **不持久化**（`notSaveKeySet`） |

持久化三分（`store.ts` 的 `prefSelfKeySet` / `notSaveKeySet` / `getPrefKeySet`）：
- **共享** `cfgeditor.yml`：`getPrefKeySet` = 全部键 − 个人键 − 不持久化键。
- **个人** `cfgeditorSelf.yml`：`prefSelfKeySet` = `{curPage, curTableId, curId, query, isEditMode, imageSizeScale, dragPanel, aiConf}`（`aiConf` 含 apiKey，单独放个人文件）。
- **不持久化** `notSaveKeySet` = `{history, resMap, resourceDir, editingCurTable, editingCurId, editingIsEdited}`（每次启动重算/重取）。

> 依赖方向：键集由 `store` 在初始化时经 `registerPrefKeySet` 注册给 `storage`，**消除 `store ↔ storage` 循环依赖**（storage 不反向 import store）。

### 5.2 setter 的标准两段式 + 声明式缓存失效

几乎每个 setter 都是同一模式（以 `setMaxImpl` 为例，`store.ts`）：
```ts
export function setMaxImpl(value: number | null) {
    if (value !== null) {                     // ① 改 resso（触发订阅者重渲）；判 null 不判 truthy，免误杀 0
        store.maxImpl = value;
        setPref('maxImpl', value.toString()); // ② 持久化（localStorage + debounce 写 YAML）
    }
}
```

**只有两步。没有第三步主动清 React Query 缓存**——这是关键设计转变。缓存失效现在是**声明式**的（Query Key Factory）：

- `useEntityToGraph`（`flow/useEntityToGraph.ts`）的 layout query 把所有"影响布局/拓扑"的字段编进 `queryKey`：
  - `layoutKeys = pickLayoutKeys(nodeShow)`：布局算法、间距等**视觉**字段；
  - `topologyKeys = { maxImpl, refIn, refOutDepth, maxNode, recordRefIn…, tauriConf }`：影响**节点集合**的拓扑字段。
- 改其中任何一个 → `queryKey` 变 → React Query 自动判缓存失效 → 重布局。

于是 store 回归**纯状态容器**：setter 只管"改值 + 写盘"，缓存协调由 queryKey 声明式完成。**新增一个拓扑 setting，只需在 `topologyKeys` 里登记一行，不必改任何 setter、不必手动清缓存。** 好处：缓存策略与状态变更解耦，不存在"改了值忘了清缓存 / 清错缓存"的 bug 面。

> 两个有意不清缓存的特例（均有注释说明）：`setQuery`——query 只用于节点高亮、不进 layout，故既不清缓存也不进 queryKey（否则每次搜索都让所有可见图在 worker 里重跑得到相同布局）；`setNodeShow` 改纯颜色字段——`pickLayoutKeys` 不含颜色，queryKey 不变 → 命中缓存不重跑 ELK。

### 5.3 持久化写入：防抖 + 串行化（`store/storage.ts`）

- **双存储**：web 用 localStorage；Tauri 下启动时读 YAML → 灌入 localStorage（`readPrefAsyncOnce`），`setPref` 写 localStorage + 防抖写 YAML。
- **防抖 300ms**（`storage.ts` 的 `WRITE_DEBOUNCE_MS`）：`navTo` 一次连发 3 个 `setPref`，防抖合并成一次 YAML 全量重写。
- **串行化**（`storage.ts` 的 `writeChain`）：串成 Promise 链，避免并发写同一文件损坏/丢字段。
- **关窗立即落盘**（`main.tsx` 的 `onCloseRequested`）：`preventDefault` → `await flushAllPrefsAsync()` → `destroy()`，绕过 debounce 直接串行写**个人 + 共享**两份偏好（`flushAllPrefsAsync` = `Promise.all([saveSelfPrefAsync(), saveSharedPrefAsync()])`），避免关窗丢失 pending 写入。

### 5.4 `useMyStore` vs `getMyStore`

```ts
export function useMyStore() { return store; }   // 在组件里调，建立 per-key 订阅
export function getMyStore() { return store; }   // 在非组件（事件回调/util）里直接读，不订阅
```

同一个 store 代理，区别只在于调用场景：组件里要响应式就用 `useMyStore`；纯工具函数/事件回调里偶尔读一下当前值用 `getMyStore`（不引发订阅）。resso 的订阅发生在组件 render 期的属性访问时（Proxy.get 里调 `useSyncExternalStore`），非 render 期访问 hook 会抛错、被 resso 的 try/catch 降级为裸读——所以"不订阅"是调用时机的自然结果。

---

## 六、EditingSession（`services/editingSession.ts`）

### 6.1 为什么编辑对象不进 resso / useState？

这是整个编辑管线最关键的设计决策。表面看 `editingObject` 是"客户端状态"，进 resso 天经地义。但 cfgeditor 为它单造了一个 `EditingSession` 类，原因是**两条性能契约**：

- **契约 1（值类零重渲）**：一个 record 可能有几十个表单字段。用户连续键入时，几十个 `EntityForm` **不能重渲**，否则卡顿。键入时输入框 UI 由 antd Form 自己管（`initialValue` + `setFieldValue`），`editingObject` 被就地改但 React 看不见 → Record 不重渲 → 表单不重渲 → 输入流畅。
- **契约 2（共享引用）**：值类编辑就地改 `editingObject`，所有 entity 闭包持有它的子对象引用（`edit.editObj = obj`），改完自动见最新值，**不必重算整棵实体图**；提交时 `submit()` 读到全量最新。

resso 是"不可变 + 赋新值触发订阅"语义。若 `editingObject` 进 resso：每次键入 `store.editingObject = newObj` → 引用变 → 所有依赖重算 → 几十表单重渲，且 antd Form 的 `useEffect([field.value])` 会 `setFieldValue` 与用户正在输入的值打架（光标跳、值被覆盖）。**与契约 1 直接冲突。**

EditingSession 的解法：**就地变异（不换引用）+ 结构版本号（选择性 bump）**：
```ts
// 值类（键入 primitive / 改 note）：就地改，不 bump
updateFormValues(...) { /* obj[key] = conv(v) */ this.notifyEditingState(); }  // 只刷脏标记，不 emit
// 结构类（增删/swap/fold/impl/paste/replace）：就地改 + bump
addArrayItem(...) { obj.push(item); this.structureChange(pos); }  // bump + emit → Record 重渲
```

`Record.tsx` 只订阅 `structureVersion`（number）。值类编辑不 bump → Record 不重渲 → 表单不重渲（契约 1 保住）；结构类编辑 bump → Record 重渲 → entityMap 重算时读最新 `editingObject`（共享引用，契约 2 保住）。

> **教学点**：同一个 app 里，resso 服务"低频、可不变即不变"的配置状态；EditingSession 服务"高频、必须就地变、选择性通知"的会话状态。**选对工具**比"统一用一个 store"重要得多。

### 6.2 EditingSession 与 resso 的桥：`editingIsEdited`

EditingSession 自持 `getIsEdited()`，但 HeaderBar（显示"未保存"脏标记的组件）在**另一个路由子树**，拿不到这个会话实例。解法是搭一座桥：

```ts
// editingSession.ts —— 值类/结构类编辑都调
notifyEditingState() { setEditingState(this.table, this.id, this.getIsEdited()); }
// store.ts
setEditingState(...) { store.editingCurTable = ...; store.editingCurId = ...; store.editingIsEdited = ...; }
```

脏标记通过 **resso 的 `editingIsEdited` 广播给 HeaderBar**，而真正的编辑对象留在 EditingSession 里。这是"会话状态（精确）+ app 指针（广播）"的分工。

存在**两个 isEdited 通道**，各有用途、有意保留：
1. resso `editingIsEdited` → HeaderBar 实时脏标记；
2. `editingObjectRes.isEdited`（`editingSession.ts` 的 `getEditingObjectRes`）→ `useEntityToGraph` 决定 layout 的 `staleTime`（脏了就每帧重取布局，干净就 5min 缓存）。

通道 2 在纯值类编辑期间不刷新（entityMap 不重算 → `getEditingObjectRes` 不被调），是有意为之：值类不改拓扑，布局不必重算，layout 继续走 5min 缓存是**正确**的。勿当 bug 修。

### 6.3 输入净化：`$refs` 剥离

EditingSession 的 `prepareEditingObject`（构造期 / `maybeReset`）和 `replaceEditingObject`（Chat/AddJson/funcClear 写入）都会调 `deleteRefsInPlace` 剥离后端附加的 `$refs`（`FieldRef[]`，"哪些记录引用了本记录"的展示元数据）。`$refs` 是运行时引用关系、非可编辑数据，不剥离会污染提交载荷并让 `getIsEdited` 误判 dirty（基准 `originalEditingObject` 已净化）。注意它不是 JSON Schema 的 `$ref`（引用指针）——命名上的相似是历史包袱。

### 6.4 undo/redo：快照栈 + 视口语义 + 值类合并

EditingSession 内置一套 per-session 的 undo/redo（`UndoStack`，`domain/undoStack.ts`），核心是**快照栈**而非命令模式：

- **三段栈**：`baseline`（初始/提交后态）/ `done`（操作后快照，末尾=最近）/ `undone`（已 undo 可 redo）。`capture` 清空 `undone`（分叉，与所有编辑器一致）。`maxDepth=50` 防大 record 内存膨胀。
- **快照 = `structuredClone(editingObject)` + 视口语义**：每个快照带 `undoFitView`（undo/redo 到此态后视口该怎么动）和 `anchorId`（KeepStable 的锚点节点）。clone 是必须的——editingObject 会被就地变异，存引用等于存一个会被改的活对象。
- **视口四语义**（`EFitView`，`domain/entityModel.ts`）：结构操作 → `KeepStable` + 锚点（undo 时锚点屏幕坐标不动）；整体替换/reset → `FitFull`（重新认识全图）；值类/Form.List 长度变 → `NoChange`（视口不动）；新增/定位 → `FitId`。`pickViewportAction`（`flow/layout/viewportMath.ts`）据此决定 `fitView` / `setViewport` / noop。
- **值类 coalescing**：同字段连续键入在 500ms 窗口内合并为一步 undo；换字段 / blur / 结构操作关闭当前组。per-key O(1)——只做"字段标识比较 + clearTimeout/setTimeout"，**严禁每键 clone/遍历**（否则啃掉契约 1 的零重渲性能）。结构操作前置 `beforeStructuralChange` 会先 flush 未 capture 的键入，避免与结构步混在一个快照。
- **接入 React 的边界**：Record **不订阅** `canUndo`/`canRedo`（它们的翻转会触发重渲，破坏契约 1）；而是由 `ctrl+z / ctrl+y` 热键回调实时判断（栈空 / 非编辑态直接放行 → 不误杀 input 原生 undo）。右键菜单的 undo/redo 项用 `disabled` 惰性求值。

> 命名坑：实例字段叫 `undoStack` 而非 `undo`，是为了避免与 `undo()` 方法同名（TS 不允许同名的属性与方法）。
>
> 展开细节见专档：[`06-undo-redo.md`](./06-undo-redo.md)、[`07-fitview.md`](./07-fitview.md)。

### 6.5 提交与基准重置

`submit()` 只触发 `mutate(editingObject)`（React Query mutation）；**真正的基准重置在网络成功之后**，由 `onCommitSuccess()` 完成：

- 提交是异步的，成败要等网络。若在 `submit()` 发请求时就重置基准，提交失败会**丢 undo 历史**且脏标记误报"无未保存"。故重基准挂在 mutation 的 `onSuccess`（`Record.tsx` 的 `addOrUpdateRecordMutation`）：成功 → `session.onCommitSuccess()` → `resetBaselines()`（重 `originalEditingObject` 归脏比较 false + `undoStack.setBaseline` 清栈设新基准）。`originalEditingObject` 与 undo baseline 共享同一份 clone，省一次 `structuredClone`。
- 新建记录提交成功后 id 由 `NEW_RECORD_ID` 变为真实 id → `navigate` 改 `RecordWithResult` 的 `key` → 旧 session unmount（栈随实例销毁）→ 新 session 用真实 id 构造。
- `maybeReset`（切记录 / 后台推新数据时由 effect 调）幂等：同表同 id 且内容深相等 → 早退保留编辑态；否则重置 + 新基准 + 清栈。

### 6.6 设计立场：可变单例 + 订阅层为何合理

EditingSession 是一个可变、长生命周期的领域对象，UI 是它的观察者。这和 Monaco editor、CodeMirror `EditorState`、ProseMirror、Three.js renderer 同类——命令式领域模型。Redux/Zustand/React Query 底层同样是模块级可变 store + 订阅机制。**可变单例本身不是反模式**，它是这些库的实现材料。

真正的边界是**接入 React 的方式**：

| 做法 | 判定 |
|---|---|
| 每会话独立实例（状态隔离）+ `useSyncExternalStore` 订阅（合法接入）+ 副作用只在事件回调/effect（render 纯净）+ 就地变异（性能契约） | ✅ EditingSession 的形态 |
| 裸可变单例被组件在 render 期直读/变异 + 无订阅层（React 看不见变化，靠引用代理旁路通信）+ render 期副作用（违反纯净，丢 React Compiler 等价性保证、StrictMode 靠幂等侥幸） | ❌ 反模式 |

也就是说，痛苦不来自"用了可变单例"，而来自"用得不够彻底"——既没把它包成有订阅的 store，又让它越界在 render 期被读写。EditingSession 承认可变单例在本场景（性能契约使然，优于 reducer）的合理性，只把接入方式做对：保留就地变异，移除 render 期副作用，用 `useSyncExternalStore` 让"外部可变状态"合法响应式接入。

> 分层要点：EditingSession 把"编辑对象（就地变异，服务性能契约）"和"undo 快照栈（不可变 clone，服务正确性）"两种语义分层——热路径走就地变异，undo/redo 走独立 clone 栈。这与 Monaco `EditorState` / ProseMirror"可变文档 + 不可变 history"同构。

---

## 七、数据流实例：一次编辑的全链路

把前面所有概念串起来。追踪用户在 record 编辑模式里**改一个 primitive 字段**：

```
用户键入
  → antd Form 受控更新输入框（Form 自管值，React 不参与）
  → antd 触发 onFinish/onValuesChange
  → entity.edit.editOnUpdateValues(values)
  → session.updateFormValues(schema, values, fieldChain)        【EditingSession】
      → 定位子对象 + 类型转换 + 就地改 editingObject[key]
      → touchValueCoalesce(fieldKey)                            【值类 undo 合并：500ms 窗口】
      → notifyEditingState()
          → setEditingState(table, id, isEdited)                【resso store】
              → store.editingIsEdited 变
              → resso triggerUpdate(editingIsEdited)
              → HeaderBar（订阅者）重渲 → 显示 "unsaved" 脏标记
      → 【不 bump structureVersion、不 emit】
  → Record 的 useSyncExternalStore 未被通知 → Record 不重渲
  → 几十个 EntityForm 不重渲 → 键入流畅（契约 1）
```

对比**结构类编辑**（加一个数组项）：

```
右键"添加" → session.addArrayItem(default, chain, pos)           【EditingSession】
  → beforeStructuralChange(): flushValueCoalesce()              【先固化未 capture 的键入】
  → obj.push(item)  就地改
  → structureChange(pos): capture(KeepStable, 锚点)             【入 undo 快照栈】
  → bumpStructure({fitView: FitId, position}):
      → structureVersion++                                       【快照变】
      → onStructureChange() → queryClient.removeQueries(['layout', pathname, 'e'])  【React Query 编辑态缓存失效】
      → notifyEditingState()                                     【resso 脏标记】
      → emit() → listeners 通知 Record
  → Record 的 useSyncExternalStore 被通知 → Record 重渲
  → useMemo 重算（structureVersion 依赖变）:
      → new RecordEditEntityCreator(entityMap, ..., session, editingObject).createThis()
      → entityMap 重建（闭包持有最新子对象引用，契约 2）
      → editingObjectRes = session.getEditingObjectRes()  (fitView=FitId)
  → useEntityToGraph 收到新 entityMap + editingObjectRes
      → layout query 因缓存被清 → 重取 → FitId 定位到新节点
```

提交：
```
alt+s / 保存 → session.submit() → mutate(editingObject)          【React Query mutation】
  → 后端 addOrUpdateRecord
  → onSuccess: session.onCommitSuccess() → resetBaselines()      【重脏基准 + undo baseline，清栈】
             + invalidateAllQueries()                            【服务端缓存失效，重取最新 record】
```

这几条链路串起了 **EditingSession × resso × React Query × antd Form × react-router(URL)** 五个状态源。看懂它，就看懂了 cfgeditor 的运行时。

---

## 八、优化方向

> 只列**尚未做、值得做**的方向。一~七节是现状描述。

### 8.1 holder 单值 → 分屏寻址

`getCurrentEditingSession` / `setCurrentEditingSession`（`editingSession.ts`）是 app 级模块单例、非响应式。每会话实例已隔离了**状态**（分屏 A/B 的 `editingObject` 不互踩），但"寻址当前会话"仍是单值：分屏同挂两个 Record 时，Chat/AddJson 只能命中最后注册的会话。方向：若要支持分屏，holder 改成按面板/tab 寻址（`Map<panelKey, EditingSession>`），Chat/AddJson 按所在面板取对应 session。

### 8.2 `isEdited` 三处判定的收敛评估

当前脏判定散在三处：resso `editingIsEdited`、`EditingSession.getIsEdited()`、`editingObjectRes.isEdited`。它们各有正当用途（广播 / 会话内查询 / layout 缓存策略），不是冗余。但“同一语义三份计算”是可读性负担，且 `getIsEdited` 每次值类编辑跑 O(n) 深比较（可缓存脏标记同时收敛此处性能与可读性）。**不急于合并通道**——性能契约使它们有意分离。

### 8.3 `StoreState` 逻辑切片 / clipboard 响应式

- **StoreState 切片**：几十字段混装配置/UI/导航/编辑指针/历史。resso per-key 订阅已消除"大 store 全树重渲"代价，**这不是性能问题**，纯可读性。若后续膨胀，可按职责拆几个 resso store或分节注释。低优先。
- **clipboard 模块单例**：`services/clipboard.ts`（`copiedObject`）app 级可变单例、命令式、非响应式。当前无 UI 需响应它，命令式够用。若将来要 UI 响应剪贴板状态再纳入 store。

### 8.4 编辑交互验证清单（任何编辑链改动后必跑，实机）

编辑交互是最容易回退的地方。以下每条都要在**实机**（`pnpm tauri dev` 或 web dev）走一遍：

- [ ] 进入编辑模式（浏览 → 编辑切换）
- [ ] 切换记录（重选已访问 id、新 id）
- [ ] 原始字段键入（int/long/float/str/text/bool）
- [ ] 数组增/删/前插/上下移（list<struct>、list<interface>、list<primitive>）
- [ ] map 增删
- [ ] 折叠/展开（含内嵌 `<>` 展开）
- [ ] 切换 interface 的 impl
- [ ] 复制/粘贴结构（structCopy / structPaste）
- [ ] **undo/redo**（ctrl+z / ctrl+y；结构操作的 KeepStable 锚点屏幕不动；整体替换 FitFull；值类合并为一步）
- [ ] 保存（提交 `addOrUpdateRecord`，含 alt+s 热键，确认仅触发一次）
- [ ] 清空（setDefaultValue / funcClear）
- [ ] AI 聊天生成 → 写入编辑对象（Chat.tsx）
- [ ] JSON 导入 → 写入编辑对象（AddJson.tsx）
- [ ] `isEdited` 指示（脏标记）随编辑/重置/undo 回到 baseline 正确翻动
- [ ] StrictMode 双调无异常（dev 控制台无 "Cannot update component while rendering"）
- [ ] fitView 行为（FitFull / FitId 定位 / KeepStable / NoChange）正常
- [ ] 后台推新数据（query invalidate）时 reset 行为符合产品预期（是否丢未保存编辑）
- [ ] （若支持）分屏两 Record 互不踩状态

### 8.5 演进原则

- **状态源只增不减是正常的**——不要为了"统一"硬把性质不同的源合并。
- **以最高设计标准为首要判据**（架构一致性 / 纯度 / 依赖方向），不为修改便利度妥协；"已能工作/已有测试/改动成本"是次要考量，不构成否决设计正解的理由。
- **优先补测试和注释固化现有契约**，再谈结构重构。

---

## 一页速记

- **状态分六类**：服务端(Query)/URL(Router)/客户端app(resso)/会话(EditingSession)/局部(useState)/持久化(storage)。**先分类，再选工具。**
- **一条底座**：resso 和 EditingSession **底层都是 `useSyncExternalStore`**；`getSnapshot` 引用稳定 + `subscribe` 引用稳定 是两条铁律。
- **resso 的精髓**：per-key 订阅——改一个字段只通知订阅它的组件，大 store 无重渲代价。
- **EditingSession 的精髓**：就地变异 + 结构版本号 bump——值类零重渲、结构类重算，保住几十表单的性能。
- **声明式缓存失效**：store 回归纯状态容器；layout 的 `queryKey` 含 `layoutKeys`（视觉）+ `topologyKeys`（节点集合），改相关字段缓存自然失效，setter 不必手动清。
- **undo/redo**：per-session 快照栈（baseline/done/undone）+ 视口四语义（FitFull/FitId/KeepStable/NoChange）+ 值类 500ms coalescing；Record 不订阅 canUndo，靠热键回调实时判。
- **两座桥**：EditingSession 通过 resso `editingIsEdited` 广播脏标记给 HeaderBar；通过 `editingObjectRes.isEdited` 驱动 layout 缓存策略。
- **提交重基准**：基准重置在 mutation `onSuccess`（`onCommitSuccess`），不在 `submit()` 发请求时——否则失败丢 undo 历史。
- **输入净化**：`$refs`（后端引用元数据，非 JSON Schema `$ref`）在 `prepareEditingObject`/`replaceEditingObject` 统一剥离。
- **设计立场**：可变单例本身不是反模式；render 期变异它 + 无订阅层直读，才是。EditingSession 是命令式领域模型接入 React 的标准形态。
- **判据**：纯 → domain；有状态/副作用 → store/services；服务端数据 → React Query；URL 能承载的 → URL。

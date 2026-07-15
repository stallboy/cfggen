# Undo/Redo：编辑态撤销重做怎么做、为什么不靠现成库

> 这篇讲一条 record **编辑态内**的撤销/重做：为什么内建快照栈、不外包给 redux-undo/zundo/immer；快照栈怎么接 EditingSession、值类怎么合并、提交边界在哪、结构 undo 视口怎么稳定。
>
> **范围**：结构类编辑（增/删/前插/上下移/折叠/换 impl/粘贴/整体替换）+ 值类编辑（primitive 键入 / note），在当前编辑会话内。与导航历史正交——`historyModel.ts`（`alt+c`/`alt+v`）是「访问过哪些 record」，undo/redo 是「当前 record 改了什么」，数据流不交叉。
>
> **不讲（不覆盖）**：跨 record 的 undo（切走再切回不保留旧 record 历史）、已提交数据的回滚、多人协同、纯 UI 局部态（如展开按钮）。
>
> **配套**：编辑会话/性能契约全景（→ [`04-state-management.md`](./04-state-management.md) §六）、视口契约与 `computeStableViewport`（→ [`07-fitview.md`](./07-fitview.md)）、性能契约背景与 release 实测（→ [`10-perf-optimization.md`](./10-perf-optimization.md)）。
>
> **锚点**：快照栈 `domain/undoStack.ts`（`UndoStack`）；接入与 coalescing 在 `services/editingSession.ts`；视口决策 `flow/layout/viewportMath.ts`。

---

## 一、业界原理

表面看 undo 就是「回到上一个状态」。难点在于：**程序的当前状态只有一个，而 undo 需要「过去的状态」——过去已被覆盖。** 所以 undo 的本质是**额外存储历史信息**，而「存什么」决定了方案。

### 1.1 四大模式

| 模式 | 存什么 | 内存 | 要写逆逻辑? | 关键风险 | 代表 |
|---|---|---|---|---|---|
| **A 快照栈** | 全量状态深拷贝 | O(深×大小) | 否 | 内存 | Excalidraw |
| **B 命令** | 操作参数 + 逆 | O(参数)，最省 | **是**（每种操作） | 地址失配 | Monaco / ProseMirror |
| **C Patch** | baseline + N 个 diff | O(1 baseline + N patch) | 否（自动逆） | 路径互译、就地 apply 污染 | fast-json-patch |
| **D CRDT** | CRDT op | 极省 | 否 | 重写数据模型 | Yjs |

- **A 快照栈**：检查点存完整深拷贝，undo = 弹栈顶恢复。实现最简、天然正确、无需写逆逻辑；缺点是内存。
- **B 命令**：每个操作封装 `{do(), undo()}`，只存「做了什么」的参数。最省内存；但每种操作要手写逆逻辑，且依赖「地址稳定」——逆操作靠「在 index 3 插回 item」时，中间若有别的操作移动了下标，逆操作会打到错误元素（**「地址失配」陷阱**，文本编辑器普遍用带 position map 的 step 而非裸命令）。
- **C Patch**：记差异（RFC 6902 的 add/remove/replace…），`compare` 自动生成、`createInverse` 自动逆。需 diff 引擎 + JSON Pointer 与应用寻址互译；就地 apply 会污染共享引用。
- **D CRDT**：数据建模为 CRDT 类型，天然协同、选择性 undo；但必须重写整个数据模型，单用户场景过度工程。

### 1.2 三大工程难题（无论哪种模式都要面对）

1. **粒度 / coalescing**：连敲 "hello" 是 5 次按键，该是 1 步还是 5 步 undo？业界共识——文本/值输入用**时间窗（典型 500ms）+ 失焦 + 换字段**合并；结构操作（增/删/移动）天然离散，1 动作 1 步。Lexical/ProseMirror `newGroupDelay`/Yjs `captureTimeout(500ms)` 都是时间窗。
2. **内存 / 结构共享**：快照栈对大对象每份独立深拷 → 内存爆炸。缓解手段：栈深封顶（如 50）、改 patch 路线、结构共享（immutable.js/immer，但要求整个数据模型不可变）。**关键事实**：结构共享只在「不可变数据结构」下免费；普通可变 JSON 对象 `structuredClone` 出来的快照之间没有任何共享。
3. **提交/持久化后的 undo 语义**：已保存到磁盘/后端的改动不靠内存 undo 回滚（那是版本控制/数据库的另一层）。提交 = undo 栈的天然边界——Monaco `setValue()` 销毁 undo 栈、Excalidraw 提交后栈重置。

> 附：**视图状态的独立性**。undo 数据时，光标/选中/视口焦点要不要一起回滚？多数工具：数据 undo，视图尽量稳定（不强制跳焦点）。视图状态（焦点/视口/折叠）是「瞬时意图」，不是「数据历史」的一部分——但 cfgeditor 对 undo 后的**视口稳定**做了专门处理（见 §四）。

### 1.3 成熟产品的做法（范本）

- **ProseMirror**（与 cfgeditor 最像：命令式 `EditorState` + 树文档 + 事件驱动 transactions）：变更单位是 step、自带 position map、可 `step.invert(doc)`；`done`/`undone` 双栈 + `newGroupDelay` 时间窗聚合 + 分叉。可借鉴：双栈、分叉、时间窗、「invert 而非回滚」。
- **CodeMirror 6**：history 默认只追踪 document + selection，其他 state 要可 undo 须用「inverted effect」显式登记。**对应 cfgeditor**：值类编辑不 bump，要 undo 必须显式 capture，不能指望框架自动捕获。
- **Monaco**：`EditStack` 命令模式 + inverse selection（undo 恢复光标）+ `setValue()` 清栈。
- **Lexical / Yjs**：确立了「500ms 时间窗」这个业界惯例。
- **Excalidraw**：不可变快照栈 + 提交重置，是模式 A 的现代范本。

### 1.4 结论：cfgeditor 为何内建快照栈

对照 cfgeditor 现状（§二详述），通用 undo 库都不适用：

- **redux-undo / zundo / use-undo**：假定不可变 reducer store `{past, present, future}`。cfgeditor 的 `EditingSession` 是**就地变异 + `getSnapshot` 返回 number**，根本冲突，强行套用要重写 EditingSession 为不可变 reducer，破坏性能契约。
- **immer `produceWithPatches`**：`produce` 要求不可变更新，与就地变异 + 共享引用闭包冲突；只能当 diff 引擎替代品（等价 fast-json-patch），无额外收益。
- **命令模式**：可行，但「下标平移」使「按地址重放逆操作」不可行（数组增删/swap/前插会让旧下标整体失效）。
- **CRDT / Yjs**：需把 `editingObject` 重写为 `Y.Map`/`Y.Array`，推翻 editObj 引用语义，过度工程。
- **纯快照栈**：可行，且必须通过 `bumpStructure` 驱动 React（`getSnapshot` 返回 number，不能返回 state 对象）。

**结论**：undo 引擎内建在 `EditingSession` 内，复用现成的 `bumpStructure`/`emit` 通道驱动 React，不能外包给不可变 store 库。`done`/`undone` 双栈 + 分叉 + coalescing 的**思路**借鉴 ProseMirror，但不引入其 store 抽象。cfgeditor 选**快照栈**（模式 A）：不为各编辑写逆逻辑、不引入 diff 引擎，正确性靠全量快照天然保证。

---

## 二、cfgeditor 编辑管线（undo 的硬约束来源）

### 2.1 EditingSession：就地变异 + 结构版本号

`EditingSession`（`services/editingSession.ts`）是**每条 record 编辑态一个**的可变 store 实例，**不进 resso**。它就地变异 `editingObject`（树状 JSON，不换引用），靠一个基本类型 **`structureVersion`（number）**接入 React：

```ts
class EditingSession {
    private editingObject: JSONObject;      // 树状 JSON，就地变异（不换引用）
    private structureVersion = 0;           // 快照：number（天然引用稳定）
    getStructureVersion = () => this.structureVersion;   // getSnapshot
}
// Record.tsx
const structureVersion = useSyncExternalStore(session.subscribe, session.getStructureVersion);
```

> useSyncExternalStore 两条铁律：① `getSnapshot` 必须返回引用稳定的值（同一份状态没变时两次调用必须 `===`）；② `subscribe`/`getSnapshot` 引用要稳定。`structureVersion` 是 number，天然满足①——这就是为什么 `getSnapshot` 不能返回 `editingObject` 引用（就地变异下顶层引用不变，返回引用会让 React 永远跳过更新）。

### 2.2 两条性能契约（undo 设计必须守住的红旗）

| 契约 | 内容 |
|---|---|
| **契约1（值类零重渲）** | 一个 record 可能有几十个表单字段。用户连续键入时，几十个 `EntityForm` **不能重渲**。键入时输入框 UI 由 antd Form 自管（`initialValue` + 内部 store），`editingObject` 被就地改但 React 看不见（不 bump）→ Record 不重渲 → 表单不重渲 → 输入流畅。 |
| **契约2（共享引用）** | 值类编辑就地改 `editingObject`，所有 entity 闭包持有它的**子对象引用**（`edit.editObj = obj`），改完闭包自动见最新值，不必重算整棵实体图；提交时 `submit()` 读到全量最新。 |

**为什么 editingObject 不进 resso**：resso 是「赋新值触发订阅」。若进 resso，每次键入 `store.editingObject = newObj` → 引用变 → 所有依赖重算 → 几十表单重渲 → 与契约1 直接冲突。`EditingSession` 的解法是**就地变异（不换引用）+ 结构版本号（选择性 bump）**。

> **对 undo 的含义**：undo 绝不能在「键入」路径上 bump（破坏契约1）。但 undo 本身是用户主动触发的离散动作（非连续键入），付一次重算代价可接受——这是 undo 能走 `bumpStructure` 的前提。

### 2.3 两类编辑通道

| | 值类编辑 | 结构类编辑 |
|---|---|---|
| 方法 | `updateFormValues` / `updateNote` | add/delete/swap/fold/impl/paste/replace |
| 改 editingObject | 就地改 | 就地改 |
| bump structureVersion | **否** | **是** |
| emit（通知 Record 重渲） | **否** | **是** |
| 结果 | Record 不重渲、entityMap 不重建 | Record 重渲、entityMap 重算 |

**值类全链路**：用户键入 → antd Form 受控更新输入框（Form 自管值，React 不参与）→ `onValuesChange` → `session.updateFormValues(...)` → 就地改 `editingObject` → `notifyEditingState()`（resso `editingIsEdited` 变 → HeaderBar 显示脏标记）→ **不 bump、不 emit** → Record 不重渲 → 键入流畅。

**结构类全链路**：右键「添加」→ `session.addArrayItem(...)` → `obj.push(item)` 就地改 → `structureChange` → `bumpStructure()`：`structureVersion++`【快照变】→ `onStructureChange()`（`removeQueries(['layout', pathname, 'e'])` 清布局缓存）→ `notifyEditingState()`【resso 脏标记】→ `emit()` → Record 重渲 → useMemo 重算 entityMap（遍历最新 editingObject，共享引用，契约2）。

### 2.4 两个关键约束

- **快照不能存 editObj 引用**：`recordEditEntityCreator.ts` 递归遍历 `editingObject`，把每个子对象的**引用本身**塞进 `entity.edit.editObj`，就地改任何一个 `editObj` 即改 `editingObject`。所以快照必须 `structuredClone` 深拷独立化——存引用会被后续就地变异污染。
- **不能用「按 fieldChain 重放逆操作」**：数组下标会随增删/swap/前插整体平移，旧地址失效（这就是命令模式不可行的根因，1.4）。

### 2.5 提交链路与生命周期

```
session.submit() → mutate(editingObject) → 后端 addOrUpdateRecord
  → onSuccess → session.onCommitSuccess()（resetBaselines：清栈+重基准）
              → invalidateAllQueries() → record refetch → maybeReset
```

`submit()` 只 `mutate(editingObject)`，异步、成败要等网络。所以**清栈/重基准挂在 `onSuccess` 而非 `submit()` 调用时**——否则提交失败会丢 undo 历史、脏标记还误报「无未保存」。`originalEditingObject`（脏比较基准）与 `undoStack.baseline` 共享同一 clone（两者都只读不被 mutate），省一次 `structuredClone`。

**生命周期**：`<RecordWithResult key={curTableId-curId}>`——切 record 就 unmount → session 实例销毁 → undo 栈随之消失。**undo 历史不跨 record 保留**。unmount 不能只靠 GC：coalesce 的 `setTimeout` 句柄不在 session 的 GC 图里（回调闭包持有 session 引用），unmount 后 500ms 仍可能 fire → 内存泄漏（Tauri 桌面敏感），所以必须显式 `dispose()`。

---

## 三、当前设计——快照栈

### 3.1 UndoStack：纯数据栈

`domain/undoStack.ts`，只管栈，不依赖 session、不调 React。三段语义：

- **baseline**：初始 / 最近一次提交后的状态。undo 到栈底恢复成它（显式存住，无 off-by-one）。
- **done**：操作后快照；`done[末]` = 最近。`capture` 入栈、`popUndo` 弹出。
- **undone**：已 undo、可 redo。`popRedo` 弹出。

```ts
export type Snapshot = {
    data: JSONObject;            // editingObject 深拷（独立，避免被后续就地变异污染）
    undoFitView: EFitView;       // undo/redo 到此快照后该用的 fitView（结构→KeepStable；整体替换→FitFull；值类→NoChange）
    anchorId?: string;           // KeepStable 时的锚点节点 id（= 产生此快照的操作的视觉焦点；delete 取父）
};

export class UndoStack {
    private baseline!: Snapshot;
    private done: Snapshot[] = [];
    private undone: Snapshot[] = [];
    private readonly maxDepth = 50;

    setBaseline(s: Snapshot) { this.baseline = s; this.done = []; this.undone = []; }   // 初始/提交后：重基准清栈
    capture(s: Snapshot) {       // 每次编辑后：入栈，丢弃 redo 历史（分叉）。超 maxDepth 丢最旧
        this.undone = [];
        this.done.push(s);
        if (this.done.length > this.maxDepth) this.done.shift();
    }
    canUndo() { return this.done.length > 0; }
    canRedo() { return this.undone.length > 0; }
    popUndo()  { /* 弹 done 顶，返回要恢复成的 target + 被撤销操作的 {undoFitView, anchorId} */ }
    popRedo()  { /* 弹 undone 顶，返回它 + 其 {undoFitView, anchorId} */ }
}
```

要点：
- **Snapshot 必须独立深拷**（`structuredClone`），不能存 `editingObject`/`editObj` 引用。clone 由 session 的 `captureUndoPoint` 负责，`UndoStack` 只存调用方传入的已 clone 对象（不二次 clone）。
- **分叉**：`capture` 清空 `undone`（undo 后又新编辑，redo 历史作废，与所有编辑器一致）。
- **maxDepth=50**：栈深硬上限，超限丢最旧（大 record 的内存兜底）。
- 栈语义由 `undoStack.test.ts` 独立单测（栈深、分叉、baseline 栈底、视口语义随快照、maxDepth 封顶）。

### 3.2 EditingSession 接入

`UndoStack` 是纯数据；capture/apply 时机与 React 驱动由 `EditingSession` 负责。相关成员：

| 成员 | 职责 |
|---|---|
| `undoStack: UndoStack` | 快照栈实例（命名 `undoStack` 而非 `undo`，避免与 `undo()` 方法同名——TS 不允许同名的属性与方法） |
| `valueCoalesceTimer` / `valueCoalesceKey` | 值类合并的定时器句柄 / 当前合并组的字段标识 |
| `initUndoBaseline()` | mount effect 调一次，设初始基准（构造函数在 render 期，`structuredClone` 是副作用，挪到 effect） |
| `undo()` / `redo()` | 入口（见 3.4） |
| `canUndo` / `canRedo` | 箭头属性（绑 this） |
| `onCommitSuccess()` | 提交成功后清栈 + 重基准（见 3.6） |
| `dispose()` | unmount 清理（见 3.7） |
| `captureUndoPoint()` | `structuredClone(editingObject)` |
| `applyUndoPoint(s)` | `editingObject = structuredClone(s)` |
| `structureChange(position, undoAnchorId?)` | 结构操作收尾：`bumpStructure({FitId, position})` + capture（KeepStable + 锚点） |
| `beforeStructuralChange()` | 结构操作前置：flush 值类组 |
| `flush/touch/coalesceKey` | 值类合并（见 3.5） |
| `resetBaselines()` | 重置脏比较基准 + undo baseline（提交/reset 后） |

`captureUndoPoint` / `applyUndoPoint` 是**升级契约点**：现为 `structuredClone`，将来若换 JSON Patch 只动这两个方法体，上层不动（见 §六）。

### 3.3 capture：什么时候存快照

**(a) 初始 / 提交后 —— `setBaseline`**。session 构造后由 Record 的 mount effect 调 `initUndoBaseline()`（不在构造函数里），提交成功后由 `onCommitSuccess → resetBaselines` 再调。baseline 用 `FitFull`。

```ts
// Record.tsx
useEffect(() => {
    session.initUndoBaseline();          // 幂等：StrictMode 双调安全
    return () => session.dispose();
}, [session]);
```

**(b) 结构类编辑后 —— `capture`**。每个结构 mutation（`addArrayItem`/`addArrayItemAtIndex`/`deleteArrayItem`/`swapArrayItem`/`updateFold`/`updateInterfaceValue`/`pasteStruct`）都走统一模板：

```ts
addArrayItem(defaultItem, arrayFieldChains, position) {
    this.beforeStructuralChange();              // ① flush 值类组（固化未 capture 的键入）
    const obj = getFieldObj(this.editingObject, arrayFieldChains);
    obj.push(defaultItem);                       // ② 改 editingObject
    this.structureChange(position);              // ③ bumpStructure({FitId, position}) + capture(KeepStable, 锚点)
}
private structureChange(position, undoAnchorId?) {
    this.bumpStructure({fitView: EFitView.FitId, position});
    this.capture(EFitView.KeepStable, undoAnchorId ?? position.id);   // undo 锚点默认=操作节点 id
}
```

结构操作前必须先 flush 值类组，否则结构操作前未固化的键入会和结构操作混在一个快照里，undo 粒度变粗。

**(c) 整体替换 —— `replaceEditingObject`**（Chat/AddJson 写入 / funcClear）。用 `FitFull`（要重算视口），就地剥离入参 `$refs`（与 `prepareEditingObject` 对齐，防污染提交载荷与脏比较），单独 capture：

```ts
replaceEditingObject(newEditingObject) {
    this.beforeStructuralChange();
    deleteRefsInPlace(newEditingObject);
    this.editingObject = newEditingObject;
    this.bumpStructure({fitView: EFitView.FitFull});
    this.capture(EFitView.FitFull);
}
```

**(d) 值类合并组结束 —— `flushValueCoalesce` 内的 `capture(NoChange)`**（见 3.5）。

另：**Form.List 长度变化**（primitive 数组的行增删）走值类通道（`updateFormValues`），但语义是结构变更。在 `updateFormValues` 里对 array 字段做长度 diff——长度变：`flushValueCoalesce` + 写回 + `capture(NoChange)`（当结构步，但不建实体、布局变化极小故不动视口）；长度同：当值类合并。

### 3.4 undo / redo 执行

```ts
undo() {
    this.flushValueCoalesce();                                        // ① 先固化未 capture 的键入（否则丢失）
    if (!this.undoStack.canUndo()) return;
    const {target, undoFitView, anchorId} = this.undoStack.popUndo(); // ② 被撤销操作快照的语义 + 要恢复成的 target
    this.applyUndoPoint(target.data);                                 // ③ editingObject = structuredClone(target.data)
    this.bumpStructure({fitView: undoFitView, position: anchorId ? {id: anchorId, x: 0, y: 0} : undefined});
    // ④ 按被撤销操作的语义驱动视口：结构→KeepStable+锚点；整体替换→FitFull；值类→NoChange
}
redo() { /* 对称：popRedo，其余相同 */ }
```

`bumpStructure` 内含 `notifyEditingState`（刷 HeaderBar 脏标记）+ `emit`（通知 `useSyncExternalStore` 订阅者）。

> `position: {id: anchorId, x: 0, y: 0}` 的 `x/y` 是占位：undo/redo 的 `undoFitView` 只会是 `KeepStable`/`NoChange`/`FitFull`，永远不是 `FitId`，故 `x/y` 不会被消费（KeepStable 的 anchorOld 来自 flow 层缓存的上一帧布局，不用 position.x/y，见 §四）。

**UI 同步链路**（undo 后 Form 显示新值）：

```
bumpStructure → structureVersion++ → emit
  → useSyncExternalStore 通知 Record
  → Record 的 useMemo 重算 entityMap（读新 editingObject）
  → Entity 新对象 → FlowNode memo 失效重渲 → EntityForm 重渲
  → useSyncFieldValue（依赖 field 引用）检测到 field 新对象
  → form.setFieldValue → Form 显示 undo 后的值
```

### 3.5 值类编辑的合并（coalescing）

值类编辑（`updateFormValues`/`updateNote`）每键触发。逐键入栈会让「打一个字 = 一步 undo」，所以合并（500ms 时间窗，业界共识）。

| 事件 | 处理 |
|---|---|
| 键入，同字段 + 500ms 窗口内 | 重置定时器，**不入栈** |
| 键入，换字段 | 关闭旧组（capture）、开新组 |
| 定时器到期（500ms 无新键入） | 关闭组（capture） |
| 任何结构操作（`beforeStructuralChange`） | 关闭组（capture），再执行结构操作 |
| undo / redo | 关闭组（capture），再 pop |

「关闭组（capture）」= 把当前 `editingObject` 存一份快照入栈。一个字段一次聚焦输入 = 一个 undo 步。

```ts
private touchValueCoalesce(fieldKey: string) {
    if (this.valueCoalesceKey !== fieldKey) {
        this.flushValueCoalesce();              // 换字段：关闭旧组
        this.valueCoalesceKey = fieldKey;
    }
    if (this.valueCoalesceTimer !== undefined) clearTimeout(this.valueCoalesceTimer);
    this.valueCoalesceTimer = setTimeout(() => this.flushValueCoalesce(), 500);
}
private flushValueCoalesce() {
    if (this.valueCoalesceTimer === undefined) return;   // 无活跃组，no-op
    clearTimeout(this.valueCoalesceTimer);
    this.valueCoalesceTimer = undefined;
    this.capture(EFitView.NoChange);
    this.valueCoalesceKey = undefined;
    this.emit();   // capture 不 bump structureVersion；canUndo/canRedo 已变，emit 通知潜在订阅者
}
```

**per-key 成本**：合并判定只做「字段标识比较 + `clearTimeout`/`setTimeout`」，不 clone、不遍历 `editingObject`。这是「键入零重渲」的关键之一（另一处是 `notifyEditingState` 里的 `isDeeplyEqual` 深比较，见 [`04-state-management.md`](./04-state-management.md) §8.2）。换字段的信号来自 `EntityForm` 的 `onValuesChange(changed, allValues)`：`allValues` 写回 editingObject，`changed` 算 coalescing key + Form.List 长度 diff。

值类 coalescing 由 `editingSession.test.ts` 单测（同字段合并、换字段关闭、timer 到期、Form.List 长度变/同、per-key 不 bump/emit、结构操作前 flush、dispose flush）。

### 3.6 提交边界

```ts
onCommitSuccess() { this.resetBaselines(); }      // 由 Record.tsx 的 onSuccess 调
private resetBaselines() {
    clearTimeout(this.valueCoalesceTimer);         // 清 coalesce（旧键入已落库，不保留 undo）
    this.valueCoalesceTimer = undefined;
    this.valueCoalesceKey = undefined;
    const snap = this.captureUndoPoint();
    this.originalEditingObject = snap;             // 脏比较基准归零（isEdited → false）
    this.undoStack.setBaseline({data: snap, undoFitView: EFitView.FitFull});  // 清栈 + 新基准 = 当前已提交状态
}
// Record.tsx 的 useMutation onSuccess（resultCode == updateOk / addOk）
session.onCommitSuccess();
invalidateAllQueries();
if (curId === NEW_RECORD_ID) navigate(navTo('record', curTableId, editResult.id, true));   // 新记录跳真实 id
// onError 分支：什么都不动，用户可继续编辑 / undo
```

**新记录**：`onSuccess` navigate 到返回的真实 id → `RecordWithResult` key 变 → 旧 session unmount（栈随实例销毁）→ 新 session 用真实 id 构造。**后台推数据覆盖**：refetch → `maybeReset` 真 reset 分支也调 `resetBaselines`（`maybeReset` 的「未保存编辑时提示是否丢弃」守卫尚未实现——目前真 reset 会静默覆盖本地编辑）。

一句话：**已提交的改动不归内存 undo 回滚**（与 Monaco `setValue` 清栈、Excalidraw 提交后重置一致）。

### 3.7 生命周期（dispose）

```ts
dispose() {
    this.flushValueCoalesce();      // 不丢用户最后一次键入
    clearTimeout(this.valueCoalesceTimer);
    this.listeners.clear();
}
```

per-session，unmount 必须 `dispose()`（flush + 清 timer + 清 listeners），防 session 被 setTimeout 闭包持住泄漏。

### 3.8 UI 接入（按钮 / 快捷键 / canUndo）

`Record.tsx` **只订阅 `structureVersion`**，**不订阅 canUndo/canRedo**——这刻意的：值类 coalescing flush 会让 `canUndo` 翻转，若订阅它会触发重渲，啃掉契约1。

- **快捷键**：`ctrl+z`/`cmd+z`（undo）、`ctrl+y`/`ctrl+shift+z`/`cmd+y`/`cmd+shift+z`（redo），per-session 注册在 `Record.tsx`。回调**实时判** `session.canUndo()`：栈空/非编辑态直接 return（不 preventDefault）→ input 原生 undo 不被误杀；`enableOnFormTags: true` 拦截 input 默认 undo，由 session coalescing 接管。不沿用全局 `getCurrentEditingSession()` 模式——分屏时会撤销错 session。
- **按钮**：画布右键菜单（`paneMenu`）加 undo/redo 项，`disabled: () => !session.canUndo()`（**惰性函数**——菜单打开时才求值，故禁用态新鲜而不需要响应式订阅）。放在 Record 自己的作用域（session 直接寻址、天然分屏隔离），不放 `HeaderBar`（Splitter 兄弟、拿不到 session 实例）。

```ts
const structureVersion = useSyncExternalStore(session.subscribe, session.getStructureVersion);
useHotkeys('ctrl+z, cmd+z', (e) => {
    if (!isEditing || !session.canUndo()) return;   // 实时判，不订阅
    e.preventDefault();
    session.undo();
}, {enableOnFormTags: true});
// paneMenu 项：{label: t('undo'), key: 'undo', handler: () => session.undo(), disabled: () => !session.canUndo()}
```

### 3.9 Form 同步：useSyncFieldValue

`useSyncFieldValue`（`flow/edit/shared/useSyncFieldValue.ts`，挂在 `PrimitiveFormItem`/`ArrayOfPrimitiveFormItem`/`InterfaceFormItem` 上）解决 antd 已知问题：`Form.Item` 的 `initialValue` 仅在字段首次注册时生效，切换 impl / 同 key 复用时新 `initialValue` 被忽略 → 表单显示旧值（[ant-design/issues/56102](https://github.com/ant-design/ant-design/issues/56102)）。

```ts
export function useSyncFieldValue(form, field) {
    useEffect(() => {
        if (form.getFieldValue(field.name) !== field.value) {   // 只在 Form 内部值与 field.value 不一致时才 set
            form.setFieldValue(field.name, field.value);
        }
    }, [form, field]);
}
```

它依赖 **field 引用**（entityMap 每次重算都是新对象）而非仅 `field.value`——保证 undo/redo 后 effect 必跑、重新评估是否需要同步；但只在 `getFieldValue(name) !== field.value` 时才 `setFieldValue`，值一致的字段跳过。这是值类 undo 正确性 + 性能的关键：

- **值类 undo**（脱节场景）：值类编辑不重算 entityMap（契约1），`field.value` 快照停在旧值、而 antd Form 内部已被用户输入改到新值；undo 让 editingObject 回旧值、entityMap 重算出 `field.value=旧值`，此时 `getFieldValue(新) !== field.value(旧)` → set，同步成功。
- **一致字段跳过**：entityMap 重算时多数字段 value 并未变（如不相关的结构编辑）。若也 set，`@rc-component/form` 的 setField 分支对路径匹配字段是**无条件 forceUpdate（不比较值）**，会让这些字段多一次额外重渲。`getFieldValue` 比较挡掉它——结构编辑只重渲真正变化的字段。

antd `setFieldValue` by design 不触发 `onValuesChange`，不会回流污染 coalescing 栈。

---

## 四、视口稳定（当前实现：KeepStable）

### 4.1 问题：结构 undo 会跳

undo/redo 早期统一用 `EFitView.NoChange`（视口参数不动）。这对**值类 undo** 正确（布局不变），但对**结构 undo**（删/加节点、折叠/展开、swap、换 impl、粘贴）会触发 ELK 重排——节点世界坐标全变，视口死死不动，于是 `screen = world·zoom + vp` 让画面整体跳。

`NoChange` 的语义是「不动视口参数」，隐含前提是**布局也不变**。结构 undo 时：`applyUndoPoint` 回滚拓扑 → `bumpStructure` 清 layout 缓存 → entityMap 重算出新 nodes/edges → ELK 重跑出全新世界坐标 → Effect 读到 `NoChange` → 视口不动 → world 变 vp 不变 → **画面跳**。值类 undo 也走 `removeQueries`（ELK 重跑一次），但因为输入不变、输出坐标逐位相同，不跳。

**精确边界**：跳只发生在「undo 前后 ELK 输入不同」时，主因是结构变更。

### 4.2 方案：锚点节点屏幕稳定

「不跳」唯一可行的定义是让**某个具体节点** undo 前后屏幕坐标不变，复用现成的 `computeStableViewport` 数学（`flow/layout/viewportMath.ts`，fitview 文档 §5 详）：

```
要求：anchorNew·zoom + newVp === anchorOld·zoom + vp，且 newVp.zoom === vp.zoom
解：  newVp = (anchorOld − anchorNew)·zoom + vp
```

这正是正向结构操作（`FitId`）用的数学——undo 复用同一套，区别只在「锚点选谁、anchorOld/anchorNew 怎么拿」。

> 为什么不能「保持视口中心的世界坐标不变」？代入 `newVp = screenCenter − W_old·zoom` 会解出 `newVp = vp_old`，与 NoChange 数学等价——「世界坐标系的同一点」undo 前是节点 A、undo 后是节点 B，中心点的节点变了，照样跳。证伪。

### 4.3 锚点选谁：视觉焦点 = 被撤销操作的节点

undo 是「撤销最近一个操作」。用户做那个操作时视觉焦点就在被操作的节点上；撤销它，用户期待那个节点（或其位置）稳定。故锚点 = **被撤销操作的节点 id**，由快照携带：

| 被撤销的操作 | 锚点取法 |
|---|---|
| add / swap / fold / impl / paste | = 操作 `position.id`（操作节点，undo 前后都在） |
| **delete** | = **父节点 id**（被删节点 undo 前不存在，父稳定） |
| 值类（primitive / note） | 无结构锚点（布局不变，快照 NoChange → 不补偿） |
| replaceEditingObject（整体换） | 无单一焦点（快照 FitFull → 重新铺满） |

**「delete 取父」**：`recordEditEntityCreator.ts` 遍历 list item 时闭包恰好持有父 id（当前 struct 的 id），delete 调用 `session.deleteArrayItem(arrayIndex, chain, position, id)` 第 4 参传父 id；正向 `position` 仍指被删 item（删后不在新布局 → FitId 自然 noop，「删除后视口不动」行为不变）。连续 undo/redo：每个快照携带「产生它的操作」的锚点，弹出时取**被弹出那个**的锚点，自然跟随每一步不漂移。

### 4.4 快照带元数据

`Snapshot` 携带 `{undoFitView, anchorId?}`（3.1）。capture 时机与取值：

| capture 时机 | undoFitView | anchorId |
|---|---|---|
| `structureChange`（add/swap/fold/impl/paste） | `KeepStable` | `position.id` |
| `structureChange`（**delete**） | `KeepStable` | `undoAnchorId`（父 id，creator 传入） |
| `replaceEditingObject` | `FitFull` | — |
| `flushValueCoalesce` / Form.List 值类步 | `NoChange` | — |
| `setBaseline`（初始/提交后） | `FitFull` | — |

`popUndo`/`popRedo` 返回 `{target, undoFitView, anchorId}`：`undoFitView`/`anchorId` 取自**被弹出那个**快照（= 被撤销操作的焦点）。栈语义不变（baseline/done/undone 三段、分叉、maxDepth=50），只是每项多了两个元数据字段。

### 4.5 `EFitView.KeepStable` 与 prevRectMapRef 时序

新增一档 `KeepStable`（`domain/entityModel.ts`）：以 `fitViewToIdPosition.id` 为锚点，relayout 后让其屏幕坐标不变；与 `FitId` 区别是 **anchorOld 来源不同**（上一帧布局 vs 操作发起时 position.x/y），与 `NoChange` 区别是**布局变就补偿、不变就不动**。`KeepStable` 与 `FitId` 复用同一 `computeStableViewport` + 同一 `{kind:'fitId'}` 动作（`pickViewportAction` 内）：

```ts
// flow/layout/viewportMath.ts
if (fitView === KeepStable && fitViewToIdPosition && opts?.prevId2RectMap) {
    const oldRect = opts.prevId2RectMap.get(id);   // anchorOld = 上一帧布局该 id 坐标（undo 发起时）
    const newRect = id2RectMap.get(id);            // anchorNew = 新布局同 id 坐标
    if (oldRect && newRect) return {kind: 'fitId', viewport: computeStableViewport(oldRect, newRect, currentVp)};
}
```

`useEntityToGraph`（`flow/useEntityToGraph.ts`）缓存上一帧布局 `prevRectMapRef`，Effect 2（视口动作，刻意与节点下发 effect 拆开）末尾更新：

```
t0  ctrl+z → session.undo() → applyUndoPoint → bumpStructure({KeepStable, anchorId})
      → structureVersion++ / removeQueries(['layout',pathname,'e']) / emit
t1  Record 重渲 → entityMap 重算（新拓扑）→ newNodes 引用变
t2  useQuery 缓存已空 → data=undefined（loading）→ ELK 异步重跑
      （id2RectMap=undefined → Effect 2 guard 不满足、不跑 → prevRectMapRef 不更新，仍=undo 前布局；
       newNodes=undefined → Effect 1 不下发 → 画面保持 undo 前节点位置）
t3  ELK 返回新 id2RectMap → newNodes 更新 → Effect 2 主体跑：
      - getViewport()=undo 前视口 / id2RectMap=新布局 / prevRectMapRef.current=undo 前布局
      - KeepStable 分支：prevRectMap.get(anchorId)=anchorOld，id2RectMap.get(anchorId)=anchorNew
        → computeStableViewport → setViewport（锚点屏幕不动）
      - prevRectMapRef.current = 新布局
```

**不变量**：`prevRectMapRef` 只在 Effect 2 主体末尾（guard 内）更新；loading 期绝不更新 → undo 前布局留到 t3 消费。

### 4.6 边界

| 场景 | 行为 |
|---|---|
| 值类 undo（快照 NoChange） | 不补偿 → 不动（布局不变）✅ |
| 结构 undo，锚点 undo 前后都在 | 补偿 → 锚点屏幕不动 ✅ |
| delete undo | 锚点=父，父 undo 前后都在 → 补偿 ✅ |
| 整体替换 undo（快照 FitFull） | 全图铺满 ✅ |
| 锚点在新布局不存在（极端） | `prevRectMap.get`/`id2RectMap.get` 命中失败 → noop（罕见，接受小跳） |
| 首次无 prevRectMapRef | `opts.prevId2RectMap` 缺失 → noop（undo 前必有布局，防御） |

视口机制全貌（`EFitView` 四档语义、`computeStableViewport` 推导、`pickViewportAction` 三分支、固定页/只读路径）见 [`07-fitview.md`](./07-fitview.md)。

---

## 五、性能契约与测试

### 5.1 契约回顾

| 契约 | 内容 |
|---|---|
| **契约1** | 值类编辑不 bump `structureVersion`、不 emit → Record 不重渲、entityMap 不重建。几十个表单输入零重渲。 |
| **契约2** | 结构类编辑 bump + emit → Record 重渲 → entityMap 重算；重算时读 `getEditingObject()`（共享引用，entity 闭包自动见最新值）。 |

undo/redo 走 `bumpStructure`，触发 entityMap 全量重算——成本等于一次结构编辑（重渲，非 mount；节点 key 稳定时 React 复用组件实例，只重跑 render + diff）。undo 是离散动作（非连续键入），付一次重算可接受。值类 coalescing 的 per-key 路径不 bump/emit（不触碰契约1）。

### 5.2 单测覆盖

- `undoStack.test.ts`：栈语义（baseline 栈底、分叉、maxDepth 封顶）、视口语义随快照（`popUndo`/`popRedo` 返回被弹出快照的 `{undoFitView, anchorId}`）。
- `editingSession.test.ts`：maybeReset 早退/真 reset/幂等、值类 vs 结构类契约、submit/replaceEditingObject、onCommitSuccess 清栈、getEditingObjectRes、fuzz 混合操作、updateFormValues 类型转换/$impl 早退、pasteStruct 深拷独立性、isDeeplyEqual、undo/redo（结构/值类/整体替换/delete 取父/分叉/清栈）、值类 coalescing（同字段合并/换字段/timer 到期/Form.List 长度变同/per-key 不 bump/结构前 flush/dispose flush）。
- `viewportMath.test.ts`：`pickViewportAction` 全分支（undefined/FitFull→fitFull；FitId+命中→锚点屏幕不变+缩放保持；FitId id 不存在/无 position→noop；NoChange→noop；KeepStable+命中→锚点屏幕不变；KeepStable 锚点缺失/无 prevMap/无 position→noop；KeepStable prev/new 逐位相同→视口不变）。

### 5.3 release 实测方案（dev 不可信）

dev 模式 longtask 不准——bundle 大、HMR、React Profiler 绝对耗时三重膨胀。绝对成本须 release build 直测（PerformanceObserver longtask 是金标准，>50ms 即 longtask）：

```js
window.__longtasks = [];
new PerformanceObserver(list => {
    for (const e of list.getEntries())
        window.__longtasks.push({duration: Math.round(e.duration), startTime: Math.round(e.startTime)});
}).observe({entryTypes: ['longtask']});
```

四项 spike（最大 record 上）：

| # | 测试 | 预期 |
|---|---|---|
| 1 | 结构 undo 重渲 longtask（add array item → ctrl+z） | <50ms（重渲非 mount） |
| 2 | 值类 undo 重渲 longtask（键入 primitive → ctrl+z） | <50ms |
| 3 | Form.List 同步（undo 含 Form.List 增删 → 行数/值正确） | 正确；若 antd Form.List 不响应 setFieldValue → 需 `key={structureVersion+fieldChain}` 强制 remount |
| 4 | Tab 遍历 clone 累计（N 字段 → 读值类组关闭累计 longtask） | 无累积 longtask |

**触发优化条件（>50ms）**：spike 1/2 结构 undo 卡 → entityMap 引用稳定化（`createThis` 复用未变节点 Entity，只重建变化子树）→ FlowNode memo 不击穿 → 一举两得修 fold 切换全量重建债；spike 2 仅值类 undo 卡 → form registry 轻量化（EntityForm 注册 Form 实例，undo 时 setFieldValue 绕过 bump，零重渲）；spike 4 累计卡 → lazy snapshot（组关闭只记 marker，undo 时才 clone）。

---

## 六、升级路径

`captureUndoPoint` / `applyUndoPoint` 是升级契约点。当前 `structuredClone` 全量快照——正确性最好、实现最简，maxDepth=50 + 快照独立深拷是内存兜底。**不建议预优化**：换 JSON Patch 要引入 diff 引擎 + 路径互译成本，而结构 undo 的卡顿根源是 entityMap 全量重算模型（命令模式/Patch 同样要走 `bumpStructure` 全量重渲），换 undo 实现解决不了。**先按 5.3 release 实测，确认真有内存/性能问题再动**；若要动，优先 entityMap 引用稳定化（一举两得），其次才考虑 JSON Patch（只改这两个方法体，上层不动）。

---

## 一句话速记

- **做什么**：record 编辑态 undo/redo（≠ 导航历史）。覆盖值类 + 结构类，session 内，不跨 record、不回滚已提交。
- **怎么做**（快照栈）：每次编辑后存 `editingObject` 全量深拷（快照带 `{undoFitView, anchorId?}`）；undo = apply 上一份 + `bumpStructure`（按快照语义：结构→`KeepStable`+锚点 / 值类→`NoChange` / 整体替换→`FitFull`）；redo 对称。`UndoStack`（`domain/undoStack.ts`）纯数据类，`undo/redo` 是 session 方法。
- **栈语义**：baseline / done / undone 三段；capture 清 undone（分叉）；maxDepth=50 封顶。
- **capture 时机**：初始/提交后 `setBaseline`（mount effect）；结构操作 `beforeStructuralChange` + `structureChange`（FitId + capture KeepStable）；整体替换 FitFull + capture FitFull；值类组关闭 capture NoChange；Form.List 长度变当结构步（NoChange）。
- **coalescing**：值类逐键合并（500ms 窗口），per-key 不 bump/emit。换字段信号来自 `changedValues`。
- **提交边界**：`onSuccess` → `onCommitSuccess` → `resetBaselines`（清栈+重基准）；新记录 navigate 真实 id。
- **生命周期**：per-session，unmount 必须 `dispose()`。
- **UI**：按钮放画布右键菜单（`disabled` 惰性函数）+ 快捷键 per-session（实时判 canUndo，**不订阅**，护契约1）。
- **Form 同步**：`useSyncFieldValue`（`flow/edit/shared/`）依赖 field 引用 + `getFieldValue` 比较——保证值类 undo 后 Form 同步，且结构编辑时不重渲未变字段。
- **视口稳定**：结构 undo 用 `KeepStable`，锚点=被撤销操作节点 id（delete 取父），anchorOld 由 flow 层 `prevRectMapRef`（上一帧布局）读；值类 NoChange、整体替换 FitFull。
- **升级路径**：`captureUndoPoint`/`applyUndoPoint` 是契约点；**不预优化**，gate on release 实测。

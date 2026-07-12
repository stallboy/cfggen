# 编辑操作 Undo/Redo 设计（cfgeditor）

> **状态：待审核**。这是一份**方案介绍**——先讲"快照栈怎么实现"（§2，详细到数据结构与伪码），再讲性能、前置技术债、其他方案对比、待你拍板的决策。
> 背景知识（业界 undo/redo 四模式 + cfgeditor 编辑管线现状）见 [`undo-redo-原理与现状.md`](./undo-redo-原理与现状.md)。
> 标 ⚠️ 的是**需要你拍板**的决策点，集中在 §6。

---

## 1. 要做什么

给 cfgeditor 的 record 编辑器加 **undo/redo**：用户在一条 record 的编辑态里，可以撤销/重做刚才的编辑操作（增删数组项、改字段值、换 impl、粘贴、AI/JSON 整体写入……）。

### 先分清：导航历史 ≠ 编辑 undo

cfgeditor 已有的 `historyModel.ts`（`alt+c`/`alt+v`）是**导航历史**——"我访问过哪些 record"。本文做的是**编辑 undo**——"我在当前 record 改了什么、撤回"。两者正交，数据流不交叉，不试图统一。下文"栈""undo"一律指编辑 undo。

### 范围
- **覆盖**：结构类编辑（增/删/前插/上下移/折叠/换 impl/粘贴/整体替换）+ 值类编辑（primitive 键入 / note），在当前编辑会话内。
- **不覆盖**（非目标）：跨 record 的 undo（切走再切回不保留旧 record 历史）；已提交数据的回滚（那是服务端 record 版本功能）；多人协同 / CRDT；纯 UI 局部态（如 `ArrayItemExpandButton`）。

### 一个前置事实（贯穿全文）
`EditingSession`（每条 record 一个）**就地变异** `editingObject`，靠 `structureVersion`（number）接入 React。**值类编辑不 bump**（键入时几十表单零重渲），**结构类编辑 bump + emit**（entityMap 重算）。已有的 `bumpStructure` 就是现成的"结构变更通知通道"。undo 引擎内建在 session 里、复用这条通道——不另起炉灶、不外包给 redux-undo 那类不可变 store 库（它们与就地变异根本冲突，原理与现状.md §4.11）。

---

## 2. 方案：快照栈（P0）

### 2.1 核心思路（一段话）

每次编辑后，存一份 `editingObject` 的**全量深拷贝**（叫一个 snapshot）。undo = 回到上一个 snapshot；redo = 前进到下一个。栈随 session 生灭，提交后清空。就这么简单——**不为 8 种编辑各写逆逻辑、不引入 diff 引擎**，正确性靠"全量快照"天然保证。

为什么是它、不是命令模式或 JSON Patch，见 §5。这里先把它讲透。

### 2.2 数据结构：`UndoStore`

`UndoStore` 是一个**纯数据类**——只管栈，不依赖 session，不调 React。

```ts
type Snapshot = JSONObject;   // editingObject 的一份独立深拷贝

class UndoStore {
    private baseline: Snapshot;            // 初始 / 最近一次提交后的状态
    private done: Snapshot[] = [];         // 操作后快照；done[末] = 最近
    private undone: Snapshot[] = [];       // 已 undo、可 redo
    private readonly maxDepth = 50;        // 栈深硬上限（⚠️ Q11：改字节预算？）

    // 初始 / 提交后调：重置基准，清栈
    setBaseline(s: Snapshot) { this.baseline = s; this.done = []; this.undone = []; }

    // 每次编辑后调：入栈新快照，丢弃 redo 历史（分叉）
    capture(s: Snapshot) {
        this.undone = [];
        this.done.push(s);
        if (this.done.length > this.maxDepth) this.done.shift();   // 封顶丢弃最旧
    }

    canUndo() { return this.done.length > 0; }
    canRedo() { return this.undone.length > 0; }

    // undo：弹出最近快照，返回"要恢复成的状态"（前一个快照或 baseline）
    popUndo(): Snapshot {
        const s = this.done.pop()!;
        this.undone.push(s);
        return this.done.length > 0 ? this.done[this.done.length - 1] : this.baseline;
    }
    // redo：返回"要恢复成的状态"（刚 redo 的快照）
    popRedo(): Snapshot {
        const s = this.undone.pop()!;
        this.done.push(s);
        return s;
    }
}
```

几个要点：

- **Snapshot 必须是独立深拷**（`structuredClone`），**不能存 `editingObject` 或 `editObj` 的引用**——它们会被后续就地变异污染，存引用等于存一个会被改的活对象，undo 时早已面目全非。
- **`baseline` 是显式的**（不是"隐式当前态"）：undo 到栈底要能恢复成初始/提交后状态，必须存住它。`canUndo = done.length > 0`，栈底 baseline 不可 pop，无 off-by-one。
- **分叉**：`capture` 清空 `undone`——undo 之后又有了新编辑，redo 历史就作废（和所有编辑器的 undo 行为一致）。
- **⚠️ Q11 内存**：`maxDepth=50` 是条数上限。大 record（含长数组字段）下 50 份独立深拷可能偏重，备选是**字节预算驱动**（push 前估算 snapshot 字节，超预算从栈底 shift）。见 §6 Q11。

### 2.3 嵌入 `EditingSession`

`UndoStore` 是纯数据；`undo()`/`redo()` 是 **session 的方法**——因为执行时要调 session 自己的 private `bumpStructure`（TypeScript 的 `private` 是类作用域，独立 class 调不到）。

session 新增的成员：

```
EditingSession（现有 + 新增）
├── private undo: UndoStore                        // 上面那个纯数据栈
├── private valueCoalesceTimer: number | undefined // 值类合并的定时器句柄
├── private valueCoalesceKey: string | undefined   // 当前合并组的字段标识
├── undo() / redo()                                // 入口（见 2.5）
├── onCommitSuccess()                              // 提交后清栈（见 2.7）
├── dispose()                                      // unmount 清理（见 2.8）
├── private captureUndoPoint(): Snapshot           // = structuredClone(editingObject)
├── private applyUndoPoint(s: Snapshot)            // = editingObject = structuredClone(s)
├── private beforeStructuralChange()               // 结构操作前置：flush 值类组
└── private flushValueCoalesce()                   // 值类组合并收尾（见 2.6）
```

`captureUndoPoint` / `applyUndoPoint` 这两个方法是**升级契约**：P0 它们是 `structuredClone`，将来若要换 JSON Patch（§5），只动这两个方法体，上层不动。

### 2.4 capture：什么时候存快照

三个时机：

**(a) 初始 / 提交后**——`setBaseline`。
session 构造后由 Record 的 mount effect 调一次（**不在构造函数里**——构造函数在 render 期执行，`structuredClone` 是副作用，必须挪到 effect）。提交成功后也调（见 2.7）。

```ts
// Record.tsx
useEffect(() => {
    session.setBaseline(session.captureUndoPoint());   // 幂等：StrictMode 双调安全
}, [session]);
```

**(b) 结构类编辑后**——`capture`。
现有的每个结构 mutation（`addArrayItem` / `deleteArrayItem` / `swapArrayItem` / `updateFold` / `updateInterfaceValue` / `pasteStruct` / `replaceEditingObject`）都走 `structureChange` 收尾。在它末尾加一次 capture。但要注意顺序——**结构操作前要先 flush 值类组**（否则结构操作前未固化的键入会和结构操作混在一个快照里，undo 粒度变粗）：

```ts
// 现有结构 mutation 模板（以 addArrayItem 为例）
addArrayItem(defaultItem, arrayFieldChains, position) {
    this.beforeStructuralChange();              // ① 先 flush 值类组（固化未 capture 的键入）
    const obj = getFieldObj(this.editingObject, arrayFieldChains);
    obj.push(defaultItem);                       // ② 改 editingObject
    this.structureChange(position);              // ③ fitView=FitId + bumpStructure + capture
}

private beforeStructuralChange() { this.flushValueCoalesce(); }

private structureChange(position) {
    this.fitView = EFitView.FitId;
    this.fitViewToIdPosition = position;
    this.bumpStructure({fitView: FitId, position});   // ④ 参量化（技术债 D4）
    this.undo.capture(this.captureUndoPoint());        // ⑤ capture 改后的状态
}
```

> **为什么 `bumpStructure` 要参量化**（技术债 D4）：现有 `structureChange` 把 `fitView=FitId` 写死、`replaceEditingObject` 手动设 `FitFull` 走 private `bumpStructure` 绕开。改成 `bumpStructure({fitView, position?})` 一个入口、一份策略表，undo 传 `{fitView: NoChange}`。同时要给 `EFitView` 加一个 `NoChange` 枚举值、给 `useEntityToGraph` 加对应分支（不调 `fitView`/`setViewport`，保持当前视口）——否则 undo 后视口会乱跳到可能已被删除的节点。

**(c) 值类合并组结束**——`flushValueCoalesce` 内的 `capture`。见 2.6。

### 2.5 undo / redo 执行

```ts
undo() {
    this.flushValueCoalesce();                    // ① 先固化未 capture 的键入（否则丢失）
    if (!this.undo.canUndo()) return;
    const target = this.undo.popUndo();           // ② 要恢复成的快照（前一个 / baseline）
    this.applyUndoPoint(target);                  // ③ editingObject = structuredClone(target)
    this.bumpStructure({fitView: EFitView.NoChange});  // ④ 通知 React，不跳视口
    this.notifyEditingState();                    // ⑤ 刷 HeaderBar 脏标记
}

redo() {
    if (!this.undo.canRedo()) return;
    const target = this.undo.popRedo();
    this.applyUndoPoint(target);
    this.bumpStructure({fitView: EFitView.NoChange});
    this.notifyEditingState();
}
```

执行后 React 这样刷新 UI：

```
bumpStructure → structureVersion++ → useSyncExternalStore 通知 Record
  → Record 的 useMemo 重算 entityMap（读新 editingObject）
  → 每个 Entity 新对象 → FlowNode memo 失效重渲 → EntityForm 重渲
  → EntityForm 的 useSyncFieldValue（effect 依赖 field.value）检测到值变化
  → form.setFieldValue → Form 显示 undo 后的值
```

关键：**undo 不跳视口**（`NoChange`），数据回滚但用户看着的位置不动。`useSyncFieldValue`（背景 §4.7）是"外部值 → Form"的现成同步通道，所以**结构 undo 不用改 EntityForm 接口**——Form.List 子项的响应要 P0 实测（§7）。

> **P0 里 undo/redo 走 bump，全量重算 entityMap。** 这是性能关键点，单独成 §3 讨论。简短结论：undo 是离散动作（不是连续键入），付一次重算 = 一次结构 add 的代价；值类 undo 若实测卡，§3 给轻量化出路。

### 2.6 值类编辑的合并（coalescing）

值类编辑（`updateFormValues` / `updateNote`）是**每键触发**的（`onValuesChange` 每键一调）。如果逐键入栈，"打一个字 = 一步 undo"，体验恶劣。所以要**合并**（业界共识，Lexical/ProseMirror/Yjs 都用 ~500ms 时间窗）。

合并规则：

| 事件 | 处理 |
|---|---|
| 键入，且与当前组合并（同字段 + 在 500ms 窗口内） | 重置定时器，**不入栈** |
| 键入，换字段了 | 关闭旧组（capture）、开新组 |
| 定时器到期（500ms 无新键入） | 关闭组（capture） |
| 字段失焦 blur | 关闭组（capture） |
| 任何结构操作（`beforeStructuralChange`） | 关闭组（capture），再执行结构操作 |
| undo / redo | 关闭组（capture），再 pop |

"关闭组（capture）" = 把当前 `editingObject` 存一份快照入栈。一个字段一次聚焦输入 = 一个 undo 步。

```ts
private flushValueCoalesce() {
    if (this.valueCoalesceTimer === undefined) return;     // 无活跃组，什么都不做
    clearTimeout(this.valueCoalesceTimer);
    this.valueCoalesceTimer = undefined;
    this.undo.capture(this.captureUndoPoint());            // 固化这段键入
    this.valueCoalesceKey = undefined;
}

// updateFormValues / updateNote 里：
private touchValueCoalesce(fieldKey: string) {
    if (this.valueCoalesceKey !== fieldKey) {
        this.flushValueCoalesce();                         // 换字段：关闭旧组
        this.valueCoalesceKey = fieldKey;
    }
    clearTimeout(this.valueCoalesceTimer);
    this.valueCoalesceTimer = setTimeout(() => this.flushValueCoalesce(), 500);
}
```

**⚠️ per-key 必须 O(1)**（性能不变量，配单测）：合并判定只做"字段标识比较 + 时间戳 + `clearTimeout`/`setTimeout`"。**严禁在每键路径上 clone、遍历 `editingObject`、浅拷 values 做 diff**——那等于每键一次轻量 bump，把"键入零重渲"的契约啃掉。

**换字段的信号从哪来**：现状 `EntityForm.tsx:763` 的 `onValuesChange` 丢了 antd 的 `changedValues`，全量灌入 `updateFormValues`，session 不知道实际改了哪个 key。要改成 `onValuesChange={(changed) => edit.editOnUpdateValues(changed)}`，合并 key 由 `changed` 算（行为不变，原全量写对未变字段是 no-op）。

**Form.List 增删移**（primitive 数组的行增删）：它现在走值类通道（`updateFormValues`），但语义是结构变更。在 `updateFormValues` 里对 array 字段做**长度 diff**：长度变 → `flushValueCoalesce`（关闭当前值类组）+ 当作结构步 capture；长度同 → 当值类合并。move（保长重排）P0 接受与键入合并的退化。

### 2.7 提交后的处理（提交边界）

`submit()` 只是把 `editingObject` 发给后端（`mutate(editingObject)`），是**异步**的，成败要等网络。所以**清栈/重基准不能在 `submit()` 调用时做**——否则提交失败会丢 undo 历史、脏标记还误报"无未保存"。

正确做法：**提交成功后**（`onSuccess`）才清栈 + 重基准。

```ts
// EditingSession
onCommitSuccess() {                     // 由 Record.tsx 的 onSuccess 调
    this.flushValueCoalesce();
    this.undo.setBaseline(this.captureUndoPoint());   // 清栈 + 新基准 = 当前已提交状态
}

// Record.tsx 的 useMutation onSuccess（resultCode == updateOk / addOk）
session.onCommitSuccess();
invalidateAllQueries();
// onError 分支：什么都不动，用户可继续编辑 / undo
```

两条路径（新记录的链路原本不通，要分开处理）：

- **更新现有记录**：`onSuccess → onCommitSuccess → invalidateAllQueries → refetch → maybeReset 真 reset`。`maybeReset` 真 reset 分支也调 `setBaseline`（统一基准变更点）。⚠️ 若 refetch 期间用户有未保存编辑（`getIsEdited()` 为 true），`maybeReset` 不应静默覆盖，要提示"服务端数据已变更，是否丢弃本地编辑"。
- **创建新记录**（`curId === NEW_RECORD_ID`，`query.enabled=!isNewRecord` 所以不 refetch、`maybeReset` 不 fire）：`onSuccess` 必须 `navigate(navTo('record', curTableId, returnedRealId, edit=true))` → `RecordWithResult` key 变 → 旧 session unmount（栈随实例销毁）→ 新 session 用真实 id 构造。

一句话：**已提交的改动不归内存 undo 回滚**（业界共识，Monaco `setValue` 清栈、Excalidraw 提交后重置）。

### 2.8 session 销毁的处理（dispose）

`<RecordWithResult key={curTableId-curId}>`——切 record 就 unmount，session 实例销毁，栈随之消失（切回原 record 是新 session、空栈）。**undo 历史不跨 record 保留**（⚠️ Q4）。

但 unmount 不能只靠 GC：coalesce 的 `setTimeout` 句柄不在 session 的 GC 图里，回调闭包持有 session 引用 → unmount 后 500ms 仍可能 fire → 对已销毁 session 跑 `structuredClone`+`capture`，且 session 无法被 GC → 内存泄漏（Tauri 桌面敏感）。所以**必须显式 `dispose()`**：

```ts
dispose() {
    this.flushValueCoalesce();          // 不丢用户最后一次键入（capture 进栈，虽然栈要销毁了）
    clearTimeout(this.valueCoalesceTimer);
    this.listeners.clear();
}

// Record.tsx unmount effect
useEffect(() => () => session.dispose(), [session]);
```

### 2.9 UI 接入（按钮 / 快捷键 / canUndo）

`canUndo` / `canRedo` 怎么响应式到达 UI？

**关键约束**：`HeaderBar` 不在 Record 路由子树（Splitter 兄弟），拿不到 session 实例；唯一寻址是模块级 `getCurrentEditingSession()`（非响应式，且分屏单值歧义）。如果按钮放 HeaderBar、在 render 期读 `canUndo()`，栈变化不经过任何 emit/resso key → 按钮 disabled 态永远滞后，重蹈"裸可变单例 render 期直读"的反模式。

**P0 决策**：

- **undo/redo 按钮放 Record 自己的工具栏 / `FlowContextMenu`**——session 直接 `useSyncExternalStore` 订阅，天然响应式、天然分屏隔离。
- **`ctrl+z` / `ctrl+y` 注册在 `Record.tsx`**（per-session，与订阅同作用域），靠 React 事件冒泡 + `e.target` 所在 DOM 子树判定归属。**不沿用 `alt+s` 的全局 `getCurrentEditingSession()` 模式**——分屏时会撤销错 session。
- HeaderBar 按钮（若产品坚持）留 P1，届时搭 resso bridge（`setUndoState` 到新 key）并接受分屏歧义代价。

`canUndo`/`canRedo` 的响应式：它们随 `structureVersion` 变化（capture/undo/redo/clear 都 bump 或在 bump 路径上），所以订阅 `structureVersion` 的组件自然拿到最新值。但要确保 capture（值类组关闭）也触发订阅更新——`flushValueCoalesce` 里的 capture 后要 `emit()` 一次（或 bump），否则按钮态不刷新。

---

## 3. 性能（你最关心的）

### 3.1 先厘清：mount ≠ 重渲

[`perf-optimization.md`](./perf-optimization.md) 实测的 **~350ms longtask**（30 节点编辑态）是**首次进编辑视图的 mount**——创建 5339 个 antd 组件的 DOM + fiber + mount 副作用（`Wave`/`useInsertStyles` 等），表单占 84%。这是"进编辑视图的一次性成本"，加载后稳态 60fps / 零 longtask。

**undo/redo 不重复这个 mount**：节点 key 稳定时 React 复用组件实例，只**重跑 render + diff**（重渲），不重建 DOM/fiber。重渲远低于 mount（量级差约 10x）。

### 3.2 P0：undo 的重渲成本

P0 里 undo/redo 走 `bumpStructure`，触发 entityMap 全量重算 → N 个 `EntityForm` 重渲（非 mount）。这个成本**等于一次结构编辑**（fold 切换 / 数组增删）的成本——perf :71 已经把"fold 切换全量重建（击穿 FlowNode memo）"列为深层 record 的**未解性能债**。

**重点：这不是快照栈特有的，也不是 undo 引入的。** 命令模式、JSON Patch 的结构 undo 同样要走 `bumpStructure` 全量重渲。卡顿根源是 entityMap 全量重算模型，换 undo 实现解决不了。

所以 P0 的策略是：

1. **先 §7 实测**结构 undo 在最大 record 上的重渲 longtask（重渲，不是 mount，预期远低于 350ms，但深层 record 仍可能可感知）。
2. **若可接受**（<50ms）：P0 完工。
3. **若不可接受**：两条出路——
   - **值类 undo 轻量化**（§3.3）：undo 撤销键入时不 bump、零重渲。
   - **结构重渲治本**（§3.4）：优化 entityMap 重算，一举两得修掉 fold/add 的现有卡顿。

### 3.3 值类 undo 轻量化（P1，按需）

值类编辑的契约是"键入不 bump"（几十表单零重渲）。**值类 undo 理应延伸这条契约——也不 bump**。但 P0 的纯快照栈里，undo 是 apply 上一份全量快照 + bump。要让值类 undo 不 bump，需要：

- **判断这次 undo 是否只涉及值类**：`diff(editingObject, target_snapshot)`，若只有叶子值不同（键集合/数组长度不变）→ 值类 undo。
- **不 bump，直接同步 Form**：通过 **form registry**（EntityForm mount 时把 `Form.useForm()` 实例按 fieldChain 注册到 session）对 diff 出的每个字段 `setFieldValue`，绕过 entityMap 重算。零重渲。

这是 P1 优化，触发条件 = §7 实测值类 undo bump 卡顿。代价：form registry（EntityForm 加注册 effect）+ undo 时一次 O(n) diff（undo 低频，可接受）。

### 3.4 结构重渲治本（独立性能项，一举两得）

结构 undo 与 fold/add 等结构编辑共享重渲链路。治本方向：

- **Entity 引用稳定化**：`createThis` 复用未变节点的 Entity 对象（只重建变化子树）→ FlowNode memo 不击穿 → 只 undo 涉及的节点重渲，其余复用。
- 或 **entityMap 增量更新**：`bumpStructure` 带"变化子树路径"，只重算该子树。

这同时修掉 perf :71 的 fold 切换债，是 undo 之后的下一项性能工作，不阻塞 undo 落地。

---

## 4. 前置：要清的技术债

undo 落地前必须或强烈建议先清理：

| # | 债务 | 清法 | 工作量 |
|---|---|---|---|
| **D4** | `bumpStructure`/`structureChange` private + fitView 强耦合；`EFitView` 无"保持视口"枚举值 | 参量化 `bumpStructure({fitView, position?})` + `EFitView.NoChange` + `useEntityToGraph` 加分支（§2.4/2.5 依赖它） | M |
| **D1** | fold 双源（`$fold` 持久化 + `Folds` 本地缓存）的独立 useState 在 `f6d27484` 重构后冗余：`updateFold` 双写、`interfaceOnChangeImpl` 只写 `$fold` 不一致、Folds 用 chain 做 key 在结构变更后变孤儿；Folds 的"React 信号"旧职责已被 `structureVersion` 接管（反证：`interfaceOnChangeImpl` 不调 setFolds 但 UI 仍刷新） | 保留两概念层（`$fold` 持久化 + `Folds` 本地缓存），但 `Folds` 改**从 `$fold` 派生**（不再独立 useState）——详见下方 fold 专节 | M |
| **D5** | `submit` 不动 `originalEditingObject`，提交后脏基准不重置 | `onCommitSuccess` 里 `setBaseline`（§2.7，设计内含） | S |

顺带：`isDeeplyEqual` 导出 + O(k²)→Set（D6）。`updateFormValues` 的 `toInt/toFloat` 不可逆→接受"值类 undo 还原到转换后有效值"（D3）。

### fold 双源澄清（D1 详）

你已定 fold **既要持久化又要本地缓存，都需要**——这满足：保留两个**概念层**（`$fold` 持久化 + `Folds` 本地缓存），只是 `Folds` 的**实现**从"独立 useState"改成"从 `$fold` 派生"。理由是独立 useState 在 `f6d27484` 重构后已冗余且带 bug：

- **`$fold` 是后端 day-1 持久化字段**（commit `5b03455b`，`app/src/main/java/configgen/value/ValueJsonParser.java:67,96-99` 的 `jsonExtraKeySet = Set.of("$type","$note","$fold","$refs")`，与 `$note` 同构；`ValueToJson.java:68` 序列化回 JSON）。**持久化层，保留不动**。
- **`Folds` useState 的历史作用是"React 可观察信号"**——`5b03455b` 时代旧 `editState` 就地变异不触发重渲，`setFolds` 是唯一驱动 Record 重渲的信号。**`f6d27484` 重构引入 `structureVersion` 后，`updateFold` 的 `bumpStructure` 已驱动 Record 重渲读最新 `$fold`**，`Folds` 在 `Record.tsx:129` 的 useMemo deps 里已是冗余依赖。
- 独立 useState 还带三个 bug：①`interfaceOnChangeImpl`（`:321`）只写 `$fold` 不写 Folds → 切 impl 不一致；②Folds 用 chain（含数组下标）做 key，结构变更（swap/delete）后下标平移 → 旧 chain 条目变孤儿，而 `$fold` 长在对象上跟着走；③undo 难同步（Folds 不在 snapshot）。

**清法（保留两概念层 + 消除冗余根源）**：
- **`$fold`**：持久化层，不动。
- **`Folds`**：本地缓存层，**改为从 `$fold` 派生**——`Folds` 类与单测保留（`domain/folds.ts` 不删，概念/类型仍在）；但 `Record.tsx:67` 的 `useState<Folds>` 与 creator 的 `folds/setFolds` 参数去掉；`getFoldState` 三处调用点（`recordEditEntityCreator.ts:68/505/578`，三处都持有 `obj`）直接读 `obj?.$fold`；`updateFold`（`editOnUpdateFold`）只调 `session.updateFold`，去掉 `setFolds`。
- **收益**：`interfaceOnChangeImpl` 不一致自动消失；结构变更后 fold 跟对象走（无 chain 孤儿）；**undo/redo fold 零额外逻辑**（`$fold` 在 snapshot，恢复后 entityMap 重算读新 `$fold`，派生 Folds 自动同步）；顺带纠正 `Record.tsx:66` 的误导注释（"`folds` 跟 notes 一样临时存"——实际 `$fold` 进提交载荷）。

> **为什么不让 `Folds` 保持独立 useState**（曾考虑、已否决）：要给 session 加 fold 同步通道（`foldsVersion` 订阅 key 或 `setExternalFoldsRebuilder` 回调），破坏 session/React 解耦契约（`editingSession.ts:8-30`）；且 `interfaceOnChangeImpl` bug 与 chain 孤儿仍需单独修；undo fold 同步复杂。代价显著高于派生，无收益。

---

## 5. 为什么选快照栈（其他方案对比）

四种模式（原理与现状.md 第一部分）在本项目的取舍：

- **CRDT / Yjs**：要把 `editingObject` 重写成 `Y.Map`/`Y.Array`，推翻 editObj 引用语义，协同收益单用户用不到。**放弃。**
- **命令模式**：朴素命令靠"按 fieldChain 地址重放逆"，但本项目数组下标会随增删/swap/前插**整体平移**，旧地址失效。要安全就得"针对当前树重算地址"，为 8+ 种 mutation 各写逆逻辑，且 `replaceEditingObject`/`pasteStruct`/`updateInterfaceValue` 这些整体替换仍需快照。收益只是省内存，而省内存 JSON Patch 能更干净地达成。**性价比最低。**
- **JSON Patch**（fast-json-patch）：理论上最契合（树状 JSON、省内存、就地 apply 兼容共享引用），但两个前置难点需 spike：①fieldChain/`$`-key 与 RFC 6901 JSON Pointer 的互译；②baseline 必须 `structuredClone` 独立（fast-json-patch 默认就地改，会污染）。**留 P1.5**：§3.4 的 entityMap 优化做完后，若内存仍是问题，再换 patch——只动 `captureUndoPoint`/`applyUndoPoint` 两个方法体。
- **快照栈（P0 选它）**：与 `bumpStructure` 同构、正确性最高（无需写逆逻辑/无 diff 引擎/无互译歧义）、capture/apply 点集中（升级路径开放）。唯一代价是内存，由 `maxDepth`/预算兜底，大 record 的 clone 成本 §7 实测。

**为什么 P0 不直接上 Patch**：Patch 的内存收益是"可能需要"而非"已知需要"，要 §7 数据支撑；P0 优先正确性与落地速度（先让用户用上 undo、验证 coalescing UX），证据足了再换——这不是为便利妥协，是证据不足以支撑更复杂的方案。

---

## 6. 待你拍板的决策点（⚠️）

| # | 决策 | 推荐 | 理由 |
|---|---|---|---|
| Q1 | P0 范围 | 值类 + 结构类 | 执行路径统一；§7 实测先过 |
| Q2 | 模式 | 快照栈 P0，Patch 留 P1.5 | §5 |
| Q3 | 提交边界 | 清栈/重基准在 onSuccess | submit 异步（§2.7） |
| Q4 | 栈生命周期 | per-session + dispose | 切 record 丢历史（§2.8） |
| Q5 | coalesce 边界 | 500ms + blur + 换字段 + 结构 op + undo/redo + Form.List 长度变 | §2.6，对齐业界 500ms |
| Q6 | undo 时 fitView | 不跳（`NoChange`） | §2.5；跳焦点留 P1 |
| Q7 | 快捷键注册点 | Record.tsx per-session | 分屏隔离（§2.9） |
| Q8 | 前置清债 | D1（M）、D4（M）、D5（S） | §4 |
| Q9 | `$fold` 是否后端契约？ | **已定（你确认）：是**——持久化、保留不剥离 | §4 D1 |
| Q10 | undo/redo 按钮位置 | Record 工具栏（非 HeaderBar） | 响应式 + 分屏（§2.9） |
| Q11 | 栈上限 | 条数（maxDepth=50）先上；若大 record 内存重，改字节预算（如 32MB）驱动 | §2.2 |
| Q12 | 快捷键名 | ctrl+z / ctrl+y | 实测 Tauri/浏览器冲突 |
| Q13 | 后台推数据覆盖本地编辑时 | getIsEdited 时提示"是否丢弃" | §2.7 maybeReset 守卫 |
| Q14 | fold 是否进 undo？ | 是（持久化数据；`Folds` 改派生后 `$fold` 在 snapshot 自然覆盖，几乎免费） | §4 D1 |
| Q15 | 值类 undo 走 bump（P0）还是 form registry 轻量？ | **P0 走 bump（简单先落地）；§7 实测若卡，P1 上 form registry 轻量** | §3.2/3.3 |

---

## 7. 风险与验证

**P0 阻塞验证（实现前/中必须过）**：
1. **结构 undo 重渲 longtask**（最大 record）：实测撤销一次结构操作（如 add）的 longtask。这是重渲非 mount（预期远低于 350ms），但深层 record 仍可能可感知（perf :71 同路径债）。若 >50ms → 触发 §3.4 entityMap 优化，或结构 undo 加 `startTransition`。
2. **值类 undo 重渲 longtask**：P0 走 bump，实测撤销键入的 longtask。若 >50ms → 触发 §3.3 form registry 轻量化（Q15）。
3. **Form.List 同步**：实机 undo 含 Form.List 增删移的 record，断言行数与值正确刷新。若 antd Form.List 不响应 `setFieldValue` → 需 `key={structureVersion+fieldChain}` 强制 remount。
4. **Tab 遍历 clone 累计**：最大 record 上 Tab 遍历 N 字段，值类组反复关闭的累计 `structuredClone` longtask。若不可接受 → lazy snapshot（组关闭只记 marker，undo 时才 clone）。

**常规 spike**：内存 RSS（对照 Q11）；coalesce UX（500ms/blur 符不符直觉）；快捷键冲突（Q12）；`useSyncFieldValue` 覆盖度。

---

## 8. 实现路线图

| 阶段 | 内容 | 依赖 |
|---|---|---|
| **0. 清债** | D4（参量化 `bumpStructure` + `EFitView.NoChange` + useEntityToGraph 分支）、D1（fold：`$fold` 持久化保留 + `Folds` 改派生）、D5（`onCommitSuccess`）、D6（顺带）、改 `onValuesChange` 传 changedValues | — |
| **1. P0 阻塞验证** | §7 的 1/2/3/4 四项 spike | 阶段 0 |
| **2. UndoStore + session 接入** | 纯数据 `UndoStore`（§2.2）；session 的 `setBaseline/capture/undo/redo/onCommitSuccess/dispose/flushValueCoalesce`；参量化 `bumpStructure` 接入；`maxDepth` 封顶 | 阶段 1 |
| **3. capture + coalescing** | 结构 mutation 前置 `beforeStructuralChange` + 后置 capture（§2.4）；值类 coalescing（per-key O(1) 不变量 + 单测）；Form.List 长度 diff（§2.6） | 阶段 2 |
| **4. 提交边界 + 生命周期** | `onCommitSuccess`；新记录 navigate；maybeReset 守卫（§2.7）；unmount `dispose`（§2.8） | 阶段 2 |
| **5. UI 接入** | Record 工具栏 undo/redo 按钮（订阅 `canUndo`/`canRedo`）；`ctrl+z/y` per-session 注册（§2.9） | 阶段 3 |
| **6. 按需优化** | 若 §7 实测卡：值类 undo form registry 轻量（§3.3）；或 entityMap 引用稳定化（§3.4） | 阶段 5 |

每阶段配套：`domain/` 纯逻辑单测（栈语义、coalescing、per-key O(1)、maxDepth）+ [`状态管理-总结与演进.md`](./状态管理-总结与演进.md) §8.4 编辑交互清单实机回归。

---

## 一页速记

- **做什么**：record 编辑态的 undo/redo（≠ 导航历史 alt+c/v）。覆盖值类 + 结构类，session 内，不跨 record、不回滚已提交。
- **怎么做**（快照栈 P0）：每次编辑后存 `editingObject` 全量深拷；undo = apply 上一份快照 + `bumpStructure({fitView:NoChange})`；redo 对称。`UndoStore` 纯数据类，`undo/redo` 是 session 方法。
- **capture 时机**：初始/提交后 `setBaseline`（mount effect，不在构造函数）；结构操作前 `flushValueCoalesce` + 操作后 capture；值类组结束（500ms/blur/换字段/结构 op/undo·redo）capture。
- **coalescing**：值类逐键合并（500ms 窗口），**per-key O(1)** 不变量。换字段信号来自 `changedValues`（改 onValuesChange）。
- **提交边界**：`onSuccess` 调 `onCommitSuccess` 清栈+重基准（submit 异步，不能同步清）；新记录 navigate 真实 id；maybeReset 守卫未保存编辑。
- **生命周期**：per-session，unmount 必须 `dispose()`（清定时器 + flush + 清 listeners）。
- **UI**：按钮放 Record 工具栏 + 快捷键 per-session 注册（非 HeaderBar、非全局 getCurrentEditingSession）。
- **性能**：undo 是重渲非 mount（远低于 350ms mount），成本 = 结构编辑（fold/add）。**非快照栈特有**。P0 走 bump；值类 undo 若卡 → P1 form registry 轻量；结构重渲治本 → Entity 引用稳定化（一举两得修 perf :71 债）。
- **前置清债**：D4（参量化 bump + NoChange，M）、D1（fold：`$fold` 持久化保留 + `Folds` 改派生消除冗余双源，M）、D5（onCommitSuccess，S）。
- **升级路径**：`captureUndoPoint`/`applyUndoPoint` 是契约点，P1.5 换 JSON Patch 只动这两处。

## 参考
- [`undo-redo-原理与现状.md`](./undo-redo-原理与现状.md) —— 业界四模式 + cfgeditor 编辑管线现状（本文背景）。
- [`状态管理-总结与演进.md`](./状态管理-总结与演进.md) —— EditingSession / resso / useSyncExternalStore 教学。
- [`perf-optimization.md`](./perf-optimization.md) —— 性能契约背景与实测数据。

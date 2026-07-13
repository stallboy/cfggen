# 编辑操作 Undo/Redo（cfgeditor）

> 本文描述 cfgeditor record 编辑态 undo/redo 的实现现状。
> 背景：[undo-redo-原理与现状.md](./undo-redo-原理与现状.md)（业界 undo/redo 模式 + cfgeditor 编辑管线）。

## 1. 概述

给 record 编辑器提供 undo/redo：在一条 record 的编辑态里撤销/重做刚才的编辑（增删数组项、改字段值、换 impl、粘贴、AI/JSON 整体写入……）。

**与导航历史正交**：`historyModel.ts`（`alt+c`/`alt+v`）是「访问过哪些 record」的导航历史；undo/redo 是「当前 record 改了什么」的编辑撤销。两者数据流不交叉。

**范围**：
- 覆盖：结构类编辑（增/删/前插/上下移/折叠/换 impl/粘贴/整体替换）+ 值类编辑（primitive 键入 / note），在当前编辑会话内。
- 不覆盖：跨 record 的 undo（切走再切回不保留旧 record 历史）；已提交数据的回滚；多人协同；纯 UI 局部态（如 `ArrayItemExpandButton` 的展开收起）。

**前置事实**：`EditingSession`（每条 record 一个实例）就地变异 `editingObject`，靠 `structureVersion`（number）接入 React：

- 值类编辑**不 bump**（键入时几十表单零重渲，性能契约1）；
- 结构类编辑 **bump + emit**（entityMap 重算，性能契约2）。

undo 引擎内建在 session 里、复用 `bumpStructure` 这条现成的结构变更通道——不外包给不可变 store 库（与就地变异根本冲突）。

---

## 2. 实现

### 2.1 核心思路：快照栈

每次编辑后存一份 `editingObject` 的**全量深拷贝**（snapshot）。undo = apply 上一份快照；redo = apply 下一份。栈随 session 生灭，提交后清空。不为各编辑写逆逻辑、不引入 diff 引擎——正确性靠全量快照天然保证。

### 2.2 UndoStore：纯数据栈

`domain/undoStore.ts`，只管栈，不依赖 session、不调 React。三段语义：

- `baseline`：初始 / 最近一次提交后的状态。undo 到栈底恢复成它（显式存住，无 off-by-one）。
- `done`：操作后快照；`done[末]` = 最近。`capture` 入栈、`popUndo` 弹出。
- `undone`：已 undo、可 redo。`popRedo` 弹出。

```ts
type Snapshot = JSONObject;

class UndoStore {
    private baseline!: Snapshot;
    private done: Snapshot[] = [];
    private undone: Snapshot[] = [];
    private readonly maxDepth = 50;

    /** 初始 / 提交后调：重置基准，清栈。 */
    setBaseline(s: Snapshot) {
        this.baseline = s;
        this.done = [];
        this.undone = [];
    }

    /** 每次编辑后调：入栈新快照，丢弃 redo 历史（分叉）。超 maxDepth 丢最旧。 */
    capture(s: Snapshot) {
        this.undone = [];
        this.done.push(s);
        if (this.done.length > this.maxDepth) this.done.shift();
    }

    canUndo() { return this.done.length > 0; }
    canRedo() { return this.undone.length > 0; }

    /** undo：返回"要恢复成的状态"（前一个快照或 baseline）。 */
    popUndo(): Snapshot {
        const s = this.done.pop()!;
        this.undone.push(s);
        return this.done.length > 0 ? this.done[this.done.length - 1] : this.baseline;
    }

    /** redo：返回"要恢复成的状态"（刚 redo 的快照）。 */
    popRedo(): Snapshot {
        const s = this.undone.pop()!;
        this.done.push(s);
        return s;
    }
}
```

要点：

- **Snapshot 必须独立深拷**（`structuredClone`），不能存 `editingObject`/`editObj` 引用——它们会被后续就地变异污染，存引用等于存一个会被改的活对象。clone 由 session 的 `captureUndoPoint` 负责，`UndoStore` 只存调用方传入的已 clone 对象（不二次 clone）。
- **分叉**：`capture` 清空 `undone`——undo 后又新编辑，redo 历史作废（与所有编辑器一致）。
- **maxDepth=50**：栈深硬上限，超限丢最旧（大 record 的内存兜底）。

栈语义由 `domain/undoStore.test.ts` 独立单测（栈深、分叉、baseline 栈底、maxDepth 封顶）。

### 2.3 EditingSession 接入

`UndoStore` 是纯数据；capture/apply 时机与 React 驱动由 `EditingSession` 负责。session 内 undo/redo 相关成员：

| 成员 | 职责 |
|---|---|
| `undoStore: UndoStore` | 快照栈实例（命名 `undoStore` 而非 `undo`，避免与 `undo()` 方法同名——TS 不允许同名的属性与方法） |
| `valueCoalesceTimer` / `valueCoalesceKey` | 值类合并的定时器句柄 / 当前合并组的字段标识 |
| `initUndoBaseline()` | mount effect 调一次，设初始基准 |
| `undo()` / `redo()` | 入口（见 2.5） |
| `canUndo` / `canRedo` | 箭头属性（绑 this），供 `useSyncExternalStore` 订阅 |
| `onCommitSuccess()` | 提交成功后清栈 + 重基准（见 2.7） |
| `dispose()` | unmount 清理（见 2.8） |
| `captureUndoPoint()` | `structuredClone(editingObject)` |
| `applyUndoPoint(s)` | `editingObject = structuredClone(s)` |
| `structureChange(position)` | 结构操作收尾：`bumpStructure({FitId, position})` + capture |
| `beforeStructuralChange()` | 结构操作前置：flush 值类组 |
| `flush/touch/coalesceKey` | 值类合并（见 2.6） |
| `resetBaselines()` | 重置脏比较基准 + undo baseline（提交/reset 后） |

`captureUndoPoint` / `applyUndoPoint` 是**升级契约点**：现为 `structuredClone`，将来若换 JSON Patch 只动这两个方法体，上层不动。

### 2.4 capture：什么时候存快照

四个时机：

**(a) 初始 / 提交后 —— setBaseline**

session 构造后由 Record 的 mount effect 调 `initUndoBaseline()`（不在构造函数里——构造函数在 render 期，`structuredClone` 是副作用，挪到 effect）。提交成功后由 `onCommitSuccess → resetBaselines` 再调 `setBaseline`。

```ts
// Record.tsx
useEffect(() => {
    session.initUndoBaseline();          // 幂等：StrictMode 双调安全
    return () => session.dispose();
}, [session]);
```

**(b) 结构类编辑后 —— capture**

每个结构 mutation（`addArrayItem`/`addArrayItemAtIndex`/`deleteArrayItem`/`swapArrayItem`/`updateFold`/`updateInterfaceValue`/`pasteStruct`）都走统一模板：

```ts
addArrayItem(defaultItem, arrayFieldChains, position) {
    this.beforeStructuralChange();              // ① flush 值类组（固化未 capture 的键入）
    const obj = getFieldObj(this.editingObject, arrayFieldChains);
    obj.push(defaultItem);                       // ② 改 editingObject
    this.structureChange(position);              // ③ bumpStructure({FitId, position}) + capture
}

private structureChange(position) {
    this.bumpStructure({fitView: EFitView.FitId, position});
    this.undoStore.capture(this.captureUndoPoint());
}
```

结构操作前必须先 flush 值类组，否则结构操作前未固化的键入会和结构操作混在一个快照里，undo 粒度变粗。

**(c) 整体替换 —— replaceEditingObject**

`replaceEditingObject`（Chat/AddJson 写入 / funcClear）用 `FitFull`（要重算视口），不走 `structureChange`，单独 capture：

```ts
replaceEditingObject(newEditingObject) {
    this.beforeStructuralChange();
    deleteRefsInPlace(newEditingObject);
    this.editingObject = newEditingObject;
    this.bumpStructure({fitView: EFitView.FitFull});
    this.undoStore.capture(this.captureUndoPoint());
}
```

**(d) 值类合并组结束 —— flushValueCoalesce 内的 capture**。见 2.6。

另：**Form.List 长度变化**（primitive 数组的行增删）走值类通道（`updateFormValues`），但语义是结构变更。在 `updateFormValues` 里对 array 字段做长度 diff——长度变：`flushValueCoalesce` + 写回 + capture（当结构步）；长度同：当值类合并。

### 2.5 undo / redo 执行

```ts
undo() {
    this.flushValueCoalesce();                    // ① 先固化未 capture 的键入（否则丢失）
    if (!this.undoStore.canUndo()) return;
    const target = this.undoStore.popUndo();      // ② 要恢复成的快照（前一个 / baseline）
    this.applyUndoPoint(target);                  // ③ editingObject = structuredClone(target)
    this.bumpStructure({fitView: EFitView.NoChange});  // ④ 通知 React，不跳视口
}
redo() { /* 对称：popRedo，其余相同 */ }
```

`bumpStructure` 内含 `notifyEditingState`（刷 HeaderBar 脏标记）+ `emit`（通知 `useSyncExternalStore` 订阅者）。

**undo 不跳视口**（`EFitView.NoChange`）：数据回滚但用户看着的位置不动。

**UI 同步链路**：

```
bumpStructure → structureVersion++ → emit
  → useSyncExternalStore 通知 Record
  → Record 的 useMemo 重算 entityMap（读新 editingObject）
  → Entity 新对象 → FlowNode memo 失效重渲 → EntityForm 重渲
  → useSyncFieldValue（依赖 field 引用）检测到 field 新对象
  → form.setFieldValue → Form 显示 undo 后的值
```

`useSyncFieldValue`（`flow/EntityForm.tsx`）依赖 **field 引用**（entityMap 每次重算都是新对象）而非仅 `field.value`——保证 undo/redo 后 effect 必跑、重新评估是否需要同步；但只在 `form.getFieldValue(name) !== field.value` 时才 `setFieldValue`，值一致的字段跳过。这是值类 undo 正确性 + 性能的关键：

- **值类 undo**（脱节）：值类编辑不重算 entityMap（契约1），`field.value` 快照停在旧值、而 antd `Form` 内部已被用户输入改到新值；undo 让 editingObject 回旧值、entityMap 重算出 `field.value=旧值`，此时 `getFieldValue(新) !== field.value(旧)` → set，同步成功。
- **一致字段跳过**：entityMap 重算时多数字段 value 并未变（如不相关的结构编辑）。若也 set，`@rc-component/form` 的 setField 分支对路径匹配字段是**无条件 forceUpdate（不比较值）**，会让这些字段多一次额外重渲。`getFieldValue` 比较挡掉它——结构编辑只重渲真正变化的字段，不会「所有 form item 全刷」。

antd `setFieldValue` by design 不触发 `onValuesChange`，不会回流污染 coalescing 栈。

### 2.6 值类编辑的合并（coalescing）

值类编辑（`updateFormValues`/`updateNote`）每键触发。逐键入栈会让「打一个字 = 一步 undo」，所以合并（500ms 时间窗，业界共识：Lexical/ProseMirror/Yjs 同档）。

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
    this.undoStore.capture(this.captureUndoPoint());
    this.valueCoalesceKey = undefined;
    this.emit();   // capture 不 bump structureVersion，但 canUndo 变了 → emit 让按钮订阅刷新
}
```

**per-key 必须 O(1)**（性能不变量，配单测）：合并判定只做「字段标识比较 + `clearTimeout`/`setTimeout`」。严禁在每键路径上 clone、遍历 `editingObject`、浅拷 diff——那会啃掉「键入零重渲」的契约。

**换字段的信号**：`EntityForm` 的 `onValuesChange(changed, allValues)` 把两者都传给 `updateFormValues`——`allValues` 写回 editingObject，`changed` 算 coalescing key + Form.List 长度 diff。

值类 coalescing 由 `editingSession.test.ts` 单测（同字段合并、换字段关闭、timer 到期、Form.List 长度变/同、per-key O(1)、结构操作前 flush、dispose flush）。

### 2.7 提交边界

`submit()` 只把 `editingObject` 发给后端（`mutate(editingObject)`），异步、成败要等网络。所以**清栈/重基准在 `onSuccess` 而非 `submit()` 调用时**——否则提交失败会丢 undo 历史、脏标记还误报「无未保存」。

```ts
// EditingSession
onCommitSuccess() { this.resetBaselines(); }      // 由 Record.tsx 的 onSuccess 调

private resetBaselines() {
    clearTimeout(this.valueCoalesceTimer);         // 清 coalesce（旧键入已落库，不保留 undo）
    this.valueCoalesceTimer = undefined;
    this.valueCoalesceKey = undefined;
    const snap = this.captureUndoPoint();
    this.originalEditingObject = snap;             // 脏比较基准归零（isEdited → false）
    this.undoStore.setBaseline(snap);              // 清栈 + 新基准 = 当前已提交状态
}

// Record.tsx 的 useMutation onSuccess（resultCode == updateOk / addOk）
session.onCommitSuccess();
invalidateAllQueries();
if (curId === NEW_RECORD_ID) {
    navigate(navTo('record', curTableId, editResult.id, true));   // 新记录跳真实 id
}
// onError 分支：什么都不动，用户可继续编辑 / undo
```

`originalEditingObject` 与 `undoStore.baseline` 共享同一 clone（两者都只读不被 mutate，`popUndo` 返回 baseline 引用但 `applyUndoPoint` 会 clone，不污染），省一次 `structuredClone`。

**新记录**（`curId === NEW_RECORD_ID`）：`onSuccess` navigate 到返回的真实 id → `RecordWithResult` key 变 → 旧 session unmount（栈随实例销毁）→ 新 session 用真实 id 构造。

**后台推数据覆盖**：refetch → `maybeReset` 真 reset 分支也调 `resetBaselines`。`maybeReset` 的守卫（未保存编辑时提示是否丢弃）尚未实现——目前真 reset 会静默覆盖本地编辑。

一句话：**已提交的改动不归内存 undo 回滚**（与 Monaco `setValue` 清栈、Excalidraw 提交后重置一致）。

### 2.8 生命周期（dispose）

`<RecordWithResult key={curTableId-curId}>`——切 record 就 unmount，session 实例销毁，栈随之消失（切回是新 session、空栈）。**undo 历史不跨 record 保留**。

unmount 不能只靠 GC：coalesce 的 `setTimeout` 句柄不在 session 的 GC 图里，回调闭包持有 session 引用 → unmount 后 500ms 仍可能 fire → 对已销毁 session 跑 capture，且 session 无法被 GC → 内存泄漏（Tauri 桌面敏感）。所以必须显式 `dispose()`：

```ts
dispose() {
    this.flushValueCoalesce();      // 不丢用户最后一次键入
    clearTimeout(this.valueCoalesceTimer);
    this.listeners.clear();
}
// Record.tsx unmount effect（见 2.4a）
```

### 2.9 UI 接入（按钮 / 快捷键 / canUndo）

`Record.tsx` 用 `useSyncExternalStore` 订阅 `canUndo`/`canRedo`（随 capture/undo/redo 变化，`flushValueCoalesce` 的 emit 触发刷新）：

```ts
const canUndo = useSyncExternalStore(session.subscribe, session.canUndo);
const canRedo = useSyncExternalStore(session.subscribe, session.canRedo);
```

**按钮**：画布右键菜单（`paneMenu`）加 undo/redo 项，`disabled` 绑 `!canUndo`/`!canRedo`。放在 Record 自己的作用域（session 直接订阅，天然响应式、天然分屏隔离），不放 `HeaderBar`（HeaderBar 是 Splitter 兄弟、拿不到 session 实例）。

**快捷键**：`ctrl+z` / `cmd+z`（undo）、`ctrl+y` / `ctrl+shift+z` / `cmd+y` / `cmd+shift+z`（redo），per-session 注册在 `Record.tsx`（与订阅同作用域）。`enableOnFormTags` 拦截 input 默认 undo，由 session coalescing 接管。

```ts
useHotkeys('ctrl+z, cmd+z',
    (e) => { e.preventDefault(); session.undo(); },
    {enableOnFormTags: true, enabled: isEditing});
useHotkeys('ctrl+y, ctrl+shift+z, cmd+y, cmd+shift+z',
    (e) => { e.preventDefault(); session.redo(); },
    {enableOnFormTags: true, enabled: isEditing});
```

不沿用 `alt+s` 的全局 `getCurrentEditingSession()` 模式——分屏时会撤销错 session。

---

## 3. 性能契约

| 契约 | 内容 |
|---|---|
| **契约1** | 值类编辑（`updateFormValues`/`updateNote`）不 bump `structureVersion`、不 emit → Record 不重渲、entityMap 不重建。几十个表单输入零重渲。 |
| **契约2** | 结构类编辑 bump + emit → Record 重渲 → entityMap 重算；重算时读 `getEditingObject()`（共享引用，entity 闭包自动见最新值）。 |

undo/redo 走 `bumpStructure({NoChange})`，触发 entityMap 全量重算——成本等于一次结构编辑（重渲，非 mount；节点 key 稳定时 React 复用组件实例，只重跑 render + diff）。undo 是离散动作（非连续键入），付一次重算可接受。值类 coalescing 的 per-key 路径严格 O(1)（见 2.6），不触碰契约1。

---

## 一页速记

- **做什么**：record 编辑态的 undo/redo（≠ 导航历史）。覆盖值类 + 结构类，session 内，不跨 record、不回滚已提交。
- **怎么做**（快照栈）：每次编辑后存 `editingObject` 全量深拷；undo = apply 上一份快照 + `bumpStructure({NoChange})`；redo 对称。`UndoStore` 纯数据类，`undo/redo` 是 session 方法。
- **栈语义**：baseline（初始/提交后）/ done / undone 三段；capture 清 undone（分叉）；maxDepth=50 封顶。
- **capture 时机**：初始/提交后 `setBaseline`（mount effect）；结构操作 `beforeStructuralChange` + `structureChange`（FitId + capture）；整体替换 FitFull + capture；值类组关闭 capture；Form.List 长度变当结构步。
- **coalescing**：值类逐键合并（500ms 窗口），**per-key O(1)**。换字段信号来自 `changedValues`。
- **提交边界**：`onSuccess` → `onCommitSuccess` → `resetBaselines`（清栈+重基准）；新记录 navigate 真实 id。
- **生命周期**：per-session，unmount 必须 `dispose()`（flush + 清 timer + 清 listeners）。
- **UI**：按钮放画布右键菜单 + 快捷键 per-session（`useSyncExternalStore` 订阅 canUndo/canRedo；非 HeaderBar、非全局 getCurrentEditingSession）。
- **同步**：`useSyncFieldValue` 依赖 field 引用 + `getFieldValue` 比较——保证值类 undo 后 Form 同步，且结构编辑时不重渲未变字段。
- **升级路径**：`captureUndoPoint`/`applyUndoPoint` 是契约点，换 JSON Patch 只动这两处。

## 相关文档
- [undo-redo-原理与现状.md](./undo-redo-原理与现状.md) —— 业界 undo/redo 模式 + cfgeditor 编辑管线背景。

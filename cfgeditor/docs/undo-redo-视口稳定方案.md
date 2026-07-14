# Undo/Redo 视口稳定方案（探索）

> 关键词：`EFitView`、undo/redo、视口、`computeStableViewport`、布局重排
> 关联文档：[fitview-视口适配机制](./fitview-视口适配机制.md)、[undo-redo-设计](./undo-redo-设计.md)、[undo-redo-原理与现状](./undo-redo-原理与现状.md)
> 状态：**已采纳 S5（上个操作节点锚点）**，落地中。

---

## 0. 一句话

undo/redo 现在统一用 `EFitView.NoChange`（视口不动）。这对**值类 undo** 正确（布局不变），但对**结构 undo**（删/加节点、展开/收起、swap、换 impl、粘贴）会触发 ELK 重排——节点世界坐标全变，视口死死不动，于是 `screen = world·zoom + vp` 让画面整体跳。修法：undo/redo 布局发生变化时，让**「被撤销那个操作」的锚点节点屏幕坐标保持不变**（复用现成的 `computeStableViewport` 数学）——锚点 = 用户当时的视觉焦点（delete 取父节点），最贴合「用户期待其稳定」。

---

## 1. 现象与根因

### 1.1 现象

undo/redo 一个结构操作时（典型：删除一个大子树后 undo、折叠/展开一个嵌套结构后 undo），整张图猛地一跳，用户视线丢失。

### 1.2 当前实现

`services/editingSession.ts`：

```ts
undo() {
    this.flushValueCoalesce();
    if (!this.undoStore.canUndo()) return;
    const target = this.undoStore.popUndo();
    this.applyUndoPoint(target);                         // editingObject 恢复成快照
    this.bumpStructure({fitView: EFitView.NoChange});    // ← 不跳视口
}
redo() { /* 对称，同样 NoChange */ }
```

`bumpStructure` 干三件事：`structureVersion++`（Record 重渲、entityMap 重算）→ `onStructureChange()`（`removeQueries(['layout', pathname, 'e'])` 清布局缓存）→ `emit()`。

`flow/useEntityToGraph.ts` 的 Effect 2 读 `editingObjectRes.fitView`：

```ts
const action = pickViewportAction(editingObjectRes, id2RectMap, getViewport());
// NoChange → {kind:'noop'} → 不调 fitView/setViewport，视口参数 (vp.x, vp.y, zoom) 原封不动
```

### 1.3 根因：NoChange 的隐含前提被打破

NoChange 的语义是「**不动视口参数**」。它隐含一个前提：**布局（节点世界坐标）也不变**。前提成立时，`screen = world·zoom + vp`，world 不变、vp 不变 → 屏幕不变 → 不跳。

但 undo **结构操作**时：

1. `applyUndoPoint` 把 `editingObject` 回滚成快照（拓扑变了：节点数 / `$fold` / `$impl` / 引用关系不同）。
2. `bumpStructure` → `removeQueries` 清掉 layout 缓存。
3. entityMap 重算出**新的 nodes/edges**（拓扑变了）。
4. `useEntityToGraph` 的 `useQuery` 缓存已空 → 重新跑 ELK → **全新的世界坐标**。
5. Effect 2：`fitView=NoChange` → `pickViewportAction` 返回 `noop` → 视口参数不动。
6. world 变了、vp 没变 → `screen` 全变 → **画面跳**。

### 1.4 精确边界：什么时候跳

| undo 的操作 | 拓扑/尺寸变化 | ELK 输出 | 是否跳 |
|---|---|---|---|
| 值类（primitive 键入 / note）且**不改尺寸** | 否 | 输入相同 → 输出相同（ELK 确定性） | 不跳 ✅ NoChange 正确 |
| 值类但**改了节点尺寸**（如长字符串行数变化） | 尺寸变 | 输入变 → 输出变 | 轻微跳（局部） |
| 结构（增/删/swap/fold/impl/paste/replace） | 是 | 输入变 → 输出变 | **大跳** ❌ |

> 注：值类 undo 也走 `removeQueries`（缓存被清、ELK 重跑一次），但因为输入不变，输出坐标与上一帧**逐位相同**，world 不变，NoChange 不跳。结构 undo 输入真变了，才跳。

**结论**：问题精确地发生在「undo 前后 ELK 输入不同」时，主因是结构变更。

---

## 2. 为什么「不跳」必须绑定到具体节点

### 2.1 两种「稳定」的辨析

- **(a) 视口参数稳定**（`vp.x/vp.y/zoom` 不变）= 现状 NoChange。world 变 → 图跟着跳。
- **(b) 视觉稳定**（用户看到的东西位置尽量不动）。这才是目标。

### 2.2 「保持视口中心的世界坐标不变」= NoChange（证伪）

一个诱人的简单方案：undo 后 `setViewport`，使 undo 前的「视口中心世界坐标」`W_old = (screenCenter − vp)/zoom` 仍落在屏幕中心。

代入：`newVp = screenCenter − W_old·zoom = screenCenter − (screenCenter − vp_old) = vp_old`。

**结果就是 `vp_old` 本身，与 NoChange 数学等价，不解决问题。** 因为「世界坐标系的同一点」undo 前是节点 A、undo 后是节点 B，中心点的节点变了，照样跳。

### 2.3 唯一可行：锚点节点稳定（已有的 `computeStableViewport`）

`flow/layout/viewportMath.ts` 已实现且单测覆盖：让**某个具体节点** undo 前后屏幕坐标不变。

```
要求：anchorNew·zoom + newVp === anchorOld·zoom + vp，且 newVp.zoom === vp.zoom
解：  newVp = (anchorOld − anchorNew)·zoom + vp
```

这正是正向结构操作（`FitId`）用的数学：用户盯着被改节点，relayout 后让它在屏幕上不动。**undo 复用同一套数学**——区别只在「锚点选谁、anchorOld/anchorNew 怎么拿」。

> 关键约束：anchorOld = **undo 发起那一刻**锚点的世界坐标（不是「当初操作发起时」的坐标）；anchorNew = undo 后新布局里同节点的坐标。两者要求锚点 id **undo 前后都存在**。

---

## 3. 锚点选谁：视觉焦点 = 「被撤销那个操作」的节点

undo 是「撤销最近一个操作」。用户做那个操作时，视觉焦点就在被操作的节点上；撤销它，用户期待那个节点（或其位置）稳定。所以锚点 = **被撤销操作的节点 id**：

| 被撤销的操作 | 视觉焦点节点 | undo 前后都在？ | 锚点取法 |
|---|---|---|---|
| add（加 item） | 添加按钮所在节点（list 父 / 右键参照节点） | ✅（undo 只是删掉新增项，参照节点仍在） | = 操作 position.id |
| **delete（删 item）** | ~~被删节点~~ → **父节点** | ❌ 被删节点 undo 前不存在 → 取**父** | `undoAnchorId = 父 id` |
| swap / fold / impl / paste | 操作节点本身 | ✅（拓扑翻转，节点仍在） | = 操作 position.id |
| 值类（primitive / note） | 无结构锚点 | 布局不变，不需要锚点 | 快照无 anchorId → 不动 |
| replaceEditingObject（整体换） | 无单一焦点 | 整图换 | undo 用 FitFull（重新铺满） |

**「delete 取父」**：`recordEditEntityCreator.ts` 遍历 list 时，闭包恰好持有父 id（当前 `createEntity` 的 `id`，即 list 所属 struct）。delete 按钮的 UI position 仍指向被删 item（正向 FitId 用，删后 item 不在新布局 → 自然 noop，**现有「删除后视口不动」行为不变**）；undo 锚点单独取父 id。

**连续 undo/redo**：每个快照携带「产生它的操作」的锚点 id；undo 弹出快照时取**被弹出那个**的锚点（= 被撤销操作的焦点），自然跟随每一步，不漂移。

---

## 4. 方案谱系

| 方案 | 锚点 | 不跳效果 | flow 层复杂度 | session/栈改动 | 正向影响 |
|---|---|---|---|---|---|
| **S0 现状** | — | 结构 undo 跳 | — | — | — |
| **S1 undo 全 FitFull** | — | 缩放重置 | 无 | 极小 | 无 |
| **S3 根节点** | 固定 root | 根不动，远离视线时无感 | 低 | flow 传 rootId | 无 |
| **S4 视口中心最近节点** | flow 算 | 视野中心不动 | 中（查宽高+遍历+fallback） | 无 | 无 |
| **S5 上个操作节点（采纳）** | 快照带的被撤销操作节点 | **视觉焦点不动** | 低（prevMap + id 查询） | 快照带元数据 | 无（正向 FitId 不动） |

**为何选 S5**：锚点 = 用户真实视觉焦点，比 S4「视野中心」更准；flow 层比 S4 简单（不用查容器宽高、不用遍历找最近、不用 fallback 链）；正向 FitId 零改动（风险隔离）。

---

## 5. 采纳方案 S5：上个操作节点锚点

### 5.1 总体思路

- **session**：undo/redo 时声明新意图 `KeepStable` + 锚点 id（从被撤销操作的快照取），只出意图、不碰 xyflow。
- **flow**：`useEntityToGraph` 缓存上一帧布局 `prevRectMap`；`pickViewportAction` 的 `KeepStable` 分支用 `anchorOld = prevRectMap.get(id)`、`anchorNew = id2RectMap.get(id)`、`computeStableViewport` 补偿。
- **值类 undo**：快照无锚点 → 不补偿 → 不动（布局不变，正确）。
- **整体替换 undo**：快照标记 FitFull → 全图铺满。

分层与现有一致：session 出意图（`fitView`），视口数学与锚点坐标读取全在 flow 层纯函数 `pickViewportAction`（+ 单测）。

### 5.2 枚举设计

新增一档 `KeepStable`（语义：布局变化时，以指定锚点 id 保持其屏幕坐标不变；锚点坐标未变时退化为不动）：

```ts
export enum EFitView {
    FitFull = 'FitFull',
    FitId = 'FitId',
    /** undo/redo 用：以 fitViewToIdPosition.id 为锚点，relayout 后让其屏幕坐标不变。
     *  与 FitId 区别：anchorOld 不来自 position.x/y（操作发起时），而来自 flow 层缓存的「上一帧布局」
     *  （= undo 发起时锚点坐标）。与 NoChange 区别：NoChange 无论如何不动（固定页/只读），KeepStable 布局变就补偿。 */
    KeepStable = 'KeepStable',
    NoChange = 'NoChange',
}
```

> 与 `NoChange` 的区分：`NoChange` = 「无论如何不动视口」（RecordRef 固定页/只读）；`KeepStable` = 「布局变就补偿、不变就不动」（undo/redo）。消费端真实分化，值得独立（fitview 文档 P2 合并 FitNone→NoChange 是因消费端**完全相同**；此处**不同**）。

### 5.3 快照携带元数据（UndoStore 扩展）

`UndoStore` 的快照从纯 `JSONObject` 扩展为带 undo 语义：

```ts
type Snapshot = {
    data: JSONObject;            // editingObject 深拷（原 Snapshot 内容）
    undoFitView: EFitView;       // undo/redo 到此快照后该用的 fitView
    anchorId?: string;           // KeepStable 时的锚点 id
};
```

capture 时机与取值：

| capture 时机 | undoFitView | anchorId |
|---|---|---|
| `structureChange`（add/swap/fold/impl/paste） | `KeepStable` | `position.id`（操作节点） |
| `structureChange`（**delete**） | `KeepStable` | `undoAnchorId`（父 id，creator 传入） |
| `replaceEditingObject` | `FitFull` | — |
| `flushValueCoalesce` / Form.List 值类步 | `NoChange` | — |
| `setBaseline`（初始/提交后） | `FitFull` | — |

`popUndo` / `popRedo` 返回 `{target, undoFitView, anchorId}`：`target` = 要恢复成的快照；`undoFitView`/`anchorId` = **被弹出（被撤销/被重做）那个快照**的语义（= 被撤销操作的焦点）。

> 栈语义不变（baseline / done / undone 三段、分叉、maxDepth=50），只是每项多了两个元数据字段。

### 5.4 逐层改动

#### (a) `domain/entityModel.ts` — 新增 `KeepStable`（见 §5.2）

#### (b) `domain/undoStore.ts` — Snapshot 带元数据

```ts
type Snapshot = { data: JSONObject; undoFitView: EFitView; anchorId?: string };

setBaseline(s: Snapshot): void { this.baseline = s; this.done = []; this.undone = []; }
capture(s: Snapshot): void { this.undone = []; this.done.push(s); if (this.done.length > this.maxDepth) this.done.shift(); }

/** undo：弹 done 顶（被撤销操作的快照），返回要恢复成的 target + 被撤销操作的视口语义。 */
popUndo(): { target: Snapshot; undoFitView: EFitView; anchorId?: string } {
    const s = this.done.pop()!;
    this.undone.push(s);
    const target = this.done.length > 0 ? this.done[this.done.length - 1] : this.baseline;
    return {target, undoFitView: s.undoFitView, anchorId: s.anchorId};
}

/** redo：弹 undone 顶（重做操作的快照），返回它 + 其视口语义。 */
popRedo(): { target: Snapshot; undoFitView: EFitView; anchorId?: string } {
    const s = this.undone.pop()!;
    this.done.push(s);
    return {target: s, undoFitView: s.undoFitView, anchorId: s.anchorId};
}
```

#### (c) `services/editingSession.ts`

```ts
// capture 包裹：组装带元数据的快照
private capture(data: JSONObject, undoFitView: EFitView, anchorId?: string): void {
    this.undoStore.capture({data, undoFitView, anchorId});
}

// 结构操作收尾：正向仍 FitId（position 含坐标，给 pickViewportAction 的 FitId 分支）；undo 记 KeepStable + 锚点
private structureChange(position: EntityPosition, undoAnchorId?: string): void {
    this.bumpStructure({fitView: EFitView.FitId, position});
    this.capture(this.captureUndoPoint(), EFitView.KeepStable, undoAnchorId ?? position.id);
}

// 各结构方法透传可选 undoAnchorId（仅 delete 传父，其余默认 position.id）
deleteArrayItem(deleteIndex, arrayFieldChains, position, undoAnchorId?) {
    this.beforeStructuralChange();
    ... obj.splice ...
    this.structureChange(position, undoAnchorId);
}
// addArrayItem / addArrayItemAtIndex / swapArrayItem / updateFold / updateInterfaceValue / pasteStruct
// 签名加可选 undoAnchorId? 透传给 structureChange（默认 position.id）

replaceEditingObject(newEditingObject) {
    this.beforeStructuralChange();
    deleteRefsInPlace(newEditingObject);
    this.editingObject = newEditingObject;
    this.bumpStructure({fitView: EFitView.FitFull});
    this.capture(this.captureUndoPoint(), EFitView.FitFull);
}

// 值类合并关闭：NoChange（布局不变）
private flushValueCoalesce() {
    ... 
    this.capture(this.captureUndoPoint(), EFitView.NoChange);
    ...
}
// Form.List 长度变（结构步但无 position）：NoChange（primitive list 行增删不建实体，布局变化极小）
//   → updateFormValues 里长度变分支的 capture 改 NoChange

undo() {
    this.flushValueCoalesce();
    if (!this.undoStore.canUndo()) return;
    const {target, undoFitView, anchorId} = this.undoStore.popUndo();
    this.applyUndoPoint(target.data);
    this.bumpStructure({
        fitView: undoFitView,
        position: anchorId ? {id: anchorId} : undefined,
    });
}
redo() {  // 对称：popRedo
    this.flushValueCoalesce();
    if (!this.undoStore.canRedo()) return;
    const {target, undoFitView, anchorId} = this.undoStore.popRedo();
    this.applyUndoPoint(target.data);
    this.bumpStructure({fitView: undoFitView, position: anchorId ? {id: anchorId} : undefined});
}

initUndoBaseline() { this.undoStore.setBaseline({data: this.captureUndoPoint(), undoFitView: EFitView.FitFull}); }
// resetBaselines 同理：setBaseline({data: snap, undoFitView: FitFull})
```

#### (d) `routes/record/recordEditEntityCreator.ts` — delete 锚点取父

```ts
// createEntity 遍历 list item 处（闭包持有父 id = 当前 id）
const onDeleteFunc = (position: EntityPosition) => {
    // 正向 position 仍指被删 item（删后不在新布局 → FitId 自然 noop，删除后视口不动）；
    // undo 锚点取父（被删节点 undo 前不存在，父稳定）
    this.session.deleteArrayItem(arrayIndex, chain, position, id);  // id = 父（当前 struct）
};
```

#### (e) `flow/layout/viewportMath.ts` — `pickViewportAction` 加 KeepStable 分支

```ts
export function pickViewportAction(
    editingObjectRes: EditingObjectRes | undefined,
    id2RectMap: Map<string, Rect>,            // 新布局（relayout 后）
    currentVp: Viewport,
    opts?: { prevId2RectMap?: Map<string, Rect> },   // 上一帧布局，仅 KeepStable 用
): ViewportAction {
    if (editingObjectRes === undefined || editingObjectRes.fitView === EFitView.FitFull) {
        return {kind: 'fitFull'};
    }
    if (editingObjectRes.fitView === EFitView.FitId && editingObjectRes.fitViewToIdPosition) {
        // 正向：anchorOld = position.x/y（操作发起时），anchorNew = 新布局。不变。
        const {id, x, y} = editingObjectRes.fitViewToIdPosition;
        const nowXy = id2RectMap.get(id);
        if (nowXy !== undefined) {
            return {kind: 'fitId', viewport: computeStableViewport({x, y}, {x: nowXy.x, y: nowXy.y}, currentVp)};
        }
    }
    if (editingObjectRes.fitView === EFitView.KeepStable
        && editingObjectRes.fitViewToIdPosition && opts?.prevId2RectMap) {
        // undo/redo：anchorOld = 上一帧布局该 id 坐标（undo 发起时），anchorNew = 新布局同 id 坐标
        const {id} = editingObjectRes.fitViewToIdPosition;
        const oldRect = opts.prevId2RectMap.get(id);
        const newRect = id2RectMap.get(id);
        if (oldRect && newRect) {
            return {kind: 'fitId', viewport: computeStableViewport({x: oldRect.x, y: oldRect.y}, {x: newRect.x, y: newRect.y}, currentVp)};
        }
    }
    return {kind: 'noop'};
}
```

> `KeepStable` 与 `FitId` 复用同一 `computeStableViewport` + 同一 `{kind:'fitId'}` 动作；区别仅 anchorOld 来源（prevMap vs position.x/y）。

#### (f) `flow/useEntityToGraph.ts` — 缓存 prevRectMap + 传参

```ts
const prevRectMapRef = useRef<Map<string, Rect> | undefined>(undefined);

// Effect 2：
useEffect(() => {
    if (viewportReady && id2RectMap && newNodes) {
        const action = pickViewportAction(editingObjectRes, id2RectMap, getViewport(), {
            prevId2RectMap: prevRectMapRef.current,
        });
        if (action.kind === 'fitFull') {
            void fitView({padding: 0.2, minZoom: 0.3, maxZoom: 1});
            if (setFitViewForPathname) setFitViewForPathname(pathname);
        } else if (action.kind === 'fitId') {
            void setViewport(action.viewport);
        }
        prevRectMapRef.current = id2RectMap;   // 每次布局变化后更新「上一帧」
    }
}, [editingObjectRes, id2RectMap, viewportReady, newNodes, fitView, setViewport, getViewport,
    setFitViewForPathname, pathname]);
```

> prevRectMapRef 只在 guard 内更新；guard 要求 `id2RectMap` 非空，loading 期（removeQueries 后 data=undefined）不更新 → undo 前布局稳稳留到补偿那帧。

### 5.5 关键时序

```
t0  ctrl+z → session.undo() → applyUndoPoint（editingObject 回滚）→ bumpStructure({KeepStable, anchorId})
      → structureVersion++ / removeQueries(['layout',pathname,'e']) / emit
t1  Record 重渲 → entityMap 重算（新拓扑）→ newNodes 引用变
t2  useQuery 缓存已空 → data=undefined（loading）→ ELK 异步重跑
      （id2RectMap=undefined → Effect 2 guard 不满足、不跑 → prevRectMapRef 不更新，仍 = undo 前布局；
       newNodes=undefined → Effect 1 不下发 → 画面保持 undo 前节点位置）
t3  ELK 返回新 id2RectMap → newNodes 更新 → Effect 2 主体跑：
      - getViewport() = undo 前视口（尚未 setViewport）✅
      - id2RectMap = 新布局 ✅
      - prevRectMapRef.current = undo 前布局（t0~t3 未被覆盖）✅
      - KeepStable 分支：prevRectMap.get(anchorId)=anchorOld，id2RectMap.get(anchorId)=anchorNew
        → computeStableViewport → setViewport（锚点屏幕不动）
      - prevRectMapRef.current = 新布局
```

**不变量**：`prevRectMapRef` 只在 Effect 2 主体末尾（guard 内）更新；loading 期绝不更新 → undo 前布局留到 t3 消费。✅

**isEdited 翻转边界**（现有 undo 已有、KeepStable 同样覆盖）：undo 到 baseline 时 `isEdited` 可能 true→false → queryKey 在带 `'e'`/不带 `'e'` 间切 → `id2RectMap` 在编辑/浏览两套缓存间切；每次切 Effect 2 都跑、`prevRectMapRef` 始终是上一帧布局，KeepStable 照常补偿（现有 NoChange 在此边界也跳，本方案一并修掉）。

### 5.6 边界

| 场景 | 行为 |
|---|---|
| 值类 undo（快照 NoChange） | 不补偿 → 不动（布局不变）✅ |
| 结构 undo，锚点 undo 前后都在 | 补偿 → 锚点屏幕不动 ✅ |
| delete undo | 锚点=父，父 undo 前后都在 → 补偿 ✅ |
| 整体替换 undo（快照 FitFull） | 全图铺满 ✅ |
| 锚点在新布局不存在（极端） | `prevRectMap.get`/`id2RectMap.get` 命中失败 → noop（罕见，接受小跳） |
| 首次无 prevRectMapRef | `opts.prevId2RectMap` 缺失 → noop（undo 前必有布局，防御） |

---

## 6. 备选（未采纳，留档）

- **S4 视口中心最近节点**：flow 层算「离视口中心最近且 undo 后仍存在的节点」为锚点。UX 亦佳，但需查容器宽高 + 遍历 + fallback，比 S5 复杂；锚点是「视野中心」而非「操作焦点」，略逊。
- **S3 根节点锚点**：实现最简，但根节点常远离视线，稳定感弱。
- **S1 undo 全 FitFull**：极简（改 2 行），但每次 undo 缩放/平移重置、丢用户手动视口，割裂感强。

---

## 7. 测试策略

### 7.1 `viewportMath.test.ts` 扩展

- `KeepStable` + prev/new 布局 + anchorId 命中 → `{kind:'fitId'}`，断言**锚点屏幕坐标不变 + 缩放保持**（复用 `screenOf`）。
- `KeepStable` + 锚点在新布局缺失 → `noop`。
- `KeepStable` + 缺 prevId2RectMap → `noop`（防御）。
- `KeepStable` + prev/new 逐位相同（值类等价）→ 补偿视口 == currentVp（不动）。
- 现有 `FitId`（position.x/y）/ `FitFull` / `NoChange` 分支不回归。

### 7.2 `undoStore.test.ts` 更新

- `Snapshot` 带 `{data, undoFitView, anchorId?}`。
- `popUndo`/`popRedo` 返回 `{target, undoFitView, anchorId}`；`undoFitView`/`anchorId` = 被弹出快照的元数据。
- 栈语义（baseline 栈底、分叉、maxDepth）不变。

### 7.3 `editingSession.test.ts` 更新

- 原「undo 不跳视口：fitView=NoChange」（:353）→ 改为按快照语义：
  - 结构操作后 undo → `fitView=KeepStable` + `fitViewToIdPosition.id=锚点`。
  - 值类编辑后 undo → `fitView=NoChange`。
  - `replaceEditingObject` 后 undo → `fitView=FitFull`。
  - redo 对称。
- delete 的 undo 锚点 = 父（需 creator 传 `undoAnchorId`，单测可构造 position + 父 id 直接调 `deleteArrayItem`）。

### 7.4 不在单测覆盖（实测）

- 真实 undo「删除大子树」/「折叠嵌套」的视觉稳定性（ELK 异步 + 真实视口），需 release 实测（参考 memory：dev Profiler 不可靠，金标准 PerformanceObserver；视口主观体验直接看）。

---

## 8. 决策记录

- **方案**：S5（上个操作节点锚点）。锚点 = 被撤销操作的节点 id；delete 取父；anchorOld 由 flow 层从上一帧布局读（不用快照坐标）。
- **枚举**：新增 `KeepStable`（与 `NoChange`/`FitId` 消费端真实分化）。
- **正向 FitId**：零改动（风险隔离）。
- **快照元数据**：`{undoFitView, anchorId?}`，让 undo 按被撤销操作语义选择 KeepStable/NoChange/FitFull。
- **`pickViewportAction` 签名**：加可选 `opts.prevId2RectMap`，纯函数可测。

---

## 9. 速记

- **根因**：`NoChange` 假设「布局不变」；结构 undo 触发 ELK 重排、世界坐标变，视口不动 → 跳。值类 undo 布局不变，NoChange 才正确。
- **本质**：「不跳」唯一可行 = 锚点节点屏幕稳定（`computeStableViewport`）；「保持世界中心」与 NoChange 数学等价，证伪。
- **S5**：undo/redo 用 `KeepStable`，锚点 = **被撤销操作的节点 id**（视觉焦点；delete 取父）；anchorOld 由 flow 层从上一帧布局（`prevRectMapRef`）读，anchorNew 从新布局读，`computeStableViewport` 补偿。值类快照 NoChange、整体替换快照 FitFull。
- **分层**：session 只出意图（不碰 xyflow）；视口数学与锚点坐标读取全在 flow 层 `pickViewportAction`（纯函数 + 单测）。
- **改动**：entityModel(+枚举) / undoStore(快照带元数据) / editingSession(capture 带语义、undo/redo 按快照走、structureChange 加 undoAnchorId) / recordEditEntityCreator(delete 传父) / viewportMath(KeepStable 分支) / useEntityToGraph(prevRectMapRef) + 3 测试 + 文档同步。
- **正向零改动**：FitId/position.x/y 不变；delete 正向仍 noop（被删 item 不在新布局）。

## 相关文档
- [fitview-视口适配机制](./fitview-视口适配机制.md) —— `EFitView`/`computeStableViewport`/`pickViewportAction` 全貌。
- [undo-redo-设计](./undo-redo-设计.md) —— undo/redo 快照栈与 `bumpStructure` 现状。
- [undo-redo-原理与现状](./undo-redo-原理与现状.md) —— 难题4「视图状态独立性」业界共识。

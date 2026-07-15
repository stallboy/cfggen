# 视口适配（FitView）：编辑后画布为什么不乱跳

> 这篇讲画布**视口（viewport）怎么适配**：编辑动作之后，视口该全图重铺、还是锚定某个节点不动、还是不动——这套"视口意图"怎么用一个枚举（`EFitView`）沿单向数据流从编辑会话传到视图层，以及 FitId 档位为什么必须自己算数学。
>
> **不讲**：编辑会话与就地变异（→ [`04-state-management.md`](./04-state-management.md) §六）、undo 快照栈与视口语义的由来（→ [`06-undo-redo.md`](./06-undo-redo.md)）、layout 缓存/queryKey（→ [`05-url-api-reactquery.md`](./05-url-api-reactquery.md) §5.3，本文 §八只点它们的耦合点）。
>
> **锚点**：契约载体 `EFitView` / `EditingObjectRes` 在 `domain/entityModel.ts`；决策与数学 `pickViewportAction`（内部 `computeStableViewport`）在 `flow/layout/viewportMath.ts`；消费在 `flow/useEntityToGraph.ts`；产生在 `services/editingSession.ts` 的 `bumpStructure`。

---

## 一、一句话理解

**FitView 是「编辑动作」与「视口动作」之间的契约**：编辑会话（`EditingSession`）每次结构变更时，在 `EditingObjectRes` 上挂一个 `EFitView` 枚举值，声明"这次变更后视口应该怎么动"；视图层（`useEntityToGraph`）读到这个值，翻译成对应的 xyflow 命令（`fitView` / `setViewport` / 什么都不做）。

它解决的核心问题是：**React 数据流是单向的，但"视口该不该跳"是带时序语义的意图**（新增节点要定位过去、undo 不要跳、全量替换要重新铺满），不能从数据 diff 反推。所以用一个显式枚举把"意图"沿着数据通道传过去。

---

## 二、背景知识：xyflow 的视口是什么

xyflow（React Flow）的视口（viewport）是一个**线性变换参数**（公开契约，见 `flow/layout/viewportMath.ts`）：

```
screenX = worldX * zoom + vp.x
screenY = worldY * zoom + vp.y
```

- `world`：节点的"世界坐标"，由布局算法（ELK）算出，写在 `node.position`。
- `screen`：节点在屏幕上的像素位置。
- `vp.{x,y}`：平移量；`zoom`：缩放。

xyflow 提供的视口相关公开 API（`useReactFlow()` 返回）：

| API | 语义 |
|---|---|
| `fitView({padding, minZoom, maxZoom, duration})` | 把所有节点框进视口（高层原语） |
| `setCenter(x, y, {zoom, duration})` | 把某个世界坐标居中 |
| `setViewport(vp)` | 直接设置平移+缩放（最底层） |
| `getViewport()` | 读当前视口 |

> **关键认知**：xyflow **没有**"保持某点屏幕坐标不变"的高层原语。这正是 FitView 需要分档、且有一档必须自己算数学的根本原因（见 §五）。

---

## 三、核心概念

### 3.1 `EFitView` 枚举

定义在 `domain/entityModel.ts`：

```ts
export enum EFitView {
    FitFull = 'FitFull',    // 全图适配：把所有节点框进视口
    FitId = 'FitId',        // 锚点不动（正向结构操作）：relayout 后让指定节点屏幕坐标不变，anchorOld=position.x/y
    /** undo/redo 结构操作：relayout 后让锚点屏幕坐标不变，但 anchorOld 取自「上一帧布局」（非 position.x/y，后者是操作发起时坐标会过时）。 */
    KeepStable = 'KeepStable',
    /** 不跳视口：只读/固定页保持当前视口；值类 undo（布局不变）亦用此（不调 fitView/setViewport）。 */
    NoChange = 'NoChange',  // 字符串枚举：DevTools/调试可读，不依赖隐式数字
}
```

| 档位 | 触发场景 | 视口效果 | xyflow 调用 |
|---|---|---|---|
| `FitFull` | 初次加载、整体替换、reset、浏览态、整体替换的 undo | 全图重新铺满 | `fitView()` |
| `FitId` | 增删/swap/fold/换 impl/粘贴等**正向结构**操作 | 让"锚点节点"屏幕位置不动 | `setViewport()`（anchorOld=position.x/y） |
| `KeepStable` | **undo/redo 结构操作** | 让"被撤销操作的焦点节点"屏幕位置不动 | `setViewport()`（anchorOld=上一帧布局坐标；锚点=被撤销操作节点，delete 取父） |
| `NoChange` | undo/redo 值类、只读/固定页（首次适配后） | 不跳 | 无 |

### 3.2 `EditingObjectRes` 契约载体

```ts
export type EditingObjectRes = {
    fitView: EFitView;
    fitViewToIdPosition?: EntityPosition;  // 仅 FitId 时携带：锚点节点的 id + 旧世界坐标
    isEdited: boolean;
}
```

它被放在 `domain/` 中立层，目的是消除 `flow → features` 的反向依赖（视图层 `flow/` 只 import `domain/` 契约，不反向依赖 `features/`）。`fitView` 名字虽带视图语义，但作为"编辑态↔视图"的共享契约放 domain 合理。

---

## 四、端到端数据流

```
┌─────────────────────────────────────────────────────────────────────┐
│  产生端：EditingSession（services/editingSession.ts）                │
│                                                                       │
│  每个编辑方法 → bumpStructure({fitView, position?})                   │
│    └─ 写 this.fitView / this.fitViewToIdPosition                      │
│    └─ structureVersion++        ← useSyncExternalStore 的快照         │
│    └─ emit()                    ← 通知 Record 重渲                    │
│                                                                       │
│  getEditingObjectRes() 读取当前 fitView → 组装 EditingObjectRes        │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ （Record.tsx useMemo 里调用）
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│  传递：Record.tsx                                                    │
│                                                                       │
│  const editingObjectRes = session.getEditingObjectRes();             │
│  useEntityToGraph({ ..., editingObjectRes });                        │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────────────┐
│  消费端：useEntityToGraph（flow/useEntityToGraph.ts）                  │
│                                                                       │
│  useEffect(() => {                                                   │
│      action = pickViewportAction(editingObjectRes, id2RectMap, vp)   │
│        fitFull: fitView({...duration})                               │
│        fitId:   setViewport(action.viewport, {duration})             │
│        noop:    不动                                                 │
│  }, [editingObjectRes, id2RectMap, ...])                             │
└─────────────────────────────────────────────────────────────────────┘
```

**三个关键不变量**：

1. **`fitView` 只在事件回调/effect 里被写**（`bumpStructure` 由编辑方法触发），绝不在 render 期写——符合 EditingSession 的设计第 1 条。
2. **`fitView` 不独立订阅**：它搭 `structureVersion` 的便车。值类编辑不 bump（性能契约 1），所以 `editingObjectRes` 也不重建。
3. **消费端幂等**：同一个 `editingObjectRes` + 同一个 `id2RectMap`，effect 跑多少次结果都一样（视口被设到同一个地方）。

---

## 五、FitId 的数学：为什么必须自己算

### 5.1 问题陈述

结构操作（比如往数组里加一项）会触发 ELK 重新布局，**所有节点的世界坐标都可能变**。但我们希望：**用户盯着那个被改的节点，它在屏幕上的位置不动**（"图没跳"的感觉）。

即：锚点节点的世界坐标从 `anchorOld` 变成 `anchorNew`，要求：

```
anchorNew*zoom + newVp.{x,y} === anchorOld*zoom + vp.{x,y}   // 锚点屏幕坐标不变（屏幕 = 世界*zoom + 平移）
newVp.zoom === vp.zoom                                    // 缩放不变
```

### 5.2 推导

代入线性变换 `screen = world * zoom + t`：

```
anchorNew.x * zoom + newVp.x = anchorOld.x * zoom + vp.x
→ newVp.x = anchorOld.x * zoom - anchorNew.x * zoom + vp.x
          = (anchorOld.x - anchorNew.x) * zoom + vp.x
```

y 同理。`flow/layout/viewportMath.ts` 的内部函数 `computeStableViewport` 即此实现（由 `pickViewportAction` 调用，不单独导出）：

```ts
function computeStableViewport(anchorOld: Point, anchorNew: Point, vp: Viewport): Viewport {
    return {
        zoom: vp.zoom,
        x: anchorOld.x * vp.zoom - anchorNew.x * vp.zoom + vp.x,
        y: anchorOld.y * vp.zoom - anchorNew.y * vp.zoom + vp.y,
    };
}
```

### 5.3 为什么不用 `fitView` / `setCenter`

- `fitView` 框的是"所有节点的包围盒"，会改变缩放和平移，做不到"锚点屏幕不动"。
- `setCenter` 把指定点居中，但用户要的不是"居中"而是"原地不动"。

xyflow 没提供这个原语，所以**必须自算**。不要试图用 `fitView` 替换 FitId 分支——数学不等价。

### 5.4 消费端代码（`useEntityToGraph.ts`）

```ts
// 视口动作决策走纯函数 pickViewportAction（flow/layout/viewportMath.ts）：三分支选择 + FitId 数学
// 全部收口其中，effect 只负责调命令。行为与原 inline 三分支逐分支等价。
const action = pickViewportAction(editingObjectRes, id2RectMap, getViewport());
if (action.kind === 'fitFull') {
    void fitView({padding: 0.2, minZoom: 0.3, maxZoom: 1});
} else if (action.kind === 'fitId') {
    // action.viewport 已由 pickViewportAction 经 computeStableViewport 算好
    // （anchorOld=操作发起时的旧坐标，anchorNew=新布局坐标，保持锚点屏幕不动）
    void setViewport(action.viewport);
}
// noop（NoChange / FitId 但 id 不存在）：不动视口
```

> **教学点**：`fitViewToIdPosition` 里的 `x/y` 是**操作发起那一刻**的旧世界坐标，由编辑方法调用方传入（如 `Record.tsx` 里 `{id: entity.id, x: entityNode.position.x, y: entityNode.position.y}`）。它不是"目标位置"，而是"锚点曾经的坐标"。

---

## 六、各档的触发点一览（EditingSession）

`services/editingSession.ts` 里 `bumpStructure` 是唯一的 fitView 写入点，所有方法按语义传参：

| 方法 | fitView | position | 说明 |
|---|---|---|---|
| 构造函数 | `FitFull` | — | 首次加载全图铺满 |
| `maybeReset` | `FitFull` | — | 切记录 / 后台推新数据 |
| `replaceEditingObject` | `FitFull` | — | Chat/AddJson 写入、funcClear |
| `addArrayItem` / `addArrayItemAtIndex` / `deleteArrayItem` / `swapArrayItem` | `FitId` | ✓ | 数组结构变更，定位到操作节点 |
| `updateFold` | `FitId` | ✓ | 折叠/展开，定位到该节点 |
| `updateInterfaceValue` | `FitId` | ✓ | 换 interface 实现 |
| `pasteStruct` | `FitId` | ✓ | 粘贴 |
| `undo` / `redo` | 按被撤销操作快照语义：结构→`KeepStable`+锚点；值类→`NoChange`；整体替换→`FitFull` | 锚点（结构） | 数据回滚；结构 undo 让焦点节点屏幕不动（详见 [`06-undo-redo.md`](./06-undo-redo.md) §四） |

**记忆口诀**：

- **整体换了 → FitFull**（重新认识这张图）
- **局部动了 → FitId**（盯着被改的地方）
- **只是回退 → NoChange**（假装没发生）

---

## 七、只读路径与固定页面（RecordRef）

`features/record/RecordRef.tsx` 不走 `EditingSession`，自己构造 `editingObjectRes`：

```ts
const keepViewport: EditingObjectRes = { fitView: EFitView.NoChange, isEdited: false }

// fittedPathname 记录"已适配过"的 pathname（由 useEntityToGraph 的 FitFull 回调写入）
let editingObjectRes;
if (inDragPanelAndFix && fittedPathname === pathname) {
    editingObjectRes = keepViewport;     // 此 pathname 已适配过 → 不再跳
}
// 否则 editingObjectRes === undefined → useEntityToGraph 走 FitFull 分支
```

**产品意图**（fixed page）：用户在 `cfgeditor.yml` 配置的"固定引用/未引用页面"，作为 Splitter 的常驻面板。希望它**首次全图适配，之后保持用户手动调整的视口**，不要因为 react-query refetch（staleTime 10s）反复重置。

`useEntityToGraph` 的 FitFull 分支会回调 `setFitViewForPathname(pathname)`，记录"已适配"。

`Record.tsx` 浏览态（非编辑）则直接 `{fitView: FitFull, isEdited: false}` 写死，每次都全图适配。

---

## 八、与性能契约、缓存的关系

FitView 不是孤立的视口开关，它和 layout 缓存通道深度耦合（见 `useEntityToGraph.ts` 注释）：

```ts
const queryKey = editingObjectRes?.isEdited
    ? ['layout', pathname, 'e', layoutKeys, topologyKeys]   // 编辑态：带 'e' 隔离
    : ['layout', pathname, layoutKeys, topologyKeys];        // 浏览态
const staleTime = editingObjectRes?.isEdited ? 0 : 1000 * 60 * 5;
```

- **结构类编辑**（FitId/NoChange）：`bumpStructure` → `onStructureChange` 同步 `removeQueries(['layout', pathname, 'e'])` → 编辑态缓存失效重布局 → 新 `id2RectMap` → effect 跑 FitId 视口矫正。
- **值类编辑**（primitive 键入）：**不 bump structureVersion**，`editingObjectRes` 不重建，layout 走 5min 干净缓存，ELK 不重跑（性能契约 1：几十个表单输入零重渲）。

**已知 quirk（有意保留，勿当 bug 修）**：纯值类编辑期间 `isEdited` 不刷新 → `editingObjectRes` 不重建 → fitView 通道也不更新。安全前提是"值类不改拓扑、布局不变"，所以不需要重新适配视口。

---

## 九、实现备忘（当前状态要点）

- **视口切换一律瞬时**：`fitView`/`setViewport` 不带 `duration`（曾加 300ms 过渡，动画引起视觉不适已撤销）。这是有意的产品决定，勿回改。
- **Effect 2 与 Effect 1 拆分**：`useEntityToGraph` 把「节点/菜单下发」（Effect 1，deps 含 `paneMenu`/`nodeMenuFunc`）与「视口动作」（Effect 2，deps 只 `editingObjectRes`/`id2RectMap`/`viewportReady`/`newNodes`）拆成两个 effect。拆分的历史诱因（值类 coalescing flush 让 `canUndo` 翻转 → 连带重置视口）已随「Record 不订阅 `canUndo` + `paneMenu` 的 `disabled` 惰性化」消除，但拆分作为视口语义的独立边界保留（详见 [`06-undo-redo.md`](./06-undo-redo.md) §3.8）。
- **决策/数学收口纯函数**：`pickViewportAction`（`flow/layout/viewportMath.ts`）是公开纯函数，三分支选择 + FitId/KeepStable 数学全收口其中，effect 只调命令；`computeStableViewport` 为内部函数。配 `viewportMath.test.ts` 单测全分支。
- **`EFitView` 字符串枚举**：`= 'FitFull'` 等字符串值（DevTools/调试可读，不依赖隐式数字）；`FitNone` 已并入 `NoChange`（消费端两者完全相同，YAGNI），`RecordRef` 用 `keepViewport` 常量。

---

## 十、常见排查清单

| 现象 | 排查方向 |
|---|---|
| 编辑后视口跳到错误位置 | 检查 `fitViewToIdPosition` 的 `x/y` 是不是**操作发起时**的旧坐标（不是目标坐标） |
| FitId 时画布猛地一跳 | 检查 `id2RectMap.get(id)` 是否拿到了**新布局**结果（layout 缓存是否被正确 remove） |
| undo 结构后视口大跳 | 结构 undo 应传 `KeepStable`+锚点（补偿让焦点节点屏幕不动）；检查快照 `anchorId` 是否正确、`prevRectMap` 是否拿到 undo 前布局（loading 期 guard 不应更新 ref） |
| 固定页面视口反复重置 | P0 已修；若复现，检查 `fittedPathname === pathname` 是否成立、`setFitViewForPathname` 回调是否传入 |
| 键入 input 后视口偶发被 fitFull | 视口 effect（Effect 2）不得含 `paneMenu`/`nodeMenuFunc` 等菜单回调 deps——任何让这些回调换引用的改动都会连带重跑视口；已拆 effect 隔离（见 §九） |
| 浏览态每次 refetch 都重铺 | `editingObjectRes` 在非编辑态每次 memo 新建对象引用，可能触发 effect 重跑；检查 memo deps |
| 新增结构后节点没定位过去 | `viewportReady`（`panZoom !== null`）是否就绪；`id2RectMap` 是否含该 id |

---

## 十一、速查表

```
编辑动作                    fitView       视口 API                    数学
─────────────────────────────────────────────────────────────────────────
初次加载 / 整体替换 / reset  FitFull       fitView()                  —
增删/swap/fold/impl/paste   FitId         setViewport(compute…)      anchor 屏幕不变（anchorOld=position）
undo/redo 结构              KeepStable    setViewport(compute…)      焦点 anchor 屏幕不变（anchorOld=上一帧布局）
undo/redo 值类 / 固定页 / 只读 NoChange   (无)                       —
```

---

## 一句话速记

- **FitView 是"编辑动作→视口动作"的契约**：`EFitView` 四档（FitFull/FitId/KeepStable/NoChange）挂在 `EditingObjectRes` 上，沿单向数据流从 EditingSession 传到 `useEntityToGraph`。
- **记忆口诀**：整体换→FitFull、局部动→FitId、只是回退→NoChange。
- **FitId 必须自算数学**（`computeStableViewport`）：xyflow 没有"保持锚点屏幕坐标不变"的原语，靠 `newVp = (anchorOld-anchorNew)*zoom + vp` 补偿。
- **决策全收口纯函数 `pickViewportAction`**（`flow/layout/viewportMath.ts`，配单测），effect 只调命令、幂等。
- **fitView 搭 structureVersion 便车**：值类编辑不 bump → 视口通道不更新（有意，值类不改拓扑）。

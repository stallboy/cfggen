# cfgeditor 图形层（src/flow）重构与 xyflow v12 教学指南

> **双线文档**：左手是 xyflow v12 + ELK 的入门教学（每个概念都落在本项目的真实代码上），
> 右手是 `src/flow` 的重构路线图（每条动作标注 ROI、风险与依赖顺序）。
>
> **读者画像**：熟悉 React、不熟 xyflow。读完既能把这些图形模式复用到下一个项目，
> 也能拿出一份可执行的重构 backlog。
>
> **覆盖版本**：`@xyflow/react ^12.11.2` + `elkjs`（Web Worker 模式）。
> 文中 `file:line` 基于撰写时的代码，重构后可能漂移，以函数/符号名为主索引。

---

## 目录

- [0. 怎么读这篇文档](#0-怎么读这篇文档)
- [第一部分 · xyflow v12 核心概念（教学主线）](#第一部分--xyflow-v12-核心概念教学主线)
- [第二部分 · 本项目架构现状巡礼](#第二部分--本项目架构现状巡礼)
- [第三部分 · 重构机会](#第三部分--重构机会)
  - [3.0 先读我：这些"看起来像问题"的，请勿动](#30-先读我这些看起来像问题的请勿动)
  - [3.1 P3+ — 架构级 big rocks](#31-p3--架构级-big-rocks)
- [第四部分 · 路线图与依赖](#第四部分--路线图与依赖)
- [附录 A · xyflow v12 概念速查](#附录-a--xyflow-v12-概念速查)
- [附录 B · 参考来源](#附录-b--参考来源)

---

## 0. 怎么读这篇文档

### 两条阅读路径

- **只想学 xyflow**：读 [第一部分](#第一部分--xyflow-v12-核心概念教学主线)（10 个核心概念，每个对照本项目代码）。
- **只想改本项目**：跳到 [第三部分](#第三部分--重构机会)，先看 [3.0 勿动清单](#30-先读我这些看起来像问题的请勿动) 避免误伤，再看 [3.1 P3+ big rocks](#31-p3--架构级-big-rocks)。

### xyflow 心智模型（三句话）

1. xyflow 是一张**带相机的节点-边画布**：React 层负责画节点（自定义组件），内部 zustand store 负责存位置/视口/尺寸，布局算法（ELK）是可选的外挂。
2. 一切 hook（`useReactFlow` / `useStore` / 节点内的隐式 Context）都绑定**最近祖先 `ReactFlowProvider`**，没有 scope 参数——作用域纯靠 React Context 嵌套。
3. 节点是普通 React 组件，但它的 props（`NodeProps`）由 xyflow 内部 store 驱动、引用常变——这是后续所有 `memo` 议题的根因。

### 本项目数据流全景图

```
Entity (domain/entityModel.ts)
  │   判别联合：ReadOnlyEntity | EditableEntity | CardEntity
  ▼
convertNodeAndEdges (flow/entityToNodeAndEdge.ts)
  │   产出 EntityNode[] / EntityEdge[]；顺手 fillHandles 标注连接点
  ▼
layoutAsync (flow/layoutAsync.ts)
  │   ELK Web Worker 算位置；结果由 React Query 按 ['layout', pathname, mode, nodeShow] 缓存
  ▼
useEntityToGraph (flow/useEntityToGraph.ts)
  │   通过 useReactFlow()（setNodes/setEdges/fitView/setViewport）+ 一个 useStore 只读切片
  │   （viewport 就绪信号）拿到 xyflow 命令面，在 useEffect 里命令式推 nodes/edges/viewport
  ▼
FlowGraph (flow/FlowGraph.tsx)
  │   ReactFlowProvider 包装器；defaultNodes/defaultEdges 初始化为空 []；
  │   管右键菜单；通过 FlowGraphContext 让 children 反向注册菜单/双击回调
  ▼
FlowNode (flow/FlowNode.tsx)
      nodeTypes.node 注册的唯一自定义节点；按 entity.type 分派渲染
      EntityProperties(readonly) / EntityForm(editable) / EntityCard(card)
```

**关键特征**：route 组件（Table/Record/…）`return null`——它们是"副作用型图形控制器"，真正的画布是它们在 `CfgEditorApp.tsx` 里的父级 `<FlowGraph>`。

### 术语速查表

| 术语 | 含义 | 本项目位置 |
|---|---|---|
| **受控模式** | `<ReactFlow nodes={..} onNodesChange={..}/>`，真相源在外部 React state | 本项目**未采用** |
| **非受控模式** | `defaultNodes` 初始化，所有权归 xyflow 内部 store | `FlowGraph.tsx:79-80` |
| **命令式 patch** | 通过 `setNodes`/`setEdges` 直接写内部 store | `useEntityToGraph.ts:128-129` |
| **声明尺寸** | `node.width/height`，你告诉 xyflow/ELK 节点多大 | `calcWidthHeight.ts` 估算后写入 |
| **实测尺寸** | `node.measured.{width,height}`，xyflow 挂载 DOM 后 ResizeObserver 实测 | 生产路径**从不读取**（仅 dev-only height drift 护栏对账）|
| **FitFull / FitId / FitNone** | 视口策略：全图适配 / 锚点不动 / 不动 | `entityModel.ts` `EFitView` |

---

## 第一部分 · xyflow v12 核心概念（教学主线）

### 1. ReactFlowProvider：图实例的边界

`ReactFlowProvider` 建立一个**独立的图实例**——自带一个内部 zustand store，持有这个图的 nodes/edges/viewport/dimensions/panZoom。`useReactFlow()` / `useStore()` 都只访问**最近祖先 Provider** 的图，没有 scope 参数。

**本项目用法**：`FlowGraph.tsx:76` 每个 `<FlowGraph>` 自带一个 `<ReactFlowProvider>`。而 `CfgEditorApp.tsx` 在 Splitter 主区与（可选的）拖拽面板**各挂一个** `<FlowGraph>`——所以同一页面最多并存两个互不可见的图实例（`CfgEditorApp.tsx:167` 主区、`:103/:128/:141` 侧面板）。

```tsx
// CfgEditorApp.tsx —— 两个独立画布并存
<Splitter>
  <Splitter.Panel>{/* 侧面板 */} <FlowGraph><RecordRef .../></FlowGraph></Splitter.Panel>
  <Splitter.Panel>{/* 主区   */} <FlowGraph><Outlet .../></FlowGraph></Splitter.Panel>
</Splitter>
```

> **教学点**：多 Provider 并存是 v12 推荐做法（见 [Discussion #2068](https://github.com/xyflow/xyflow/discussions/2068)），不是反模式。`useEntityToGraph` 之所以能正确操作"自己那个画布"，纯粹是因为 route 组件在对应 `<FlowGraph>` 的 JSX 子树里——靠 React Context 嵌套隐式寻址。

---

### 2. nodeTypes + 自定义节点 + NodeProps

```ts
// FlowGraph.tsx
const nodeTypes: NodeTypes = { node: FlowNode };        // 必须定义在组件外（模块级）
export type EntityNode = Node<{ entity: Entity }, "node">;

<ReactFlow defaultNodes={...} defaultEdges={...} nodeTypes={nodeTypes} ... />
```

```tsx
// FlowNode.tsx:43
export const FlowNode = memo(function FlowNode(nodeProps: NodeProps<EntityNode>) {
  const entity = nodeProps.data.entity;
  // 按 entity.type 分派：EntityProperties / EntityForm / EntityCard
});
```

**三条 v12 官方最佳实践，本项目全部合规**：

1. `nodeTypes` 定义在组件外（`FlowGraph.tsx:14-16`）——避免每次 render 新建对象导致 xyflow 认为节点类型变了、全量卸载重挂。
2. 自定义节点用 `React.memo` 包裹（`FlowNode.tsx:43`）。
3. 用 `NodeProps<TData>` 泛型强类型 `data`（`EntityNode`）。

> **教学点**：xyflow 在内部 store 每次变更后会**重组传给节点组件的 props**，`NodeProps` 引用常变。即使 `nodeTypes`/`memo` 都合规，若 `useMemo` 的 deps 写错（漏掉实际读取的可变字段），节点仍会 stale——本项目 FlowNode 的 color memo 曾因 deps 只写 `[entity]`（漏 `nodeShow`）而 stale，现已加 `nodeShow` 修复。

---

### 3. Handle / Position / nodrag

```tsx
// FlowNode.tsx:238-239
{handleIn  && <Handle type='target' position={Position.Left}  id='@in'  style={handleStyle} />}
{handleOut && <Handle type='source' position={Position.Right} id='@out' style={handleStyle} />}
```

- `<Handle type='source'|'target' position id>` 是节点上的**连接桩**；同一节点可有多个 Handle，用 `id` 区分（本项目用 `@in`/`@out`，或字段名 `item.name`）。
- `className='nodrag'` 让按钮/表单区域**不触发节点拖拽**——`EntityForm.tsx` 里所有可交互控件都带这个 class。

> **教学点**：`fillHandles`（`entityToNodeAndEdge.ts:26`）在 entityMap 构建后遍历 `sourceEdges`，把 `handleIn`/`handleOut` 标记盖到 entity/field 上，渲染时 FlowNode 据此决定是否画 `<Handle>`。这是"数据驱动连接桩"的干净做法。

---

### 4. 受控 vs 非受控（本项目是混用）

xyflow 支持两种节点管理范式：

| 模式 | 写法 | 真相源 |
|---|---|---|
| **受控** | `<ReactFlow nodes={nodes} edges={edges} onNodesChange={..} onEdgesChange={..}/>` | 外部 React state |
| **非受控** | `defaultNodes={..}`（初值）+ `useReactFlow().setNodes(..)` 命令式 patch | xyflow 内部 store |

**本项目是混用**：

```tsx
// FlowGraph.tsx:79-80 —— 非受控初始化（空数组）
<ReactFlow defaultNodes={defaultNodes} defaultEdges={defaultEdges} ...>

// useEntityToGraph.ts:128-129 —— 命令式 patch
setNodes(newNodes);
setEdges(edges);
```

> **关键澄清（勿误判为 bug）**：这种混用**不是错误**，是 xyflow 完全支持的"非受控初值 + 命令式注入"模式。它的代价是**数据流不直观**——route `return null`，节点哪来的要看 effect；但好处是可在 effect 里"等 ELK 布局算完一次性原子注入"，避免中间态闪烁。详见 [3.0 勿动清单](#a6-useentitytograph-的超长-effect--受控非受控混用--手算-viewport)。

---

### 5. useReactFlow（公开稳定）vs useStore（内部 escape-hatch）

> 本项目 `useEntityToGraph` 的命令面已收口到 `useReactFlow`（`setNodes`/`setEdges`/`setViewport`/`getViewport`/`fitView`），仅保留一个 `useStore` 只读切片作 viewport 就绪信号，不再调用内部 `panZoom` 方法。先把两类 API 的概念讲清：

| API | 性质 | 返回 | 稳定性 |
|---|---|---|---|
| `useReactFlow()` | **公开** instance API | `ReactFlowInstance`：`setNodes`/`setEdges`/`setViewport`/`getViewport`/`fitView`/`fitBounds`/`setCenter`/`getNodesBounds` | 跨版本稳定，带 `Promise`/`duration`/`ease` |
| `useStore(selector)` | **内部** zustand escape-hatch | store 任意切片 | 文档自标 escape-hatch；`panZoom` 尤其是**实现细节**，非公开契约 |

```ts
// useEntityToGraph.ts —— 命令面收口后：命令走公开 useReactFlow，仅留一个 useStore 只读切片
const viewportReady = useStore((state) => state.panZoom !== null);  // 就绪信号（本版本无 viewportInitialized 字段，panZoom 非空即就绪）
const { setNodes, setEdges, setViewport, getViewport, fitView } = useReactFlow();  // 公开
```

> **教学点**：业务代码应优先 `useReactFlow`，把 `useStore` 留给 xyflow 自身或"只读尺寸/就绪信号"等必要场景。`setNodes`/`setEdges`/`setViewport`/`fitView` 走公开 instance 最稳；`panZoom` 是实现细节，升级时可能被改，故仅作 null 判断、不调用其方法。

---

### 6. Viewport 数学：screen = world·zoom + translation

`viewport = { x, y, zoom }` 是一个**线性变换**参数（公开契约，`screenToFlowPosition`/`flowToScreenPosition` 基于此）：

```
屏幕坐标 = 世界坐标 × zoom + 平移量
screenX = worldX * zoom + x
screenY = worldY * zoom + y
```

本项目在 `flow/viewportMath.ts` 的 `computeStableViewport` 里实现了这个公式，用于"relayout 后保持锚点屏幕坐标不动"（FitId 分支）：

```ts
// flow/viewportMath.ts —— FitId：让锚点 relayout 前后屏幕位置不变
// 要求 screenOf(anchorOld, vp) === screenOf(anchorNew, newVp)，且 zoom 不变，解得：
export function computeStableViewport(anchorOld, anchorNew, vp: Viewport): Viewport {
    return {
        zoom: vp.zoom,
        x: anchorOld.x * vp.zoom - anchorNew.x * vp.zoom + vp.x,
        y: anchorOld.y * vp.zoom - anchorNew.y * vp.zoom + vp.y,
    };
}
```

> **教学点**：这个方程是**代数正确**的，且 xyflow 没有提供"保持某点屏幕坐标不变"的高层原语——所以 FitId 分支**必须自算**，不能用 `fitView` 替代（这是 [3.0 勿动清单](#a6-useentitytograph-的超长-effect--受控非受控混用--手算-viewport) 的重要一条）。`useEntityToGraph` 的 FitId 分支调用此函数 + `useReactFlow().setViewport`；不变量由 `viewportMath.test.ts` 锁定。

---

### 7. 视口 API 三档抽象

| 档位 | API | 适用 |
|---|---|---|
| **底层手算** | `getNodesBounds(nodes)` + `getViewportForBounds(bounds, w, h, minZoom, maxZoom, padding)` + `setViewport` | 需要 padding/minZoom/maxZoom 精确控制 |
| **高层适配** | `fitView({ padding, minZoom, maxZoom, nodes, duration })` | "把所有节点框进视口" |
| **居中一点** | `setCenter(x, y, { zoom, duration })` | "把某世界坐标居中" |

**本项目 FitFull 走高层 fitView，FitId 走自算**（`useEntityToGraph.ts`）：

```ts
// FitFull 分支：直接用公开 fitView（等价于原 getNodesBounds+getViewportForBounds+setViewport 三步）
void fitView({ padding: 0.2, minZoom: 0.3, maxZoom: 1 });
// FitId 分支：见 §6 的 computeStableViewport + useReactFlow().setViewport（fitView 做不到锚点不动）
```

> **教学点**：FitFull 这种"把所有节点框进视口"的需求用 `fitView` 最直接；FitId 的"保持某点屏幕坐标不变"xyflow 无高层原语，必须自算（见 §6）。

---

### 8. 节点尺寸双轨：声明（width/height）vs 实测（measured）

v12 把节点尺寸拆成**两条独立轨道**：

| 轨道 | 字段 | 来源 | 用途 |
|---|---|---|---|
| **声明/期望** | `node.width` / `node.height` | 你主动设置 | 喂给 ELK 当边界框；首次渲染 inline style |
| **实测** | `node.measured.width` / `node.measured.height` | xyflow 内部 ResizeObserver，节点 DOM 挂载后回写 | 真实渲染尺寸 |

理想流程是**"声明估算 → ELK 算位 → 挂载 → 用 measured 校正 → 偏差大则重排"**。

**本项目只走第一轨**，从不读 measured（`calcWidthHeight.ts:6-7` 作者注释明说放弃了 measured 重排，原因是闪烁）：

```ts
// calcWidthHeight.ts:6-7 —— 作者书面弃案
// 在一次又一次尝试了等待 node 准备好，直接用 node 的 computed 的 width/height 后，
// 增加这一个异步，太容易有闪烁和被代码绕晕了。放弃放弃，还是预先估算好。
```

```ts
// layoutAsync.ts:14-18 —— 把估算尺寸直接当 ELK 边界框
function nodeToLayoutChild(node, id2RectMap): ElkNode {
  const [width, height] = calcWidthHeight(node.data.entity);   // 纯魔数估算
  id2RectMap.set(node.id, { x: 0, y: 0, width, height });
  return { id: node.id, width, height };                       // ELK 当作不可压缩边界框
}
```

> **关键教学点**：ELK（elkjs）**不会去测 DOM**，它把 `ElkNode.{width,height}` 当不可压缩边界框排布。所以你喂给 ELK 的尺寸**必须与真实渲染一致**，否则节点重叠或留异常间隙。这正是 `calcWidthHeight` 的带名常量（`FIELD_ROW_H=41`/`CARD_DS_H=38`/`DESC_ROW_H=22`/`IMAGE_H=200`…）精度的意义，也是 measured vs 声明权衡的根源。魔数已常量化、并有 dev-only height drift 护栏兜底；仅当护栏显示系统性漂移才考虑 measured 重排（见 [3.1 Big Rock 6](#big-rock-6calcwidthheight-measured-重排仅护栏持续报警才上)）。
>
> **已知坑**（v12）：custom node 把 `width/height` 设为 0 会让 `measured` 永不赋值、`useNodesInitialized()` 永远 false（[issue #5215](https://github.com/xyflow/xyflow/issues/5215)）。

---

### 9. ELK 布局管线：Web Worker + React Query 缓存 + 竞态

```ts
// layoutAsync.ts —— 模块级单例 ELK，workerUrl 模式跑在 Web Worker
const elk = new ELK({ workerUrl: elkWorkerUrl });

// layoutAsync.ts —— 异步布局：失败一律 throw LayoutError（绝不 resolve undefined），透传 AbortSignal
if (signal?.aborted) throw new LayoutError('aborted', '...');
const { children } = await elk.layout(graph);
if (signal?.aborted) throw new LayoutError('aborted', '...');
if (!children) throw new LayoutError('no_children', '...');
toPositionMap(id2RectMap, children);
if (!allPositionXYOk(nodes, id2RectMap)) throw new LayoutError('dropped_nodes', '...');
return id2RectMap;
```

```ts
// useEntityToGraph.ts —— React Query 缓存布局结果；queryKey 只含布局相关字段，透传 ctx.signal
const { data: id2RectMap, error: layoutError } = useQuery({
  queryKey: ['layout', pathname, isEdited ? 'e' : '', pickLayoutKeys(nodeShowSetting)],
  queryFn: (ctx) => layoutAsync(nodes, edges, strategy, nodeShowSetting, ctx.signal),
  staleTime: isEdited ? 0 : 1000 * 60 * 5,   // 编辑态每次重取，浏览态 5min
});
```

**三个教学子概念**：

1. **内容寻址缓存**：queryKey 是 queryFn 输入的"指纹"——凡影响 ELK 输出的变量都必须进 key。本项目 queryKey 收窄到 `pickLayoutKeys(nodeShowSetting)`（算法/间距/节点尺寸/拓扑过滤字段），**纯颜色字段不进 key**——故改主题色命中缓存、不重跑 ELK。`setNodeShow` 也仅在布局相关字段变化时才 `clearLayoutCache`。仍漏进 key 的是**拓扑相关 store setting**（`maxImpl`/`refDepth`/…），靠各自 setter 手动 `clearLayoutCache` 同步——这是"声明式依赖退化成命令式失效"，见 [3.1 Big Rock 4](#big-rock-4storets-querykey-纳入拓扑-settingp4)。

2. **`removeQueries` vs `invalidateQueries`**（本项目用得很精妙）：
   - `invalidate`：对 active query 立即 refetch，但**重渲前的旧闭包会算出旧布局**。
   - `removeQueries`：物理删 entry，等重渲用**新闭包**重取。
   - 本项目 `Record.tsx:87` `onStructureChange` 用 `removeQueries(['layout', pathname, 'e'])` 只清编辑态缓存——这是对的。任何 queryKey 重构都必须保留这个 prefix 语义（`'e'` 标记保持在 `pathname` 之后同一层级）。

3. **竞态与失败的正确处置**：react-query v5 给每个 queryFn 注入 `AbortSignal`，query 变 stale/inactive 时自动 abort。`layoutAsync` 透传 signal 并在 `elk.layout` 前后校验 `aborted`；失败一律 `throw LayoutError`——`undefined` 是 react-query 的非法缓存值（会 `isSuccess=true` 但 `data=undefined` 打破下游守卫），throw 才能进 retry/error 通道。下游解构 `error`，error 态兜底 `setNodes(未布局节点)` 保证画布非空。

---

### 10. Context 不能优化重渲（关键误解澄清）

这是本项目最容易被误开的一枪，专列一节。

**误解**："把 nodeShow 放进 Context，组件 `useContext` 只在 nodeShow 变时重渲，能优化性能。"

**真相**（[React 官方文档](https://react.dev/reference/react/useContext)）：

> Changing the provided value re-renders **all** the components using that context.

且 `React.memo` **挡不住** `useContext` 触发的重渲——memo 只拦 props，不拦 context。

**真正能选择性重渲的只有**：
- 拆 Context（粒度更细）
- `useMemo` 稳定 value
- **外部 store + selector**（resso / zustand 的 `useSyncExternalStore` 路线）

**对本项目的含义**：`nodeShow` 变更本来就会 `clearLayoutCache` → ELK 重算所有节点位置 → 全部节点重渲（**这是正确行为**，颜色/宽度变了本就该全刷）。唯一"本可避免却没避免"的是 `query` 变更（`setQuery` 故意不清 layout 缓存，见 `store.ts:272-277`），但那要靠 selector store 才能兑现，**Context 兑现不了**。

> 所以 [3.1 Big Rock 1](#big-rock-1nodeshow-下发去- entitysharedsetting-mutate) 推 nodeShow 下发时，**理由是"契约洁净 + 解 domain 耦合"，不是"性能"**。不要承诺减重渲。

---

## 第二部分 · 本项目架构现状巡礼

### 2.1 渲染层

`FlowNode`（`flow/FlowNode.tsx`）是唯一的 xyflow 自定义节点，承担**全部节点级 UI**：标题栏、fold/unfold、moveUp/Down/delete、note 编辑入口、资源（res）弹层按钮，并按 `entity.type` 分派到：

- `EntityProperties`（readonly 字段表）
- `EntityForm`（editable 表单，antd Form + 一组 memo 子组件）
- `EntityCard`（card 卡片，带图片/描述）

**note 有两套并行的持久化通道**（有意设计，非重复）：
- readonly/card note → React Query `['notes']` + `updateNote` 网络 API（`NoteShowOrEdit.tsx`）
- editable note → `EditingSession.updateNote` 就地改 `editingObject.$note`（不走网络，提交时一并上报）

**右键菜单**用 xyflow v12 标准事件 `onNodeContextMenu`/`onPaneContextMenu`/`onNodeDoubleClick`（`FlowGraph.tsx:33-53,87-92`），菜单自绘为 antd `Menu` + `position:fixed` 定位。是推荐做法。

### 2.2 样式与尺寸层

三层样式注入：
1. **配色**（`colors.ts`）：纯函数，按"值 → 标签 → 实体类型"三级优先级返回节点背景色，默认色集中在 `NODE_SHOW_DEFAULTS` 常量对象（与 antd token 刻意解耦）；`getNodeBackgroundColor(entity, nodeShow?)` 显式收 nodeShow 入参（避免 FlowNode memo stale）。
2. **尺寸**（`calcWidthHeight.ts`）：纯估算函数，带名常量（`FIELD_ROW_H`/`CARD_DS_H`/`DESC_ROW_H` 等）预先算 `width/height`，喂给 ELK；节点宽度走 `dimensions.ts` 的 `getNodeWidth`（240/280 单一来源）。dev-only `HeightDriftGuard` 子组件读 `measured.height` 对账估算漂移。
3. **CSS 变量**（`FlowStyleManager.tsx`）：只在 `documentElement` 上设一个 `--edge-stroke-width`（配合 `style.css` 的 `svg .react-flow__edge-path`）；其余 nodeWidth/editNodeWidth 那两行被注释掉了——**名不副实**，几乎是空壳。

`colors` / `calcWidthHeight` / `getDsLenAndDesc` / `simpleStrRowCount` / `dimensions` 均有纯函数测试（CLAUDE.md 约定只测纯逻辑）。

### 2.3 集成层

4 个 route（`Table`/`TableRef`/`Record`/`RecordRef`）+ 1 个 dragPanel 固定页消费者，都调同一个 `useEntityToGraph` 把 entityMap 推进 xyflow store。每个 route 都 `return null`——它们是"副作用型图形控制器"，真正的画布 `<FlowGraph>` 是它们在 `CfgEditorApp.tsx` 里的兄弟/父节点。

**配置回流**：`NodeShowSetting`/`FlowVisualizationSetting` 调 `setNodeShow` 写 `store.nodeShow`，**仅当布局相关字段（`NODESHOW_LAYOUT_KEYS`：算法/间距/尺寸/拓扑过滤）变化时**才 `clearLayoutCache()`（全局 `removeQueries(['layout'])`）；纯颜色变更不清缓存。`useEntityToGraph` 把 nodeShow 放进 useMemo 依赖（触发节点重转换）和 layout queryKey（`pickLayoutKeys` 收窄——纯颜色不进 key，故不重布局）。

### 2.4 数据模型与 store 边界

- `domain/entityModel.ts`：`Entity` 判别联合 + `EntityGraph` + `EntitySharedSetting`；并把编辑态↔视图契约 `EFitView`/`EditingObjectRes` 也放在 domain（消除 `flow → routes` 的反向依赖，是正确的"共享内核"）。
- `store.ts`：用 vendored 的 `resso`（基于 `useSyncExternalStore`）做全局 `StoreState`，所有 setter 走"整体赋新对象 + `setPref` 持久化"模式。
- **关键边界**：`resso` store 与 xyflow 内部 store 是**两套独立的 `useSyncExternalStore` 实例**。`store.ts` 的 `StoreState` 里**没有** `setNodes`/`setEdges`/`panZoom`——那些是 xyflow 内部 store 的字段，`useEntityToGraph` 通过 `useReactFlow()` 取命令面（`setNodes`/`setEdges`/`setViewport`/`getViewport`/`fitView`），仅留一个 `useStore(s=>s.panZoom!==null)` 只读切片作 viewport 就绪信号。
- `services/editingSession.ts`：每会话可变 store（`useSyncExternalStore` 订阅 `structureVersion`），产出 `EditingObjectRes` 喂给 flow 的 layout queryKey 与 viewport 决策。**这是 2026-07 由"方案 C"根治的成熟设计**——值类编辑不 bump（零重渲契约）、结构类编辑 bump，`editingObject` 就地变异是有意设计，**勿改**（详见 memory 与 [3.0 勿动 A4](#a4record-entitymap-的-12-项-usememo-依赖折叠时全量重建)）。

**测试覆盖**：`fillHandles`/`convertNodeAndEdges`/`colors`/`calcWidthHeight`/`getDsLenAndDesc`/`simpleStrRowCount`/`dimensions`/`viewportMath`/`layoutAsync`(vi.mock ELK)/`nodeShowLayoutKeys`/`entityPredicates`/`Folds`/`editingSession.getEditingObjectRes` 均有纯函数测试；flow 层的 effect 本身（命令式推 nodes/viewport）不测（CLAUDE.md 约定 vitest 只测纯逻辑，effect 行为靠手测）。

---

## 第三部分 · 重构机会

> 本部分只剩两类内容：[3.0 勿动清单](#30-先读我这些看起来像问题的请勿动)（曾疑为缺陷、核实后确认勿动的"伪问题"）与 [3.1 P3+ 架构级 big rocks](#31-p3--架构级-big-rocks)（需独立 PR 的根治项）。
> 每条 big rock 统一格式：**现状**(file:line) / **问题** / **改法**(代码) / **风险**。

### 3.0 先读我：这些"看起来像问题"的，请勿动

> 这 6 条都曾被怀疑为缺陷，深入核实后确认是**合理设计或诊断错误**。重构时若遇到，**不要动**；其中几条与 memory 记录的"方案 C 根治"一致。误改会引入真实回归。

#### A1. 4 个 route "复制"useEntityToGraph 骨架 → 夸大

四个 route 确实都遵循 `entityMap → fillHandles → 菜单 → useEntityToGraph → return null` 模板，但**真正逐字重复的只有 ~3 行**（`fillHandles(entityMap)` + 一行 `useEntityToGraph({...})` + `return null`）。`useEntityToGraph` 的实参四处各异：

- `Table` 只传基础 5 参；
- `TableRef` 加 `nodeDoubleClickFunc`；
- `Record` 传 `editingObjectRes` 并按 `isEditing` 切 `type`；
- `RecordRef` 传 `setFitViewForPathname`/`nodeShow`/`nodeDoubleClickFunc`。

entityMap 构造各用完全不同的 creator（`TableEntityCreator` / `includeRefTables` / `RecordEntityCreator`|`RecordEditEntityCreator` / `createRefEntities`），菜单逻辑也完全不同。`Record.tsx` 还有 ~180 行额外逻辑（useMutation、EditingSession、folds、structureVersion、3 个 useEffect），与 `Table`/`TableRef` 的 ~50 行天差地别，**绝非同构**。`fillHandles` 确为 4 处必调的约定（`entityToNodeAndEdge.ts:26`，纯 mutate 设 handleIn/handleOut，不依赖 route 信息），但这只是 4 处共用的一个调用约定，不构成"抽公共 hook"的理由。

> **结论**：不需要抽公共 `useFlowPage` 工厂。重复量未到 DRY 阈值，强抽会增加间接层。

#### A2. nodeShow 固定页"陈旧快照" → 有意的 per-graph override

`useEntityToGraph.ts:91` `nodeShowSetting = nodeShow ?? currentNodeShow`；`makeFixedPage`（`store.ts:394-405`）**有意冻结整套视图参数**（`recordRefIn`/`refOutDepth`/`maxNode`/`nodeShow` 4 项），让固定页保留创建时的配置快照，与主图当前配置解耦。

主图（`RecordRefRoute.tsx:210`）读全局 `useMyStore().nodeShow`；固定页（`CfgEditorApp.tsx:136/148`）读 `fix.nodeShow` 快照；同屏不一致是**有意设计**——用户期望"我钉住的引用图保持当初的样子"。

> **结论**：不是 bug。任何 nodeShow 下发重构（[big rock 1](#big-rock-1nodeshow-下发去- entitysharedsetting-mutate)）都**必须保留这个 per-graph override**，子组件**绝不能**直接 `useMyStore().nodeShow`，否则 FixedPage 配置失效。

#### A3. Table/TableRef 的 `getDefaultIdInTable` useCallback 重复 → 只两处，非项目级

`Table.tsx:23-24` 与 `TableRef.tsx:29-30` 确实逐字相同（`useCallback((tableName) => getDefaultIdInTable(schema, tableName, curId), [schema, curId])`），但**仅此两处兄弟 route**，`Record.tsx` 不用此模式，不是项目级约定。被调函数 `getDefaultIdInTable` 已在 `schema.test.ts` 单测。

> **结论**：可顺带提取，但**不建议单独开 PR**，价值低。

#### A4. Record entityMap 的 12 项 useMemo 依赖、折叠时全量重建 → 折叠改拓扑，是有意设计

`Record.tsx:129-130` 的 useMemo deps 实测为 12 项：`[isEditing, curId, schema, recordResult, tauriConf, resourceDir, resMap, curTable, folds, setFolds, session, structureVersion]`。fold 触发时 `recordEditEntityCreator.ts:136-138/208-210` 折叠分支直接 `continue` **不创建子 entity**——子节点不存在于 entityMap 而非 hidden，这是**改拓扑**，必须重建 entityMap + 重布局。

`folds`（本地 React state）与 `obj.$fold`（提交载荷）**双存储是有意设计**（`recordEditEntityCreator.ts:628-637` `getFoldState`：本地 Folds 优先、`obj.$fold` 兜底），提供"本地覆盖持久化 fold"语义。

> **结论**：折叠全量重建是正确的。这与 memory 记录的 [EditingSession 方案 C 根治] 一致——**editingObject 就地变异是有意设计，勿改 reducer**。

#### A5. EFitView/EditingObjectRes 下沉 domain 是依赖方向反了 → 错诊

`entityModel.ts:372-387` 注释明说"消除 `flow/useEntityToGraph → routes/record/editingObject` 的反向依赖"。真正生产 `EditingObjectRes` 的是 `services/editingSession.ts`（`EditingSession` 持有 `fitView`/`fitViewToIdPosition`，`structureChange()` 在结构编辑时 set `FitId`+position），消费方是 `flow/useEntityToGraph.ts:132-156`。把契约类型放 domain，让 flow 与 routes/services 都依赖 domain 而非互相依赖，是**正确的"共享内核"式依赖反转**。`.oxlintrc.json` 的边界规则（`src/flow/**` 禁 import `@/routes/**`；`src/services/**` 禁 import `@/flow/**`）也确认 `routes→flow` 是允许的既定方向。

> **结论**：依赖方向正确，勿动。教学启示：依赖反转的目标层应是中性契约，`EFitView` 名字虽带视图语义，但作为"编辑态↔视图"的共享契约放 domain 合理。

#### A6. useEntityToGraph 的超长 effect + 受控/非受控混用 + 手算 viewport → 诊断错误

这条最容易被误开枪，逐点澄清：

1. **effect deps 数组长 ≠ 过度触发**：其中 `setNodes`/`setEdges`/`fitView`/`setViewport`/`getViewport`（来自 `useReactFlow`）与 `viewportReady`（`useStore` 只读切片）都是**稳定引用**，`flowGraph` 是 memo 过的 context——真正会变的只有数据/缓存/编辑态/菜单回调等少数项。

2. **受控/非受控混用不是 bug**（见 [第一部分 §4](#4-受控-vs-非受控本项目是混用)）：xyflow 完全支持 `defaultNodes` + 命令式 `setNodes`。

3. **手算 viewport 不能简单替换**：FitFull 已用 `fitView`，但 FitId 分支的"relayout 后锚点屏幕坐标不变"方程 fitView **做不到**，必须自算（已抽到 `viewportMath.computeStableViewport`，不变量有测试）。

4. **FlowGraph.tsx:85 的 `// fitView` 注释悬空**——确实是死注释，可清理，但不是 bug。

> **结论**：effect 本身不是问题。命令面已收口到 `useReactFlow`、FitFull 已 `fitView` 化、FitId 数学已抽纯函数（`panZoom` 仅留 null 判断作就绪信号）——这些都是稳定性/可读性改进，不是修 bug。

---

### 3.1 P3+ — 架构级 big rocks

> 需独立 PR，遵守依赖顺序。详见 [第四部分](#第四部分--路线图与依赖)。

#### Big Rock 1. nodeShow 下发，去 entity.sharedSetting mutate（B + C + F 的共同根因）

> ✅ **已落地（2026-07，轻档）**：`nodeShow/notes` 下发到 ReactFlow node.data（`EntityNodeData = {entity, nodeShow?, notes?}`），`FlowNode` 读 `nodeProps.data.{nodeShow,notes}`；`convertNodeAndEdges` 删 mutate，`EntityBase.sharedSetting`/`EntitySharedSetting`/`EntityGraph` 一并移除（domain 解耦）。`calcWidthHeight(entity, nodeShow?, notes?)`、`getNodeWidth(entity, nodeShow?)` 收显式入参（`layoutAsync.nodeToLayoutChild` 从 node.data 取值，保住布局路径的 note 高度估算）。`query` 走 `useMyStore()`（resso per-key 订阅，零多余重渲）。**FixedPage per-graph override 保留**（nodeShow 全程经 node.data 下发，子组件不直接读 store.nodeShow）。重档（SharedSettingContext）未做——轻档已达成「契约洁净 + 解 domain 耦合」目标。

**现状**：`EntityBase.sharedSetting`（`entityModel.ts:286`）让 domain Entity 携带 `NodeShowType`（UI 配置）——domain↔presentation 耦合。`useEntityToGraph` 构造一次 `sharedSetting`，`entityToNodeAndEdge.ts:70` 在循环里把**同一引用盖章**到每个 entity（mutate 入参）。渲染层遍布 `entity.sharedSetting?.nodeShow?.xxx` 两层可选链（`FlowNode`/`EntityProperties`/`EntityForm`/`EntityCard`/`colors`）。而 `nodeShow`/`query` 本就在 resso store——等于 `store → entity.sharedSetting → prop → ?. ` 一圈空转。

这是多条 finding 的共同根因：B（convertNodeAndEdges 副作用 mutate）、C（memo 纪律）、F（nodeShow 透传）。其中 **color stale 已止血**（`getNodeBackgroundColor` 显式收 `nodeShow` 入参 + `FlowNode` 的 color memo deps 含 `nodeShow`），但 **mutate 根因仍在**——本 big rock 是根治。

**改法**（分两档）：

- **轻档（推荐先做，零结构风险）**：把解析后的 nodeShow 切片直接放进 ReactFlow node 的 `data`，`FlowNode` 读 `nodeProps.data.nodeShow`（解析好的、含 per-graph override）而非 `entity.sharedSetting?.nodeShow?.`。子组件形参从 `sharedSetting?` 改 `nodeShow?`。（`colors.ts` `getNodeBackgroundColor(entity, nodeShow)` 签名已改好。）`query` 可直接 `useMyStore()` 读（无 per-graph override，`setQuery` 明确不进 layout）。
- **重档（可选根治）**：新增 `SharedSettingContext`，**Provider 必须放在 `ReactFlowProvider` 内、`<ReactFlow>` 之前把 ReactFlow 一起包住**（⚠️ 现 `FlowGraphContext.tsx:100-102` 只包 `{children}` 不包 ReactFlow，FlowNode 读不到）。`value` 要 `useMemo` 稳定引用。

**无论哪档**：删 `entityToNodeAndEdge.ts:70` 的 mutate，entity 保持不可变 memo-safe；同步改 `entityToNodeAndEdge.test.ts` 的"回写实体"断言。

> ⚠️ **关键陷阱**：(1) 子组件**绝不能**直接 `useMyStore().nodeShow`，否则破坏 FixedPage per-graph override（[A2](#a2-nodeshow-固定页陈旧快照--有意的-per-graph-override)）。(2) **不要承诺减重渲**——nodeShow 变更本就该全图刷新（[§10](#10-context-不能优化重渲关键误解澄清)）；性能增益只在 `query` 变更，且需 selector store 才能兑现，Context 兑现不了。

---

#### Big Rock 2. FlowStyleManager 提升 + 全局 CSS 变量竞争根治

**现状**：`FlowStyleManager` 在**每个 `<FlowGraph>` 实例**里挂载（`FlowGraph.tsx:77`），`useEffect` 在 `documentElement` 上设 `--edge-stroke-width`，cleanup 里 `removeProperty`。当多实例并存（`CfgEditorApp` 主区 + 侧面板），一个实例 unmount 的 cleanup 会 `removeProperty` 把另一个实例仍在用的变量抹掉 → 右画布失样式。

**改法**：`FlowStyleManager` 从每个 `FlowGraph` 实例提升到 `CfgEditorApp` 顶层**只挂一次**（CSS 变量本就该全局唯一），cleanup **不再 `removeProperty`**。

**风险**：低。需确认多画布共享同一 `--edge-stroke-width` 是预期（当前 `nodeShow` 全局唯一，是）。

---

#### Big Rock 3. ActiveMediaPlayerContext 替代 document.querySelectorAll

**现状**（`ResPopover.tsx:50-58`）：`VideoAudioSyncer.onPlay` 用 `document.querySelectorAll("video")`/`"audio"` 全局暂停其他播放器，副作用逃逸出 React 子树。⚠️ **纠正误判**：`onPause`（`:71-82`）用的是 `ref.current.querySelectorAll("audio")`，作用域限本组件，**无问题，勿混改**。

**改法**：轻档给 `Popover` 加 `destroyTooltipOnHide`（video 随子树卸载）；重档引入 `ActiveMediaPlayerContext`（`register`/`take` 互斥），Provider 放 `CfgEditorApp` 或 `FlowGraphProvider` 外层。

**风险**：中。`onPause` 局部 ref 逻辑保留不动。

---

#### Big Rock 4. store.ts queryKey 纳入拓扑 setting（P4）

**现状**（`store.ts:260-270`）：混入 `clearLayoutCache`/`invalidateAllQueries` 命令式 cache 操作，11 处 setter 调 `clearLayoutCache`；`setQuery:272-277` 用特意注释解释"为何不加"，说明模式已脆弱（开发者每次加 setting 要记得该不该加）。

**改法**（根治）：把拓扑相关 setting（`maxImpl`/`refDepth`/`maxNode`/`recordRef*`/`tauriConf`）纳入 layout queryKey，setter 只剩 `setPref` 不再 `clearLayoutCache`——让"是否清缓存"问题消失，store 重新变纯状态容器（Query Key Factory 模式）。

**风险**：⚠️ 前置 `P0-3`（layoutAsync throw）已修，undefined 污染不会再暴露；⚠️ `'e'` 标记层级不变以保 `Record.tsx:87` prefix 语义。可选最小方案：先审计去冗余（`setFixedPagesConf` 的 `clearLayoutCache` 嫌疑最大，pageConf 不直接改当前 layout 输入）。

---

#### Big Rock 5. 菜单变 ReactFlow props，删 FlowGraphContext（可选）

**现状**：`useEntityToGraph.ts:121-162` 把"注册菜单回调"+"推 nodes/edges"+"算 viewport"三件不相关的事塞进**同一 useEffect 同一 `if(newNodes)` 守卫**——菜单被布局就绪状态错误门控，初次挂载/路由切换的 layout 拉取期右键无菜单。`FlowGraphContext` 是项目自创的"child→parent 反向注册"模式（xyflow 官方 context-menu 示例是直接把 `onNodeContextMenu` 作为稳定 props 传给 `ReactFlow`）。

**改法**：
- **方案 A（最小修复）**：拆菜单 effect 到独立 + 路由 unmount 清理（cleanup 里 `setNodes([])`/`setEdges([])`）。
- **方案 B（根治，可选）**：删 `FlowGraphContext` 反向注册，菜单变 `ReactFlow` props（`onNodeContextMenu` 等），各 route 用 `useCallback` 稳定函数直接传。

**风险**：⚠️ naive 拆分会 worse——B 布局拉取期内菜单已是 B 的、可见节点仍是 A 的，B 菜单函数作用在 A 节点上。正确最小修复是拆分 + unmount 清理。方案 B 涉及 4 个 route + `FlowGraph` 签名变更，回归面广，建议方案 A 先行。

---

#### Big Rock 6. calcWidthHeight measured 重排（仅护栏持续报警才上）

**现状**：作者 `calcWidthHeight.ts:6-7` 已书面弃案（measured 重排导致闪烁与代码绕晕）。魔数已常量化、dev-only `HeightDriftGuard` 护栏已落地（读 `measured.height` 对账估算，偏差 >8px & >5% 按 id 去重 `console.warn`）。

**改法**：**仅当 dev 护栏显示系统性漂移**（多种实体类型都偏）才考虑 measured 驱动重排，且要走 React Query `staleTime` 缓存避免每次重算。优先调常量不重排。

**风险**：高。与作者已解决的闪烁问题直接冲突。优先级最低。

---

## 第四部分 · 路线图与依赖

### 依赖顺序硬约束

```
Big Rock 1 (nodeShow 下发 + 删 entity.sharedSetting mutate)
  └─► 同时解决 B + C + F（同根）；color stale 已止血，本 rock 根治 mutate

Big Rock 4 (store queryKey 纳入拓扑 setting)
  └─► 前置已满足（layoutAsync throw 已修）；'e' 标记层级不变以保 Record.tsx:87 prefix

Big Rock 6 (calcWidthHeight measured 重排)
  └─► 仅当 dev height drift 护栏（已落地）持续报警才上
```

### Big Rocks PR 规划（架构级，独立 PR）

| PR | 内容 | 依赖 | 回归面 |
|---|---|---|---|
| ✅ BR-1（已完成 2026-07，轻档）| nodeShow 轻档下发（node.data 切片）+ 删 entity.sharedSetting mutate | — | 所有节点渲染、改主题色、FixedPage |
| BR-4 | FlowStyleManager 提升 + CSS 变量竞争 | — | 多画布并存场景 |
| BR-5 | ActiveMediaPlayerContext + Popover destroyTooltipOnHide | — | 视频节点播放 |
| BR-6 | store queryKey 纳入拓扑 setting | — | 所有 setter 的缓存行为 |
| BR-7（可选）| 菜单变 ReactFlow props，删 FlowGraphContext（方案 A 先行）| — | 右键菜单、路由切换 |

### 验证策略

本项目 vitest **只测纯逻辑不测 UI**（CLAUDE.md 约定），big rock 落地时要么**配纯函数测试**，要么**手测关键路径**：

- BR-1：改主题色 → 节点背景实时刷新；FixedPage 钉住的引用图保持当初配置（per-graph override 不失效）
- BR-4：多画布并存（Splitter 主区 + 固定页）样式不互相抹掉
- BR-5：视频节点播放互斥、Popover 关闭后 video/media 停止
- BR-6：改拓扑相关 setting（`maxImpl`/`refDepth`/…）后布局正确刷新；改非拓扑 setting 不触发重布局
- BR-7：路由切换 / 初次挂载的 layout 拉取期右键菜单可用

进度追踪建议：把上表做成 issue/milestone，每条标注改动文件、预估行数、是否需手测。

---

## 附录 A · xyflow v12 概念速查

> 精选与本项目最相关的概念，每条带"在本项目哪里用到"。

| 概念 | 一句话 | 本项目位置 |
|---|---|---|
| **ReactFlowProvider** | 建独立图实例（store/viewport/dimensions），hook 绑定最近祖先 | `FlowGraph.tsx:76`；`CfgEditorApp.tsx` 多实例 |
| **nodeTypes + 自定义节点** | `{[type]: Component}` 注册分派，必须定义在组件外 | `FlowGraph.tsx:14-16` |
| **NodeProps\<TData\>** | 节点 props，泛型强类型 data；引用常变是 memo 议题根因 | `FlowNode.tsx:43` |
| **Handle / Position / nodrag** | 连接桩；`className='nodrag'` 禁拖拽 | `FlowNode.tsx:238-239`；`EntityForm` 全程 `nodrag` |
| **受控 vs 非受控** | `nodes` prop + onNodesChange vs `defaultNodes` + 命令式 setNodes | 本项目混用 |
| **useReactFlow vs useStore** | 公开稳定 instance vs 内部 escape-hatch（panZoom 非公开）| `useEntityToGraph` 命令面走 useReactFlow，仅留 useStore 读 viewport 就绪 |
| **Viewport 数学** | `screen = world*zoom + t`，线性变换公开契约 | `flow/viewportMath.ts` `computeStableViewport`（+不变量测试）|
| **视口三档** | 手算 / fitView / setCenter | FitFull 走 fitView、FitId 走 computeStableViewport+setViewport（`useEntityToGraph`）|
| **声明 vs measured 尺寸** | `node.width/height`（喂 ELK）vs `node.measured.{w,h}`（实测）| 生产只走声明（`calcWidthHeight.ts`）；dev-only `HeightDriftGuard` 对账 measured |
| **ElkNode 边界框** | ELK 把 `{w,h}` 当不可压缩框，不测 DOM | `layoutAsync.ts` `nodeToLayoutChild` |
| **ELK Web Worker + 竞态** | workerUrl 模式不阻塞主线程；失败 throw LayoutError + AbortSignal | `layoutAsync.ts`（+vi.mock 测试）|
| **React Query 缓存布局** | queryKey 是输入指纹（`pickLayoutKeys` 收窄）；removeQueries vs invalidateQueries 时序 | `useEntityToGraph`；`Record.tsx:87` |
| **Context 不优化重渲** | useContext value 变重渲所有 consumer，memo 挡不住 | （本项目潜在误区）|
| **defaultEdgeOptions** | ReactFlow prop，统一边 style/type/marker | 本项目未用（潜在改进）|
| **useNodesInitialized** | "实测就绪"信号；node 尺寸为 0 会永 false | 本项目未用 |
| **FlowGraphContext 反向注册** | 项目自创：child 把菜单工厂反向注册给 parent | `FlowGraphContext.ts`；`useEntityToGraph.ts:84,123-127` |
| **useSyncExternalStore** | 外部可变 store 接 React 官方机制 | `resso.ts`；`editingSession.ts:67-87` |
| **就地变异 + 共享引用契约** | editingObject 各回调就地改同一对象；值类不 bump 版本 | `editingSession.ts`；**勿改** |
| **Outlet context** | react-router 父→子传值主干 | `CfgEditorApp.tsx:87-89` |
| **判别联合 Entity** | ReadOnly\|Editable\|Card，type 字段区分 + 类型守卫 | `entityModel.ts:300-352` |

---

## 附录 B · 参考来源

**xyflow / React Flow v12**
- 官方文档（API reference）：https://reactflow.dev/api-reference/types/node-props
- 迁移到 v12（`node.measured`）：https://reactflow.dev/learn/troubleshooting/migrate-to-v12
- `useNodesInitialized`：https://reactflow.dev/api-reference/hooks/use-nodes-initialized
- `useReactFlow`：https://reactflow.dev/api-reference/hooks/use-react-flow
- ELK 集成示例：https://reactflow.dev/examples/layout/elkjs
- 多 Provider 并存（Discussion #2068）：https://github.com/xyflow/xyflow/discussions/2068
- 节点 width/height vs measured（Discussion #3764）：https://github.com/xyflow/xyflow/discussions/3764
- 自定义节点 width=0 致 measured 永不赋值（issue #5215）：https://github.com/xyflow/xyflow/issues/5215
- 自定义节点重渲性能（issue #4711）：https://github.com/xyflow/xyflow/issues/4711
- 内部 ResizeObserver 回写尺寸（Discussion #2973）：https://github.com/xyflow/xyflow/discussions/2973
- Stately React Flow + ELK pipeline cookbook：https://stately.ai/docs/packages/graph/react-flow-elk-pipeline

**React Query / TanStack**
- TkDodo「Mastering Mutations」：https://tkdodo.eu/blog/mastering-mutations-in-react-query
- TkDodo「Error Handling in React Query」：https://tkdodo.eu/blog/react-query-error-handling
- TkDodo「Breaking React Query's API on purpose」：https://tkdodo.eu/blog/breaking-react-querys-api-on-purpose
- 业务状态码不进 onError（Discussion #3931）：https://github.com/TanStack/query/discussions/3931
- 缓存资源释放（Discussion #1109）：https://github.com/TanStack/query/discussions/1109
- 官方 mutations 文档：https://tanstack.com/query/v4/docs/framework/react/guides/mutations
- QueryCache reference：https://tanstack.com/query/v4/docs/reference/QueryCache
- React Query cleanup：https://peterkrieg.com/react-query-cleanup/

**React**
- `useContext` 重渲语义（官方）：https://react.dev/reference/react/useContext
- Hooks 官方文档：https://react.dev/reference/react

**elkjs**
- ELK layered/mrtree 算法选项：https://www.eclipse.org/elk/reference.html

**antd / 浏览器**
- antd Form（`initialValue` 仅初始化生效）：https://ant.design/components/form/
- antd AutoComplete（onChange vs onSearch 时机）：https://ant.design/components/auto-complete/
- ant-design#56102（Form.Item initialValue mount-only）：https://github.com/ant-design/ant-design/issues/56102
- MDN `URL.revokeObjectURL`：https://developer.mozilla.org/en-US/docs/Web/API/URL/revokeObjectURL_static
- MDN `Intl.Segmenter`：https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl/Segmenter

---

*本文档由通读 `src/flow` 全部源码 + 多 agent 对每条重构机会做 xyflow v12 文档对齐验证后综合而成。[3.0 勿动清单](#30-先读我这些看起来像问题的请勿动) 是经"读源码确认现状 → 查官方文档对齐惯用法 → 评估风险"三步核实后被否决的"伪问题"，专门列出以防误伤；[3.1 P3+ big rocks](#31-p3--架构级-big-rocks) 是剩余的架构级根治项。*


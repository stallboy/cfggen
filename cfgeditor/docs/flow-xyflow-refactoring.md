# cfgeditor 图形层（src/flow）现状与优化 + xyflow v12 教学指南

> **双线文档**：左手是 xyflow v12 + ELK 的入门教学（每个概念落在本项目的真实代码上），
> 右手是 `src/flow` 的**现状说明 + 优化 backlog**——只记当前状态与待优化点，不记变更历史。
>
> **读者画像**：熟悉 React、不熟 xyflow。读完既能把这些图形模式复用到下一个项目，
> 也能拿出一份可执行的优化 backlog。
>
> **覆盖版本**：`@xyflow/react ^12.11.2` + `elkjs`（Web Worker 模式）。
> 文中 `file:line` 基于当前代码，可能漂移，以函数/符号名为主索引。

---

## 目录

- [0. 怎么读这篇文档](#0-怎么读这篇文档)
- [第一部分 · xyflow v12 核心概念（教学主线）](#第一部分--xyflow-v12-核心概念教学主线)
- [第二部分 · 架构现状](#第二部分--架构现状)
- [第三部分 · 现状澄清（看起来像问题、实则勿动）](#第三部分--现状澄清看起来像问题实则勿动)
- [第四部分 · 待优化点](#第四部分--待优化点)
- [附录 A · xyflow v12 概念速查](#附录-a--xyflow-v12-概念速查)
- [附录 B · 参考来源](#附录-b--参考来源)

---

## 0. 怎么读这篇文档

### 两条阅读路径

- **只想学 xyflow**：读 [第一部分](#第一部分--xyflow-v12-核心概念教学主线)（10 个核心概念，每个对照本项目代码）。
- **只想改本项目**：读 [第二部分](#第二部分--架构现状) 摸清现状，再跳 [第三部分](#第三部分--现状澄清看起来像问题实则勿动) 避免误伤"伪问题"，最后看 [第四部分](#第四部分--待优化点) 的优化 backlog。

### xyflow 心智模型（三句话）

1. xyflow 是一张**带相机的节点-边画布**：React 层负责画节点（自定义组件），内部 zustand store 负责存位置/视口/尺寸，布局算法（ELK）是可选的外挂。
2. 一切 hook（`useReactFlow` / `useStore` / 节点内的隐式 Context）都绑定**最近祖先 `ReactFlowProvider`**，没有 scope 参数——作用域纯靠 React Context 嵌套。
3. 节点是普通 React 组件，但它的 props（`NodeProps`）由 xyflow 内部 store 驱动、引用常变——这是后续所有 `memo` 议题的根因。

### 本项目数据流全景图

```
Entity (domain/entityModel.ts)
  │   判别联合：ReadOnlyEntity | EditableEntity | CardEntity（纯 domain，不携带 UI 配置）
  ▼
convertNodeAndEdges (flow/entityToNodeAndEdge.ts)
  │   产出 EntityNode[] / EntityEdge[]；node.data = {entity, nodeShow?, notes?}（呈现层下发袋，无 mutate）
  │   顺手 fillHandles 标注连接点
  ▼
layoutAsync (flow/layoutAsync.ts)
  │   ELK Web Worker 算位置；结果由 React Query 按
  │   ['layout', pathname, ('e'), pickLayoutKeys(nodeShow), topologyKeys] 缓存
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
      nodeTypes.node 唯一自定义节点；读 nodeProps.data.{entity, nodeShow, notes}，
      按 entity.type 分派 EntityProperties(readonly) / EntityForm(editable) / EntityCard(card)；
      query 经 useMyStore() per-key 读（resso selector，零多余重渲）
```

**关键特征**：route 组件（Table/Record/…）`return null`——它们是"副作用型图形控制器"，真正的画布是它们在 `CfgEditorApp.tsx` 里的父级 `<FlowGraph>`。

### 术语速查表

| 术语 | 含义 | 本项目位置 |
|---|---|---|
| **受控模式** | `<ReactFlow nodes={..} onNodesChange={..}/>`，真相源在外部 React state | 本项目**未采用** |
| **非受控模式** | `defaultNodes` 初始化，所有权归 xyflow 内部 store | `FlowGraph.tsx` |
| **命令式 patch** | 通过 `setNodes`/`setEdges` 直接写内部 store | `useEntityToGraph.ts` |
| **声明尺寸** | `node.width/height`，你告诉 xyflow/ELK 节点多大 | `calcWidthHeight.ts` 估算后写入 |
| **实测尺寸** | `node.measured.{width,height}`，xyflow 挂载 DOM 后 ResizeObserver 实测 | 生产路径**从不读取**（仅 dev-only height drift 护栏对账）|
| **FitFull / FitId / FitNone** | 视口策略：全图适配 / 锚点不动 / 不动 | `entityModel.ts` `EFitView` |
| **node.data 下发袋** | ReactFlow node 的 data 承载 entity（domain）+ nodeShow/notes（呈现层） | `FlowGraph.tsx` `EntityNodeData` |

---

## 第一部分 · xyflow v12 核心概念（教学主线）

### 1. ReactFlowProvider：图实例的边界

`ReactFlowProvider` 建立一个**独立的图实例**——自带一个内部 zustand store，持有这个图的 nodes/edges/viewport/dimensions/panZoom。`useReactFlow()` / `useStore()` 都只访问**最近祖先 Provider** 的图，没有 scope 参数。

**本项目用法**：每个 `<FlowGraph>` 自带一个 `<ReactFlowProvider>`（`FlowGraph.tsx`）。而 `CfgEditorApp.tsx` 在 Splitter 主区与（可选的）拖拽面板**各挂一个** `<FlowGraph>`——所以同一页面最多并存两个互不可见的图实例（主区 + 侧面板）。

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
// FlowGraph.tsx —— nodeTypes 必须定义在组件外（模块级）
const nodeTypes: NodeTypes = { node: FlowNode };

// EntityNodeData 是「呈现层下发袋」：entity 是纯 domain，nodeShow/notes 是呈现层配置。
// 用 type 别名（非 interface）以满足 xyflow Node<T> 的 T extends Record<string,unknown> 约束。
export type EntityNodeData = {
    entity: Entity;
    nodeShow?: NodeShowType;
    notes?: Map<string, string>;
};
export type EntityNode = Node<EntityNodeData, "node">;

<ReactFlow defaultNodes={...} defaultEdges={...} nodeTypes={nodeTypes} ... />
```

```tsx
// FlowNode.tsx
export const FlowNode = memo(function FlowNode(nodeProps: NodeProps<EntityNode>) {
  const entity = nodeProps.data.entity;
  const nodeShow = nodeProps.data.nodeShow;   // 呈现层配置经 node.data 下发（保留 per-graph override）
  const notes = nodeProps.data.notes;
  // 按 entity.type 分派：EntityProperties / EntityForm / EntityCard
});
```

**三条 v12 官方最佳实践，本项目全部合规**：

1. `nodeTypes` 定义在组件外（`FlowGraph.tsx`）——避免每次 render 新建对象导致 xyflow 认为节点类型变了、全量卸载重挂。
2. 自定义节点用 `React.memo` 包裹（`FlowNode.tsx`）。
3. 用 `NodeProps<TData>` 泛型强类型 `data`（`EntityNode`）。

> **教学点**：xyflow 在内部 store 每次变更后会**重组传给节点组件的 props**，`NodeProps` 引用常变。即使 `nodeTypes`/`memo` 都合规，若 `useMemo` 的 deps 写错（漏掉实际读取的可变字段），节点仍会 stale——例如 `FlowNode` 的 color memo 必须把 `nodeShow`（来自 `nodeProps.data.nodeShow`）放进 deps：entity 引用不变时，改主题色仍要重算 color，否则背景 stale。

---

### 3. Handle / Position / nodrag

```tsx
// FlowNode.tsx
{handleIn  && <Handle type='target' position={Position.Left}  id='@in'  style={handleStyle} />}
{handleOut && <Handle type='source' position={Position.Right} id='@out' style={handleStyle} />}
```

- `<Handle type='source'|'target' position id>` 是节点上的**连接桩**；同一节点可有多个 Handle，用 `id` 区分（本项目用 `@in`/`@out`，或字段名 `item.name`）。
- `className='nodrag'` 让按钮/表单区域**不触发节点拖拽**——`EntityForm.tsx` 里所有可交互控件都带这个 class。

> **教学点**：`fillHandles`（`entityToNodeAndEdge.ts`）在 entityMap 构建后遍历 `sourceEdges`，把 `handleIn`/`handleOut` 标记盖到 entity/field 上，渲染时 FlowNode 据此决定是否画 `<Handle>`。这是"数据驱动连接桩"的干净做法。

---

### 4. 受控 vs 非受控（本项目是混用）

xyflow 支持两种节点管理范式：

| 模式 | 写法 | 真相源 |
|---|---|---|
| **受控** | `<ReactFlow nodes={nodes} edges={edges} onNodesChange={..} onEdgesChange={..}/>` | 外部 React state |
| **非受控** | `defaultNodes={..}`（初值）+ `useReactFlow().setNodes(..)` 命令式 patch | xyflow 内部 store |

**本项目是混用**：

```tsx
// FlowGraph.tsx —— 非受控初始化（空数组）
<ReactFlow defaultNodes={defaultNodes} defaultEdges={defaultEdges} ...>

// useEntityToGraph.ts —— 命令式 patch
setNodes(newNodes);
setEdges(edges);
```

> **关键澄清（勿误判为 bug）**：这种混用**不是错误**，是 xyflow 完全支持的"非受控初值 + 命令式注入"模式。它的代价是**数据流不直观**——route `return null`，节点哪来的要看 effect；但好处是可在 effect 里"等 ELK 布局算完一次性原子注入"，避免中间态闪烁。详见 [第三部分 A6](#a6-useentitytograph-的超长-effect--受控非受控混用--手算-viewport)。

---

### 5. useReactFlow（公开稳定）vs useStore（内部 escape-hatch）

> 本项目 `useEntityToGraph` 的命令面收口到 `useReactFlow`（`setNodes`/`setEdges`/`setViewport`/`getViewport`/`fitView`），仅保留一个 `useStore` 只读切片作 viewport 就绪信号，不调用内部 `panZoom` 方法。先把两类 API 的概念讲清：

| API | 性质 | 返回 | 稳定性 |
|---|---|---|---|
| `useReactFlow()` | **公开** instance API | `ReactFlowInstance`：`setNodes`/`setEdges`/`setViewport`/`getViewport`/`fitView`/`fitBounds`/`setCenter`/`getNodesBounds` | 跨版本稳定，带 `Promise`/`duration`/`ease` |
| `useStore(selector)` | **内部** zustand escape-hatch | store 任意切片 | 文档自标 escape-hatch；`panZoom` 尤其是**实现细节**，非公开契约 |

```ts
// useEntityToGraph.ts —— 命令走公开 useReactFlow，仅留一个 useStore 只读切片
const viewportReady = useStore((state) => state.panZoom !== null);  // 就绪信号（panZoom 非空即就绪）
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

> **教学点**：这个方程是**代数正确**的，且 xyflow 没有提供"保持某点屏幕坐标不变"的高层原语——所以 FitId 分支**必须自算**，不能用 `fitView` 替代（这是 [第三部分 A6](#a6-useentitytograph-的超长-effect--受控非受控混用--手算-viewport) 的重要一条）。`useEntityToGraph` 的 FitId 分支调用此函数 + `useReactFlow().setViewport`；不变量由 `viewportMath.test.ts` 锁定。

---

### 7. 视口 API 三档抽象

| 档位 | API | 适用 |
|---|---|---|
| **底层手算** | `getNodesBounds(nodes)` + `getViewportForBounds(bounds, w, h, minZoom, maxZoom, padding)` + `setViewport` | 需要 padding/minZoom/maxZoom 精确控制 |
| **高层适配** | `fitView({ padding, minZoom, maxZoom, nodes, duration })` | "把所有节点框进视口" |
| **居中一点** | `setCenter(x, y, { zoom, duration })` | "把某世界坐标居中" |

**本项目 FitFull 走高层 fitView，FitId 走自算**（`useEntityToGraph.ts`）：

```ts
// FitFull 分支：直接用公开 fitView
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

**本项目只走第一轨**，从不读 measured（`calcWidthHeight.ts` 顶部注释说明：放弃 measured 重排——异步等待实测尺寸会引入闪烁与控制流复杂度，改为预先估算）：

```ts
// layoutAsync.ts —— nodeToLayoutChild：从 node.data 取 entity + 呈现层配置，估算尺寸喂 ELK
function nodeToLayoutChild(node: EntityNode, id2RectMap): ElkNode {
  const { entity, nodeShow, notes } = node.data;
  const [width, height] = calcWidthHeight(entity, nodeShow, notes);   // 纯魔数估算
  id2RectMap.set(node.id, { x: 0, y: 0, width, height });
  return { id: node.id, width, height };                              // ELK 当作不可压缩边界框
}
```

> **关键教学点**：ELK（elkjs）**不会去测 DOM**，它把 `ElkNode.{width,height}` 当不可压缩边界框排布。所以你喂给 ELK 的尺寸**必须与真实渲染一致**，否则节点重叠或留异常间隙。这正是 `calcWidthHeight` 的带名常量（`FIELD_ROW_H=41`/`CARD_DS_H=38`/`DESC_ROW_H=22`/`IMAGE_H=200`…）精度的意义，也是 measured vs 声明权衡的根源。魔数已常量化、并有 dev-only height drift 护栏兜底；仅当护栏显示系统性漂移才考虑 measured 重排（见 [第四部分 优化点 3](#优化点-3-calcwidthheight-measured-重排仅护栏持续报警才上)）。
>
> **已知坑**（v12）：custom node 把 `width/height` 设为 0 会让 `measured` 永不赋值、`useNodesInitialized()` 永远 false（[issue #5215](https://github.com/xyflow/xyflow/issues/5215)）。

---

### 9. ELK 布局管线：Web Worker + React Query 缓存 + 竞态

```ts
// layoutAsync.ts —— 模块级单例 ELK，workerUrl 模式跑在 Web Worker
const elk = new ELK({ workerUrl: elkWorkerUrl });

// 异步布局：失败一律 throw LayoutError（绝不 resolve undefined），透传 AbortSignal
if (signal?.aborted) throw new LayoutError('aborted', '...');
const { children } = await elk.layout(graph);
if (signal?.aborted) throw new LayoutError('aborted', '...');
if (!children) throw new LayoutError('no_children', '...');
toPositionMap(id2RectMap, children);
if (!allPositionXYOk(nodes, id2RectMap)) throw new LayoutError('dropped_nodes', '...');
return id2RectMap;
```

```ts
// useEntityToGraph.ts —— React Query 缓存布局结果；queryKey 含布局 + 拓扑输入，透传 ctx.signal
const layoutKeys = pickLayoutKeys(nodeShowSetting);            // nodeShow 布局字段（算法/间距/尺寸/拓扑过滤）
const topologyKeys = { maxImpl, refIn, refOutDepth, maxNode,   // 拓扑 setting（影响 entityMap 节点集合）
                       recordRefIn, recordRefInShowLinkMaxNode, recordRefOutDepth, recordMaxNode, tauriConf };
const queryKey = isEdited ? ['layout', pathname, 'e', layoutKeys, topologyKeys]
                          : ['layout', pathname, layoutKeys, topologyKeys];
const { data: id2RectMap, error: layoutError } = useQuery({
  queryKey,
  queryFn: (ctx) => layoutAsync(nodes, edges, strategy, nodeShowSetting, ctx.signal),
  staleTime: isEdited ? 0 : 1000 * 60 * 5,   // 编辑态每次重取，浏览态 5min
});
```

**三个教学子概念**：

1. **内容寻址缓存**（Query Key Factory）：queryKey 是 queryFn 输入的"指纹"——凡影响 ELK 输出的变量都进 key。本项目 queryKey 含两块：`pickLayoutKeys(nodeShow)`（nodeShow 布局字段）+ `topologyKeys`（影响节点集合的拓扑 setting）。改它们 → queryKey 变 → 缓存自然失效重布局；**纯颜色字段不进 key** → 改主题色命中缓存不重跑 ELK。store setter 因此**不再手动清 layout 缓存**——"改 setting 要不要清缓存"的判断消失，store 重新变纯状态容器。

2. **`removeQueries` vs `invalidateQueries`**（本项目用得很精妙）：
   - `invalidate`：对 active query 立即 refetch，但**重渲前的旧闭包会算出旧布局**。
   - `removeQueries`：物理删 entry，等重渲用**新闭包**重取。
   - 本项目 `Record.tsx` `onStructureChange` 用 `removeQueries(['layout', pathname, 'e'])` 只清编辑态缓存——这是对的。任何 queryKey 重构都必须保留这个 prefix 语义（`'e'` 标记保持在 `pathname` 之后同一层级）。

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

**对本项目的含义**：nodeShow 布局字段变更 → queryKey 变 → 缓存失效 → ELK 重算所有节点位置 → 全部节点重渲（**这是正确行为**，颜色/宽度变了本就该全刷）。`query` 变更走的是另一条路：`FlowNode`/`EntityProperties`/`EntityCard` 各自 `useMyStore()` 订阅 query（resso per-key selector），query 变只重渲这些高亮组件、不触发重布局——这正是「外部 store + selector」路线能兑现、Context 兑现不了的选择性重渲。

> 所以 nodeShow 经 node.data 下发（而非子组件各自 `useContext`）的理由是"**契约洁净 + 保留 per-graph override + 解 domain 耦合**"，不是"Context 性能差"。不要承诺减重渲——nodeShow 变更本就该全图刷新。

---

## 第二部分 · 架构现状

### 2.1 渲染层

`FlowNode`（`flow/FlowNode.tsx`）是唯一的 xyflow 自定义节点，承担**全部节点级 UI**：标题栏、fold/unfold、moveUp/Down/delete、note 编辑入口、资源（res）弹层按钮，并按 `entity.type` 分派到：

- `EntityProperties`（readonly 字段表）
- `EntityForm`（editable 表单，antd Form + 一组 memo 子组件）
- `EntityCard`（card 卡片，带图片/描述）

FlowNode 从 `nodeProps.data` 读 `entity`/`nodeShow`/`notes`，把 `nodeShow` 作为 prop 下发子组件（`EntityProperties`/`EntityForm`/`EntityCard` 形参是 `nodeShow?`）。`query` 不经 node.data 下发——它无 per-graph override，渲染组件各自 `useMyStore()` per-key 订阅。

**note 有两套并行的持久化通道**（有意设计，非重复）：
- readonly/card note → React Query `['notes']` + `updateNote` 网络 API（`NoteShowOrEdit.tsx`）
- editable note → `EditingSession.updateNote` 就地改 `editingObject.$note`（不走网络，提交时一并上报）

**右键菜单**用 xyflow v12 标准事件 `onNodeContextMenu`/`onPaneContextMenu`/`onNodeDoubleClick`（`FlowGraph.tsx`），菜单自绘为 antd `Menu` + `position:fixed` 定位。是推荐做法。

### 2.2 样式与尺寸层

三层样式注入：
1. **配色**（`colors.ts`）：纯函数，按"值 → 标签 → 实体类型"三级优先级返回节点背景色，默认色集中在 `NODE_SHOW_DEFAULTS` 常量对象（与 antd token 刻意解耦）；`getNodeBackgroundColor(entity, nodeShow?)` 显式收 nodeShow 入参（让调用方把 nodeShow 放进 useMemo deps，避免 FlowNode memo stale）。
2. **尺寸**（`calcWidthHeight.ts`）：纯估算函数 `calcWidthHeight(entity, nodeShow?, notes?)`，带名常量（`FIELD_ROW_H`/`CARD_DS_H`/`DESC_ROW_H` 等）预先算 `width/height` 喂给 ELK；节点宽度走 `dimensions.ts` 的 `getNodeWidth(entity, nodeShow?)`（240/280 单一来源）。dev-only `HeightDriftGuard` 子组件读 `measured.height` 对账估算漂移。
3. **CSS 变量**（`FlowStyleManager.tsx`）：在 `CfgEditorApp` 顶层**挂单实例**（非每个 FlowGraph），只在 `documentElement` 上设一个 `--edge-stroke-width`（配合 `style.css` 的 `svg .react-flow__edge-path`）；cleanup 不 `removeProperty`——CSS 变量全局唯一，app 生命周期内常驻。

`colors` / `calcWidthHeight` / `getDsLenAndDesc` / `simpleStrRowCount` / `dimensions` 均有纯函数测试（CLAUDE.md 约定只测纯逻辑）。

### 2.3 集成层与缓存失效契约

4 个 route（`Table`/`TableRef`/`Record`/`RecordRef`）+ 1 个 dragPanel 固定页消费者，都调同一个 `useEntityToGraph` 把 entityMap 推进 xyflow store。每个 route 都 `return null`——它们是"副作用型图形控制器"，真正的画布 `<FlowGraph>` 是它们在 `CfgEditorApp.tsx` 里的兄弟/父节点。

**配置回流**：`NodeShowSetting`/`FlowVisualizationSetting` 调 `setNodeShow` 写 `store.nodeShow`。`useEntityToGraph` 把 nodeShow 经 `convertNodeAndEdges` 写进每个 `node.data`（触发节点重转换），并放进 layout queryKey（`pickLayoutKeys` 收窄——纯颜色不进 key，故不重布局）。

**layout 缓存失效全靠 queryKey 自反映**（Query Key Factory），store setter 不再手动清缓存：
- `pickLayoutKeys(nodeShow)`：nodeShow 布局字段（算法/间距/尺寸/拓扑过滤）。
- `topologyKeys`：影响 entityMap 节点集合的 store setting（`maxImpl`/`refIn`/`refOutDepth`/`maxNode`/`recordRefIn`/`recordRefInShowLinkMaxNode`/`recordRefOutDepth`/`recordMaxNode`/`tauriConf`）。
- 改任一块 → queryKey 变 → 缓存自然失效重布局；改纯颜色/query（不进 key）→ 命中缓存不重跑 ELK。

前提：各 route 的 entityMap useMemo 依赖都包含自身用到的拓扑 setting（Table:`maxImpl`；TableRef:`refIn`/`refOutDepth`/`maxNode`；Record:`tauriConf`；RecordRef:`recordRefInShowLinkMaxNode`/`tauriConf` + recordRef* 经 recordRefResult 流入），所以拓扑变更时 entityMap 重算 + queryKey 变 + 重布局，链路自洽。

### 2.4 数据模型与 store 边界

- `domain/entityModel.ts`：`Entity` 判别联合（ReadOnly/Editable/Card）；`EFitView`/`EditingObjectRes`（编辑态↔视图契约）放 domain，消除 `flow → routes` 的反向依赖（正确的"共享内核"）。**Entity 不携带任何 UI 配置**——`nodeShow`/`notes` 是呈现层数据，经 ReactFlow `node.data` 下发（见 §2.1/2.3）。
- `store.ts`：vendored `resso`（基于 `useSyncExternalStore`）做全局 `StoreState`。**resso 是 per-key 订阅**（Proxy 上每个属性各自 `useSyncExternalStore`），`const {a,b} = useMyStore()` 只订阅 a/b 两个 key、各自独立触发。所有 setter 走 `setPref` 持久化；layout 缓存失效全靠 queryKey，setter 不手动清缓存。
- **关键边界**：resso store 与 xyflow 内部 store 是**两套独立的 `useSyncExternalStore` 实例**。`StoreState` 里**没有** `setNodes`/`setEdges`/`panZoom`——那些是 xyflow 内部 store 的字段，`useEntityToGraph` 通过 `useReactFlow()` 取命令面，仅留一个 `useStore(s=>s.panZoom!==null)` 只读切片作 viewport 就绪信号。
- `services/editingSession.ts`：每会话可变 store（`useSyncExternalStore` 订阅 `structureVersion`），产出 `EditingObjectRes` 喂给 flow 的 layout queryKey 与 viewport 决策。方案 C 设计：值类编辑不 bump（零重渲契约）、结构类编辑 bump，`editingObject` 就地变异是有意设计，**勿改**（见 [第三部分 A4](#a4-record-entitymap-全量重建--折叠改拓扑有意设计)）。

**测试覆盖**：`fillHandles`/`convertNodeAndEdges`/`colors`/`calcWidthHeight`/`getDsLenAndDesc`/`simpleStrRowCount`/`dimensions`/`viewportMath`/`layoutAsync`(vi.mock ELK)/`nodeShowLayoutKeys`/`entityPredicates`/`Folds`/`editingSession.getEditingObjectRes` 均有纯函数测试；flow 层的 effect 本身（命令式推 nodes/viewport）不测（CLAUDE.md 约定 vitest 只测纯逻辑，effect 行为靠手测）。

---

## 第三部分 · 现状澄清（看起来像问题、实则勿动）

> 这 6 条都曾被怀疑为缺陷，深入核实后确认是**合理设计或诊断错误**。重构时若遇到，**不要动**；误改会引入真实回归。

#### A1. 4 个 route "复制"useEntityToGraph 骨架 → 夸大

四个 route 确实都遵循 `entityMap → fillHandles → 菜单 → useEntityToGraph → return null` 模板，但**真正逐字重复的只有 ~3 行**（`fillHandles(entityMap)` + 一行 `useEntityToGraph({...})` + `return null`）。`useEntityToGraph` 的实参四处各异：

- `Table` 只传基础 5 参；
- `TableRef` 加 `nodeDoubleClickFunc`；
- `Record` 传 `editingObjectRes` 并按 `isEditing` 切 `type`；
- `RecordRef` 传 `setFitViewForPathname`/`nodeShow`/`nodeDoubleClickFunc`。

entityMap 构造各用完全不同的 creator（`TableEntityCreator` / `includeRefTables` / `RecordEntityCreator`|`RecordEditEntityCreator` / `createRefEntities`），菜单逻辑也完全不同。`Record.tsx` 还有 ~180 行额外逻辑（useMutation、EditingSession、folds、structureVersion、3 个 useEffect），与 `Table`/`TableRef` 的 ~50 行天差地别，**绝非同构**。`fillHandles` 确为 4 处必调的约定（纯 mutate 设 handleIn/handleOut，不依赖 route 信息），但这只是 4 处共用的一个调用约定，不构成"抽公共 hook"的理由。

> **结论**：不需要抽公共 `useFlowPage` 工厂。重复量未到 DRY 阈值，强抽会增加间接层。

#### A2. nodeShow 固定页"陈旧快照" → 有意的 per-graph override

`useEntityToGraph` 里 `nodeShowSetting = nodeShow ?? currentNodeShow`；`makeFixedPage`（`store.ts`）**有意冻结整套视图参数**（`recordRefIn`/`refOutDepth`/`maxNode`/`nodeShow` 4 项），让固定页保留创建时的配置快照，与主图当前配置解耦。

主图（`RecordRef.tsx`）读全局 `useMyStore().nodeShow`；固定页（`CfgEditorApp.tsx`）读 `fix.nodeShow` 快照；同屏不一致是**有意设计**——用户期望"我钉住的引用图保持当初的样子"。

> **结论**：不是 bug。nodeShow 全程经 `nodeProps.data` 下发（`convertNodeAndEdges` 写入 node.data），子组件**绝不能**直接 `useMyStore().nodeShow`（当前架构已如此），否则 FixedPage 配置失效。

#### A3. Table/TableRef 的 `getDefaultIdInTable` useCallback 重复 → 只两处，非项目级

`Table.tsx` 与 `TableRef.tsx` 确实逐字相同（`useCallback((tableName) => getDefaultIdInTable(schema, tableName, curId), [schema, curId])`），但**仅此两处兄弟 route**，`Record.tsx` 不用此模式，不是项目级约定。被调函数 `getDefaultIdInTable` 已在 `schema.test.ts` 单测。

> **结论**：可顺带提取，但**不建议单独开 PR**，价值低。

#### A4. Record entityMap 全量重建、折叠时重建 → 折叠改拓扑，有意设计

Record 的 useMemo deps 实测为 12 项：`[isEditing, curId, schema, recordResult, tauriConf, resourceDir, resMap, curTable, folds, setFolds, session, structureVersion]`。fold 触发时 `recordEditEntityCreator.ts` 折叠分支直接 `continue` **不创建子 entity**——子节点不存在于 entityMap 而非 hidden，这是**改拓扑**，必须重建 entityMap + 重布局。

`folds`（本地 React state）与 `obj.$fold`（提交载荷）**双存储是有意设计**（`recordEditEntityCreator.ts` `getFoldState`：本地 Folds 优先、`obj.$fold` 兜底），提供"本地覆盖持久化 fold"语义。

> **结论**：折叠全量重建是正确的。这与 `editingSession` 的方案 C 设计一致——**editingObject 就地变异是有意设计，勿改 reducer**。

#### A5. EFitView/EditingObjectRes 下沉 domain 是依赖方向反了 → 错诊

`entityModel.ts` 注释明说"消除 `flow/useEntityToGraph → routes/record/editingObject` 的反向依赖"。真正生产 `EditingObjectRes` 的是 `services/editingSession.ts`（`EditingSession` 持有 `fitView`/`fitViewToIdPosition`，`structureChange()` 在结构编辑时 set `FitId`+position），消费方是 `flow/useEntityToGraph.ts`。把契约类型放 domain，让 flow 与 routes/services 都依赖 domain 而非互相依赖，是**正确的"共享内核"式依赖反转**。`.oxlintrc.json` 的边界规则（`src/flow/**` 禁 import `@/routes/**`；`src/services/**` 禁 import `@/flow/**`）也确认 `routes→flow` 是允许的既定方向。

> **结论**：依赖方向正确，勿动。教学启示：依赖反转的目标层应是中性契约，`EFitView` 名字虽带视图语义，但作为"编辑态↔视图"的共享契约放 domain 合理。

#### A6. useEntityToGraph 的超长 effect + 受控/非受控混用 + 手算 viewport → 诊断错误

这条最容易被误开枪，逐点澄清：

1. **effect deps 数组长 ≠ 过度触发**：其中 `setNodes`/`setEdges`/`fitView`/`setViewport`/`getViewport`（来自 `useReactFlow`）与 `viewportReady`（`useStore` 只读切片）都是**稳定引用**，`flowGraph` 是 memo 过的 context——真正会变的只有数据/缓存/编辑态/菜单回调等少数项。

2. **受控/非受控混用不是 bug**（见 [第一部分 §4](#4-受控-vs-非受控本项目是混用)）：xyflow 完全支持 `defaultNodes` + 命令式 `setNodes`。

3. **手算 viewport 不能简单替换**：FitFull 已用 `fitView`，但 FitId 分支的"relayout 后锚点屏幕坐标不变"方程 fitView **做不到**，必须自算（已抽到 `viewportMath.computeStableViewport`，不变量有测试）。

4. **`FlowGraph.tsx` 的 `// fitView` 注释悬空**——确实是死注释，可清理，但不是 bug。

> **结论**：effect 本身不是问题。命令面收口到 `useReactFlow`、FitFull 走 `fitView`、FitId 数学抽纯函数（`panZoom` 仅留 null 判断作就绪信号）——这些都是稳定性/可读性改进，不是修 bug。

---

## 第四部分 · 待优化点

> 每条统一格式：**现状问题**(file/symbol) / **优化方向**(代码) / **风险**。

### 优化点 1. ActiveMediaPlayerContext 替代 document.querySelectorAll

**现状问题**（`ResPopover.tsx` `VideoAudioSyncer.onPlay`）：用 `document.querySelectorAll("video")`/`"audio"` 全局暂停其他播放器，副作用逃逸出 React 子树。⚠️ `onPause` 用的是 `ref.current.querySelectorAll("audio")`，作用域限本组件，**无问题，勿混改**。

**优化方向**：轻档给 `Popover` 加 `destroyTooltipOnHide`（video 随子树卸载）；重档引入 `ActiveMediaPlayerContext`（`register`/`take` 互斥），Provider 放 `CfgEditorApp` 或 `ReactFlowProvider` 外层。

**风险**：中。`onPause` 局部 ref 逻辑保留不动。

---

### 优化点 2. 菜单变 ReactFlow props，删 FlowGraphContext（可选）

**现状问题**：`useEntityToGraph.ts` 把"注册菜单回调"+"推 nodes/edges"+"算 viewport"三件不相关的事塞进**同一 useEffect 同一 `if(newNodes)` 守卫**——菜单被布局就绪状态错误门控，初次挂载/路由切换的 layout 拉取期右键无菜单。`FlowGraphContext` 是项目自创的"child→parent 反向注册"模式（xyflow 官方 context-menu 示例是直接把 `onNodeContextMenu` 作为稳定 props 传给 `ReactFlow`）。

**优化方向**：
- **方案 A（最小修复）**：拆菜单 effect 到独立 + 路由 unmount 清理（cleanup 里 `setNodes([])`/`setEdges([])`）。
- **方案 B（根治，可选）**：删 `FlowGraphContext` 反向注册，菜单变 `ReactFlow` props（`onNodeContextMenu` 等），各 route 用 `useCallback` 稳定函数直接传。

**风险**：⚠️ naive 拆分会 worse——B 布局拉取期内菜单已是 B 的、可见节点仍是 A 的，B 菜单函数作用在 A 节点上。正确最小修复是拆分 + unmount 清理。方案 B 涉及 4 个 route + `FlowGraph` 签名变更，回归面广，建议方案 A 先行。

---

### 优化点 3. calcWidthHeight measured 重排（仅护栏持续报警才上）

**现状问题**：`calcWidthHeight.ts` 顶部注释明确放弃 measured 重排（异步等待实测尺寸→闪烁 + 控制流复杂度）。魔数已常量化、dev-only `HeightDriftGuard` 护栏已落地（读 `measured.height` 对账估算，偏差 >8px & >5% 按 id 去重 `console.warn`）。

**优化方向**：**仅当 dev 护栏显示系统性漂移**（多种实体类型都偏）才考虑 measured 驱动重排，且要走 React Query `staleTime` 缓存避免每次重算。优先调常量不重排。

**风险**：高。与已解决的闪烁问题直接冲突。优先级最低。

---

## 附录 A · xyflow v12 概念速查

> 精选与本项目最相关的概念，每条带"在本项目哪里用到"。

| 概念 | 一句话 | 本项目位置 |
|---|---|---|
| **ReactFlowProvider** | 建独立图实例（store/viewport/dimensions），hook 绑定最近祖先 | `FlowGraph.tsx`；`CfgEditorApp.tsx` 多实例 |
| **nodeTypes + 自定义节点** | `{[type]: Component}` 注册分派，必须定义在组件外 | `FlowGraph.tsx` |
| **NodeProps\<TData\>** | 节点 props，泛型强类型 data；引用常变是 memo 议题根因 | `FlowNode.tsx` |
| **node.data 下发袋** | node.data 承载 entity（domain）+ nodeShow/notes（呈现层）；type 别名满足 `Record<string,unknown>` | `FlowGraph.tsx` `EntityNodeData`；`convertNodeAndEdges` 写入 |
| **Handle / Position / nodrag** | 连接桩；`className='nodrag'` 禁拖拽 | `FlowNode.tsx`；`EntityForm` 全程 `nodrag` |
| **受控 vs 非受控** | `nodes` prop + onNodesChange vs `defaultNodes` + 命令式 setNodes | 本项目混用 |
| **useReactFlow vs useStore** | 公开稳定 instance vs 内部 escape-hatch（panZoom 非公开）| `useEntityToGraph` 命令面走 useReactFlow，仅留 useStore 读 viewport 就绪 |
| **Viewport 数学** | `screen = world*zoom + t`，线性变换公开契约 | `flow/viewportMath.ts` `computeStableViewport`（+不变量测试）|
| **视口三档** | 手算 / fitView / setCenter | FitFull 走 fitView、FitId 走 computeStableViewport+setViewport（`useEntityToGraph`）|
| **声明 vs measured 尺寸** | `node.width/height`（喂 ELK）vs `node.measured.{w,h}`（实测）| 生产只走声明（`calcWidthHeight.ts`）；dev-only `HeightDriftGuard` 对账 measured |
| **ElkNode 边界框** | ELK 把 `{w,h}` 当不可压缩框，不测 DOM | `layoutAsync.ts` `nodeToLayoutChild` |
| **ELK Web Worker + 竞态** | workerUrl 模式不阻塞主线程；失败 throw LayoutError + AbortSignal | `layoutAsync.ts`（+vi.mock 测试）|
| **React Query 缓存布局** | queryKey 是输入指纹（`pickLayoutKeys` + `topologyKeys`），store setter 不手动清缓存 | `useEntityToGraph`；`Record.tsx` `onStructureChange` |
| **resso per-key 订阅** | `useMyStore()` 解构几个 key 只订阅这几个（Proxy 每属性各自 useSyncExternalStore） | `store/resso.ts`；query 经此 selector 读 |
| **Context 不优化重渲** | useContext value 变重渲所有 consumer，memo 挡不住；选择性重渲靠外部 store + selector | nodeShow 经 node.data 下发；query 经 resso per-key |
| **defaultEdgeOptions** | ReactFlow prop，统一边 style/type/marker | 本项目未用（潜在改进）|
| **useNodesInitialized** | "实测就绪"信号；node 尺寸为 0 会永 false | 本项目未用 |
| **FlowGraphContext 反向注册** | 项目自创：child 把菜单工厂反向注册给 parent（见优化点 2）| `FlowGraphContext.ts`；`useEntityToGraph.ts` |
| **useSyncExternalStore** | 外部可变 store 接 React 官方机制 | `resso.ts`；`editingSession.ts` |
| **就地变异 + 共享引用契约** | editingObject 各回调就地改同一对象；值类不 bump 版本 | `editingSession.ts`；**勿改** |
| **Outlet context** | react-router 父→子传值主干 | `CfgEditorApp.tsx` |
| **判别联合 Entity** | ReadOnly\|Editable\|Card，type 字段区分 + 类型守卫；不携带 UI 配置 | `entityModel.ts` |

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

*本文档只记 `src/flow` 的**现状**与**待优化点**，不记变更历史。[第三部分 现状澄清](#第三部分--现状澄清看起来像问题实则勿动) 是经"读源码确认现状 → 查官方文档对齐惯用法 → 评估风险"三步核实后被否决的"伪问题"，专门列出以防误伤；[第四部分 待优化点](#第四部分--待优化点) 是剩余的架构级优化项。*

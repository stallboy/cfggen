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
  - [3.1 P0 — 立即修复](#31-p0--立即修复)
  - [3.2 P1 — 高 ROI quick wins](#32-p1--高-roi-quick-wins)
  - [3.3 P2 — 中等改动](#33-p2--中等改动)
  - [3.4 P3+ — 架构级 big rocks](#34-p3--架构级-big-rocks)
- [第四部分 · 路线图与依赖](#第四部分--路线图与依赖)
- [附录 A · xyflow v12 概念速查](#附录-a--xyflow-v12-概念速查)
- [附录 B · 参考来源](#附录-b--参考来源)

---

## 0. 怎么读这篇文档

### 两条阅读路径

- **只想学 xyflow**：读 [第一部分](#第一部分--xyflow-v12-核心概念教学主线)（10 个核心概念，每个对照本项目代码）。
- **只想改本项目**：跳到 [第三部分](#第三部分--重构机会)，先看 [3.0 勿动清单](#30-先读我这些看起来像问题的请勿动) 避免误伤，再按 P0→P3 顺序取用。

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
  │   通过 useStore(s=>s.setNodes) / useReactFlow 拿到 xyflow 内部 API，
  │   在 useEffect 里命令式推 nodes/edges/viewport
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
| **实测尺寸** | `node.measured.{width,height}`，xyflow 挂载 DOM 后 ResizeObserver 实测 | 本项目**从不读取** |
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

> **教学点**：xyflow 在内部 store 每次变更后会**重组传给节点组件的 props**，`NodeProps` 引用常变。即使 `nodeTypes`/`memo` 都合规，若 `useMemo` 的 deps 写错（见 [3.1 P0-2](#p0-2-flownode-背景色-memo-的-stale-陷阱)），节点仍会 stale。

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

这是本项目最值得收口的一处（见 [3.3 P2](#p2-1-useentitytograph-命令面收口-useractflow--fitfull-fitview-化)），先把概念讲清：

| API | 性质 | 返回 | 稳定性 |
|---|---|---|---|
| `useReactFlow()` | **公开** instance API | `ReactFlowInstance`：`setNodes`/`setEdges`/`setViewport`/`getViewport`/`fitView`/`fitBounds`/`setCenter`/`getNodesBounds` | 跨版本稳定，带 `Promise`/`duration`/`ease` |
| `useStore(selector)` | **内部** zustand escape-hatch | store 任意切片 | 文档自标 escape-hatch；`panZoom` 尤其是**实现细节**，非公开契约 |

```ts
// useEntityToGraph.ts:86-92 —— 当前的"一半内部一半公开"写法
const width  = useStore((state) => state.width);      // 内部
const height = useStore((state) => state.height);     // 内部
const setNodes = useStore((state) => state.setNodes);  // 内部（但 useReactFlow 也有）
const setEdges = useStore((state) => state.setEdges);  // 内部（同上）
const panZoom  = useStore((state) => state.panZoom);   // 内部，非公开契约 ⚠️
const { getNodesBounds } = useReactFlow();             // 公开
```

> **教学点**：业务代码应优先 `useReactFlow`，把 `useStore` 留给 xyflow 自身或"只读尺寸"等必要场景。`setNodes`/`setEdges` 两者都有，但走公开 instance 更稳；`panZoom` 是实现细节，升级时可能被改。

---

### 6. Viewport 数学：screen = world·zoom + translation

`viewport = { x, y, zoom }` 是一个**线性变换**参数（公开契约，`screenToFlowPosition`/`flowToScreenPosition` 基于此）：

```
屏幕坐标 = 世界坐标 × zoom + 平移量
screenX = worldX * zoom + x
screenY = worldY * zoom + y
```

本项目在 `useEntityToGraph.ts:147-153` 的注释里完整记录了这个公式，并用它反解"relayout 后保持锚点屏幕坐标不动"：

```ts
// useEntityToGraph.ts:147-154 —— FitId 分支：让 (id) 节点 relayout 前后屏幕位置不变
// screenX = x*zoom + tx，要求 nowX*zoom + nowTx === x*zoom + tx
const nowTx = x * zoom + tx - nowX * zoom;
const nowTy = y * zoom + ty - nowY * zoom;
panZoom.setViewport({ x: nowTx, y: nowTy, zoom });
```

> **教学点**：这个方程是**代数正确**的，且 xyflow 没有提供"保持某点屏幕坐标不变"的高层原语——所以 FitId 分支**必须自算**，不能用 `fitView` 替代（这是 [3.0 勿动清单](#a6-useentitytograph-的超长-effect--受控非受控混用--手算-viewport) 的重要一条）。

---

### 7. 视口 API 三档抽象

| 档位 | API | 适用 |
|---|---|---|
| **底层手算** | `getNodesBounds(nodes)` + `getViewportForBounds(bounds, w, h, minZoom, maxZoom, padding)` + `setViewport` | 需要 padding/minZoom/maxZoom 精确控制 |
| **高层适配** | `fitView({ padding, minZoom, maxZoom, nodes, duration })` | "把所有节点框进视口" |
| **居中一点** | `setCenter(x, y, { zoom, duration })` | "把某世界坐标居中" |

**本项目当前全走底层手算**（`useEntityToGraph.ts:133-135`）：

```ts
// FitFull 分支：等价于一次 fitView({ padding: 0.2, minZoom: 0.3, maxZoom: 1 })
const bounds = getNodesBounds(newNodes);
const viewportForBounds = getViewportForBounds(bounds, width, height, 0.3, 1, 0.2);
panZoom.setViewport(viewportForBounds);
```

> **重构机会**：FitFull 可整段替换为 `fitView({ padding: 0.2, minZoom: 0.3, maxZoom: 1 })`（见 [3.3 P2-1](#p2-1-useentitytograph-命令面收口-useractflow--fitfull-fitview-化)）；FitId 必须保留手算。

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

> **关键教学点**：ELK（elkjs）**不会去测 DOM**，它把 `ElkNode.{width,height}` 当不可压缩边界框排布。所以你喂给 ELK 的尺寸**必须与真实渲染一致**，否则节点重叠或留异常间隙。这正是 `calcWidthHeight` 一堆魔数（`41`/`38`/`22`/`200`…）精度的意义，也是 measured vs 声明权衡的根源。详见 [3.2 P1-2](#p2-2-calcwidthheight-魔数常量化--dev-实测对账护栏) / [3.4 big rock 6](#big-rock-6calcwidthheight-measured-重排仅护栏持续报警才上)。
>
> **已知坑**（v12）：custom node 把 `width/height` 设为 0 会让 `measured` 永不赋值、`useNodesInitialized()` 永远 false（[issue #5215](https://github.com/xyflow/xyflow/issues/5215)）。

---

### 9. ELK 布局管线：Web Worker + React Query 缓存 + 竞态

```ts
// layoutAsync.ts:11 —— 模块级单例 ELK，workerUrl 模式跑在 Web Worker
const elk = new ELK({ workerUrl: elkWorkerUrl });

// layoutAsync.ts:95-107 —— 异步布局 + 竞态守卫
const { children } = await elk.layout(graph);
if (children) {
  toPositionMap(id2RectMap, children);
  if (!allPositionXYOk(nodes, id2RectMap)) {   // 请求开始时的 nodes 与返回时可能不同
    console.log('layout ignored', nodes);
    return;                                     // ⚠️ 返回 undefined —— 见 P0-3
  }
  return id2RectMap;
}
```

```ts
// useEntityToGraph.ts:104-111 —— React Query 缓存布局结果
const { data: id2RectMap } = useQuery({
  queryKey: ['layout', pathname, (isEdited ? 'e' : ''), nodeShowSetting],
  queryFn: () => layoutAsync(nodes, edges, strategy, nodeShowSetting),
  staleTime: isEdited ? 0 : 1000 * 60 * 5,   // 编辑态每次重取，浏览态 5min
});
```

**三个教学子概念**：

1. **内容寻址缓存**：queryKey 是 queryFn 输入的"指纹"——凡影响输出的变量都必须进 key。本项目把 `pathname`/`mode`/`nodeShowSetting` 进了 key，但**拓扑相关 setting（maxImpl/refDepth/…）漏进 key**，靠 setter 手动 `clearLayoutCache` 同步——这是"声明式依赖退化成命令式失效"，见 [3.3 P2-3](#p2-3-setnodeshow-缓存策略升级依赖-p0-3-先修)。

2. **`removeQueries` vs `invalidateQueries`**（本项目用得很精妙）：
   - `invalidate`：对 active query 立即 refetch，但**重渲前的旧闭包会算出旧布局**。
   - `removeQueries`：物理删 entry，等重渲用**新闭包**重取。
   - 本项目 `Record.tsx:87` `onStructureChange` 用 `removeQueries(['layout', pathname, 'e'])` 只清编辑态缓存——这是对的。任何 queryKey 重构都必须保留这个 prefix 语义。

3. **竞态的正确处置**：react-query v5 给每个 queryFn 注入 `AbortSignal`，query 变 stale/inactive 时自动 abort。`layoutAsync` 当前用 `allPositionXYOk` 守卫 + 静默 `return undefined`——守卫思路对，但 `undefined` 是非法缓存值（见 [P0-3](#p0-3-layoutasync-失败返回-undefined-导致偶发空图)），应改 throw + 透传 signal。

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

> 所以 [3.4 big rock 1](#big-rock-1nodeshow-下发去- entitysharedsetting-mutate) 推 nodeShow 下发时，**理由是"契约洁净 + 解 domain 耦合"，不是"性能"**。不要承诺减重渲。

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
1. **配色**（`colors.ts`）：纯函数，按"值 → 标签 → 实体类型"三级优先级返回节点背景色，默认色硬编码 `DEFAULT_*`。
2. **尺寸**（`calcWidthHeight.ts`）：纯估算函数，魔数预先算 `width/height`，喂给 ELK。
3. **CSS 变量**（`FlowStyleManager.tsx`）：只在 `documentElement` 上设一个 `--edge-stroke-width`（配合 `style.css` 的 `svg .react-flow__edge-path`）；其余 nodeWidth/editNodeWidth 那两行被注释掉了——**名不副实**，几乎是空壳。

`colors` / `calcWidthHeight` 都有测试（`colors.test.ts` / `calcWidthHeight.test.ts`），但 `getDsLenAndDesc` / `simpleStrRowCount` 无独立测试。

### 2.3 集成层

4 个 route（`Table`/`TableRef`/`Record`/`RecordRef`）+ 1 个 dragPanel 固定页消费者，都调同一个 `useEntityToGraph` 把 entityMap 推进 xyflow store。每个 route 都 `return null`——它们是"副作用型图形控制器"，真正的画布 `<FlowGraph>` 是它们在 `CfgEditorApp.tsx` 里的兄弟/父节点。

**配置回流**：`NodeShowSetting`/`FlowVisualizationSetting` 调 `setNodeShow` 写 `store.nodeShow` + `clearLayoutCache()`（全局 `removeQueries(['layout'])`），`useEntityToGraph` 把 nodeShow 放进 useMemo 依赖（触发节点重转换）和 layout queryKey（触发 ELK 重布局）。

### 2.4 数据模型与 store 边界

- `domain/entityModel.ts`：`Entity` 判别联合 + `EntityGraph` + `EntitySharedSetting`；并把编辑态↔视图契约 `EFitView`/`EditingObjectRes` 也放在 domain（消除 `flow → routes` 的反向依赖，是正确的"共享内核"）。
- `store.ts`：用 vendored 的 `resso`（基于 `useSyncExternalStore`）做全局 `StoreState`，所有 setter 走"整体赋新对象 + `setPref` 持久化"模式。
- **关键边界**：`resso` store 与 xyflow 内部 store 是**两套独立的 `useSyncExternalStore` 实例**。`store.ts` 的 `StoreState` 里**没有** `setNodes`/`setEdges`/`panZoom`——那些是 xyflow 内部 store 的字段，`useEntityToGraph` 通过 `useStore(s=>s.setNodes)` 直取。
- `services/editingSession.ts`：每会话可变 store（`useSyncExternalStore` 订阅 `structureVersion`），产出 `EditingObjectRes` 喂给 flow 的 layout queryKey 与 viewport 决策。**这是 2026-07 由"方案 C"根治的成熟设计**——值类编辑不 bump（零重渲契约）、结构类编辑 bump，`editingObject` 就地变异是有意设计，**勿改**（详见 memory 与 [3.0 勿动 A4](#a4record-entitymap-的-12-项-usememo-依赖折叠时全量重建)）。

**测试覆盖**：`fillHandles`/`convertNodeAndEdges`/`colors`/`calcWidthHeight`/`Folds`/`editingSession.getEditingObjectRes` 有测试；flow 层的 effect/viewport 数学/`layoutAsync` 竞态**无测试**（CLAUDE.md 约定 vitest 只测纯逻辑）。

---

## 第三部分 · 重构机会

> **判定原则**：active bug（correctness/high）> 零风险清理 + 测试护栏 > 架构根治；同级内按修复成本升序。
> 每条统一格式：**现状**(file:line) / **问题** / **改法**(代码) / **风险**。

### 3.0 先读我：这些"看起来像问题"的，请勿动

> 这 6 条都曾被怀疑为缺陷，深入核实后确认是**合理设计或诊断错误**。重构时若遇到，**不要动**；其中几条与 memory 记录的"方案 C 根治"一致。误改会引入真实回归。

#### A1. 4 个 route "复制"useEntityToGraph 骨架 → 夸大

四个 route 确实都遵循 `entityMap → fillHandles → 菜单 → useEntityToGraph → return null` 模板，但**真正逐字重复的只有 ~3 行**（`fillHandles(entityMap)` + 一行 `useEntityToGraph({...})` + `return null`）。`useEntityToGraph` 的实参四处各异：

- `Table` 只传基础 5 参；
- `TableRef` 加 `nodeDoubleClickFunc`；
- `Record` 传 `editingObjectRes` 并按 `isEditing` 切 `type`；
- `RecordRef` 传 `setFitViewForPathname`/`nodeShow`/`nodeDoubleClickFunc`。

entityMap 构造各用完全不同的 creator（`TableEntityCreator` / `includeRefTables` / `RecordEntityCreator`|`RecordEditEntityCreator` / `createRefEntities`），菜单逻辑也完全不同。`Record.tsx` 还有 ~180 行额外逻辑（useMutation、EditingSession、folds、structureVersion、3 个 useEffect），与 `Table`/`TableRef` 的 ~50 行天差地别，**绝非同构**。`fillHandles` 确为 4 处必调的约定（`entityToNodeAndEdge.ts:26`，纯 mutate 设 handleIn/handleOut，不依赖 route 信息），但这属于 [P1-1 dimensions](#p1-1dimensions-ts-统一宽度魔数-240280) 范畴，不构成"抽公共 hook"的理由。

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

1. **effect deps 是 16 项（非 17）**：`[newNodes, edges, nodeMenuFunc, paneMenu, editingObjectRes, flowGraph, nodeDoubleClickFunc, setNodes, setEdges, id2RectMap, width, height, panZoom, setFitViewForPathname, pathname, getNodesBounds]`。其中 `setNodes`/`setEdges`/`panZoom`/`getNodesBounds` 是 xyflow 内部 store 的**稳定引用**，`flowGraph` 是 memo 过的 context——**真正会变的只有 6-8 项**（数据/缓存/编辑态/resize/菜单回调）。长 deps 数组≠过度触发。

2. **受控/非受控混用不是 bug**（见 [第一部分 §4](#4-受控-vs-非受控本项目是混用)）：xyflow 完全支持 `defaultNodes` + 命令式 `setNodes`。

3. **手算 viewport 不能简单替换**：FitFull 分支**可** fitView 化（[P2-1](#p2-1-useentitytograph-命令面收口-useractflow--fitfull-fitview-化)），但 FitId 分支（`useEntityToGraph.ts:142-154`）的"relayout 后锚点屏幕坐标不变"方程 fitView **做不到**，必须自算。

4. **FlowGraph.tsx:85 的 `// fitView` 注释悬空**——确实是死注释，可清理，但不是 bug。

> **结论**：effect 本身不是问题。可做的改进是 [P2-1]（收口 useReactFlow + FitFull fitView 化），把 `panZoom` 这类内部字段换成公开 API——但这是**稳定性/可读性**改进，不是修 bug。

---

### 3.1 P0 — 立即修复

#### P0-1. NoteEdit onSuccess 在业务失败分支仍关闭编辑器，丢弃用户输入

**现状**（`NoteShowOrEdit.tsx:53-70`）：`onSuccess` 把 `setIsEdit(false)` 放在 if/else 之后**无条件执行**：

```ts
onSuccess: (editResult, variables) => {
  const { resultCode, notes } = editResult;
  if (resultCode == 'updateOk' || resultCode == 'addOk' || resultCode == 'deleteOk') {
    // ... queryClient.setQueryData(['notes'], notesToMap(notes));
  } else {
    notification.warning({ ... });
  }
  setIsEdit(false);   // ⚠️ 第 69 行：warning 分支也执行
}
```

**问题**：本仓 `updateNote`（`api/api.ts`）是"HTTP 200 + resultCode 业务状态码"模式：transport 失败（网络/4xx/5xx）走 `onError`；业务逻辑失败（`storeErr`/`keyNotSet`/`keyNotFoundOnDelete`，HTTP 200 带非 OK resultCode）走 `onSuccess`。`onError` 已正确注释"保留 newNote 便于重试"，但 `onSuccess` 的业务失败分支照样 `setIsEdit(false)` → 卸载 `NoteEdit` → `newNote` 局部 state 销毁 → 用户输入丢失，与 `onError` 设计意图自相矛盾。

**改法**（1 行）：把 `setIsEdit(false)` 移进 OK 分支末尾。

```ts
onSuccess: (editResult, variables) => {
  const { resultCode, notes } = editResult;
  if (resultCode === 'updateOk' || resultCode === 'addOk' || resultCode === 'deleteOk') {
    notification.info({ ... });
    queryClient.setQueryData(['notes'], notesToMap(notes));
    setIsEdit(false);                 // ✅ 仅真成功才关闭
  } else {
    notification.warning({ ... });
    // 保留编辑框与 newNote，与 onError 一致
  }
}
```

**风险**：极低。需确认三类非 OK（`storeErr`/`keyNotSet`/`keyNotFoundOnDelete`）均应保持打开——从 onError 注释与 warning 语气判断是默认正确。建议手测：mock 服务端返回 `resultCode='storeErr'`，确认编辑框保留输入。可选附带：抽 `isNoteOk(code)` 谓词到 `noteModel.ts`，统一本仓 `Record`/`Operations`/`AddJson` 的同类判定。

---

#### P0-2. FlowNode 背景色 memo 的 stale 陷阱

**现状**（`FlowNode.tsx:67`）：

```ts
const color: string = useMemo(() => getNodeBackgroundColor(entity), [entity]);
// getNodeBackgroundColor 内部读 entity.sharedSetting.nodeShow.xxx（colors.ts:13-38）
```

**问题**：这是一个绝佳的"`memo` + 深读 deps + 就地 mutate"教学案例。stale 链路：

1. `convertNodeAndEdges`（`entityToNodeAndEdge.ts:70`）`entity.sharedSetting = sharedSetting` **就地 mutate**，entity 引用不变。
2. 浏览态（如 `Table.tsx:28-35`）`entityMap` 的 useMemo deps 是 `[schema, curTable, maxImpl]`——**不含 nodeShow**。所以改 nodeShow 颜色时，entityMap 不重建，entity 引用不变。
3. `useEntityToGraph.ts:94` 的 `convertNodeAndEdges` useMemo deps 含 `nodeShowSetting` → 重算 → 产出**新 node 对象**，但 `node.data.entity` 仍是**同一个引用**（entityMap 没变）。
4. `FlowNode`（`memo`）看到 `node.data` 是新引用 → 放行重渲；但内部 `color` 的 useMemo deps `[entity]` 没变 → **color 不重算** → 改主题色后节点背景 stale。

**对照反例**：同组件 `nodeStyle`（`:69-71`）把 `editFoldColor` 深读到 deps → 没 stale。证明 `[entity]` 是漏网，可用作自检信号。

**改法**（止血档，1 行）：deps 改 `[entity, nodeShow]`，`getNodeBackgroundColor` 增 `nodeShow` 入参。

```ts
// colors.ts —— 对齐已有的 getFieldBackgroundColor(field, nodeShow?) 风格
export function getNodeBackgroundColor(entity: Entity, nodeShow?: NodeShowType): string {
  // 原来读 entity.sharedSetting?.nodeShow，改为直接用入参 nodeShow
}

// FlowNode.tsx:67
const nodeShow = entity.sharedSetting?.nodeShow;
const color = useMemo(() => getNodeBackgroundColor(entity, nodeShow), [entity, nodeShow]);
```

**风险**：低。`getNodeBackgroundColor` 全项目调用点（`FlowNode.tsx:67`）需同步加参数；跑 `colors.test.ts`。根治方向见 [big rock 1](#big-rock-1nodeshow-下发去- entitysharedsetting-mutate)。

---

#### P0-3. layoutAsync 失败返回 undefined，导致偶发空图

**现状**（`layoutAsync.ts:95-107`）：

```ts
const { children } = await elk.layout(graph);
if (children) {
  toPositionMap(id2RectMap, children);
  if (!allPositionXYOk(nodes, id2RectMap)) {
    console.log('layout ignored', nodes);
    return;                  // ⚠️ 显式 return undefined
  }
  return id2RectMap;
} else {
  console.log('layout children null');   // ⚠️ 隐式 return undefined
}
```

**问题**：`undefined` 是 react-query v4+ 的**非法缓存值**——返回 undefined 会 `isSuccess=true` 但 `data=undefined`，彻底打破下游 `if (data)`，且不进 retry/error 通道。`useEntityToGraph` 只解构 `data` 丢弃 `error`，`newNodes` 在 undefined 时为 undefined，effect 的 `if (newNodes)` 守卫让 `setNodes`/viewport 整体跳过 → **偶发空/旧图且零反馈**。当 ELK 因竞态丢弃结果（`allPositionXYOk` 失败）或返回 null 时，用户看到的是一张没节点的画布，控制台只有一句 `console.log`。

**改法**：失败分支改 `throw`，加可选 `signal` 形参；`useEntityToGraph` 解构 `error`/`isPending` 并 fallback。

```ts
// layoutAsync.ts
export async function layoutAsync(nodes, edges, strategy, nodeShow?, signal?: AbortSignal) {
  // ...
  if (signal?.aborted) throw new LayoutError('aborted');
  const { children } = await elk.layout(graph);
  if (!children) throw new LayoutError('no_children');
  toPositionMap(id2RectMap, children);
  if (!allPositionXYOk(nodes, id2RectMap)) throw new LayoutError('dropped_nodes');
  return id2RectMap;
}

// useEntityToGraph.ts —— 解构 error，保证非空
const { data: id2RectMap, error, isPending } = useQuery({ queryKey, queryFn: ctx => layoutAsync(nodes, edges, strategy, nodeShowSetting, ctx.signal), staleTime });
// error 时 setNodes(未布局占位) + console.error，保证画布非空
```

**风险**：中。这是 setNodeShow queryKey 收窄（[P2-3](#p2-3-setnodeshow-缓存策略升级依赖-p0-3-先修)）的**前置依赖**——必须先修这条，否则 undefined 污染更易暴露。配 `layoutAsync.test.ts`（`vi.mock` ELK）覆盖三个 throw 分支。

---

### 3.2 P1 — 高 ROI quick wins

#### P1-1. dimensions.ts 统一宽度魔数 240/280

**现状**：同一对默认值（240 非 edit / 280 edit）+ 判定逻辑散落 4 处：
- `calcWidthHeight.ts:10`（ELK 边界框）
- `FlowNode.tsx:68`（节点 CSS width）
- `EntityProperties.tsx:44`（readOnly 右侧 Handle 的 `left` 定位）
- `EntityForm.tsx:665`（edit 右侧 Handle 定位，**无 `??` 兜底**，靠 `:198` 的 `DEFAULT_NODE_WIDTH=280` 深层兜住）

另有 `store.ts:118-119`、`fixtures.ts:24/35` 也是同值。节点宽度在 xyflow 里**同时驱动 3 件事**（ELK 边界框 / CSS 渲染宽度 / Handle 绝对定位），必须来自同一数字源。

**改法**：新建 `src/flow/dimensions.ts` 导出纯函数 + 常量，4 处改 1 行委托。

```ts
// src/flow/dimensions.ts
export const DEFAULT_NODE_WIDTH = 240;
export const DEFAULT_EDIT_NODE_WIDTH = 280;
export function getNodeWidth(entity: Entity): number { /* isEditableEntity ? editNodeWidth ?? 280 : nodeWidth ?? 240 */ }
export function getReadNodeWidth(ss?: EntitySharedSetting): number { /* nodeWidth ?? 240 */ }
export function getEditNodeWidth(ss?: EntitySharedSetting): number { /* editNodeWidth ?? 280 */ }
```

**风险**：低。⚠️ **不要**统一 `EntityForm` 的 `-10` 与 `EntityProperties` 的 `-2` Handle 偏移——那是各自视觉细节。⚠️ **不要**改用 `node.measured`（与 ELK Worker 需预算尺寸的架构相悖）。配 `dimensions.test.ts` 4 组合矩阵（edit×缺省/覆盖、非 edit×缺省/覆盖）。

---

#### P1-2. calcWidthHeight 魔数常量化 + dev 实测对账护栏

**现状**（`calcWidthHeight.ts:11-50`）：一批裸魔数估高度（`41 * fields.length`、`48 + title?32:0 + showDsLen*38 + desc?22*rowCount + image?200`、`20 + 40*cnt + extra`…），喂给 ELK 当边界框，与真实 DOM 尺寸无任何对账。

**改法**（递减三步，不动主链路）：

1. **零风险常量化**（先做）：魔数抽带注释常量并标 DOM 对应结构。`calcWidthHeight.test.ts` 已锁算术，天然护航。
   ```ts
   const FIELD_ROW_H = 41;   // antd List size='small' List.Item 实测行高
   const CARD_BASE = 48; const CARD_TITLE_H = 32; const CARD_DS_H = 38;
   const DESC_ROW_H = 22; const IMAGE_H = 200;
   const EDIT_BASE = 20; const EDIT_ROW_H = 40; const EDIT_FOLD_H = 16;
   const NOTE_ROW_H = 22; const NOTE_WRAP_COLS = 15;
   ```

2. **dev-only 实测对账护栏**（必做）：`FlowNode` 挂载后读 `nodeProps.measured?.height` 与估算比，偏差 `>8px & >5%` 时 `console.warn`（按 entity.id 去重）。**不重排、不闪烁**，把"静默漂移"变"可观测告警"。
   ```ts
   // FlowNode.tsx —— dev-only，模块级 warned Set 去重
   useEffect(() => {
     if (import.meta.env.DEV && nodeProps.measured?.height) {
       const [, est] = calcWidthHeight(entity);
       const drift = Math.abs(nodeProps.measured.height - est);
       if (drift > 8 && drift / est > 0.05 && !warned.has(id)) {
         warned.add(id);
         console.warn(`[flow] node ${id} height drift: est=${est} measured=${nodeProps.measured.height} Δ=${drift}px`);
       }
     }
   }, [id, entity, nodeProps.measured?.height]);
   ```

3. **条件触发**（仅护栏持续报警才做）：对报警集中的类型调常量；系统性漂移才考虑 measured 重排（[big rock 5](#big-rock-6calcwidthheight-measured-重排仅护栏持续报警才上)）。

**风险**：低。⚠️ `width` 两端同源（`FlowNode` 与 `calcWidthHeight` 都读 `nodeShow.nodeWidth/editNodeWidth`）不会漂，**护栏只针对 height**。

---

#### P1-3. viewportMath.ts 抽纯函数 + 不变量测试

**现状**（`useEntityToGraph.ts:142-154`）：FitId 的"锚点不动"方程藏在 effect 里，只有注释，无测试。

**改法**：抽纯函数 + 不变量测试，把"锚点不动"从注释提升为可执行契约。

```ts
// src/flow/viewportMath.ts
export type Viewport = { x: number; y: number; zoom: number };
export function screenOf(world: { x: number; y: number }, vp: Viewport) {
  return { x: world.x * vp.zoom + vp.x, y: world.y * vp.zoom + vp.y };
}
// relayout 后让 anchorNew 的屏幕坐标等于 anchorOld 在旧 vp 下的屏幕坐标
export function computeStableViewport(anchorOld: { x: number; y: number }, anchorNew: { x: number; y: number }, vp: Viewport): Viewport {
  return {
    zoom: vp.zoom,
    x: anchorOld.x * vp.zoom - anchorNew.x * vp.zoom + vp.x,
    y: anchorOld.y * vp.zoom - anchorNew.y * vp.zoom + vp.y,
  };
}
```

```ts
// viewportMath.test.ts —— 不变量：relayout 前后锚点屏幕坐标相等
expect(screenOf(anchorNew, computeStableViewport(anchorOld, anchorNew, vp))).toEqual(screenOf(anchorOld, vp));
```

**风险**：零行为改动。这是 flow 层少数能加纯函数测试的地方（CLAUDE.md 约定只测纯逻辑），ROI 极高。

---

#### P1-4. getDsLenAndDesc 5 路矩阵测试 + ±1 契约

**现状**（`getDsLenAndDesc.ts`）：纯函数，4 路 switch（`show`/`showFallbackValue`/`showValue`/`none`）+ `nodeShow=undefined` 路径，被布局（`calcWidthHeight`）和渲染（`EntityCard`）**双重消费**，却无独立测试。

**关键反直觉点**：`show` 的 `showDsLen = ds.length - 1`（末条当 desc），`showValue` 的 `showDsLen = ds.length`（含全部，desc 取 `brief.value`）——**同变量名不同含义，恰差 1**，最易在重构中误改。

**改法**：新增 `getDsLenAndDesc.test.ts`，5 路 × ds 形态矩阵 + 显式 ±1 断言。`±1` 错误会同时导致（a）布局高度偏差 38px（节点 overlap/留白）+（b）EntityCard 多/少渲染一条 description，叠加后肉眼难归因。

**风险**：仅新增测试，零回归。⚠️ 写测试前确认 `show` + ds 单条时 `showDsLen=0`（唯一条降级为纯 desc）是产品预期——从 `EntityCard` 的切片逻辑看是有意设计。

---

#### P1-5. colors.ts NODE_SHOW_DEFAULTS 统一 + 测试解耦

**现状**：`DEFAULT_NODE_COLOR` 等 6 个默认色在 `colors.ts:42-46`、`store.ts:126-130`、`fixtures.ts`、`colorUtils.ts` 重复 4 处；且 `colors.test.ts`/`entityToNodeAndEdge.test.ts` 把 hex **硬断言**进单测，改一处破多处。

**改法**：`colors.ts` 导出 `NODE_SHOW_DEFAULTS` 常量对象，其余 3 处改 spread/引用；测试断言改 `.toBe(NODE_SHOW_DEFAULTS.nodeColor)` 解除字面 hex 耦合。

**风险**：低。⚠️ 若未来要让默认色跟随 antd 主题，纯函数（`colors.ts`）无 hook 上下文不能用 `useToken`——需在调用点（`FlowNode`）`useToken` 后传入，`colors.ts` 顶部加注释声明"DEFAULT_* 与 antd token 刻意解耦"。

---

#### P1-6. CustomAutoComplete 删冗余事件绑定

**现状**（`CustomAutoComplete.tsx:29-34`）：

```tsx
<AutoComplete ... value={value} onChange={onChange} onSelect={onChange} showSearch={{onSearch: onChange}}>
```

**问题**：`AutoComplete` 的 `onChange` 在"输入"和"选中"都触发（受控值回调契约），已覆盖全部情形。`onSelect` + `showSearch.onSearch` 都别名到 `onChange` → **每输入一次/选中一次，`editOnUpdateValues → session.updateFormValues` 被调 2 次**，每次白跑一遍 schema 查找 + 转换器查找 + `notifyEditingState`。

**改法**：只保留 `onChange`，删 `onSelect={onChange}` 和 `showSearch={{onSearch: onChange}}`，`{...filters}` 原样透传。

**风险**：极低。`updateFormValues` 对相同值幂等，下游无人区分触发次数。手测带 AutoComplete 的节点（`options.length>0` 且非 `isEnum`）的 `alt+s` 提交流程，确认 HeaderBar 脏标记只刷新一次。

---

#### P1-7. VideoAudioSyncer blob URL lifecycle（内存泄漏）

**现状**（`ResPopover.tsx:25-27`）：`getSrt2VttUrls` 里 `new Blob([vtt])` + `URL.createObjectURL`，返回的 url 喂给 `useQuery({queryKey:['vtt', path]})` 缓存，再作为 `<track src={url}>`。全仓 `revokeObjectURL` **0 处** → blob URL 永不释放，浏览器 blob store 驻留到 document unload。

**改法**：queryFn 改返回 VTT **文本数组**（纯字符串，可正常缓存/GC），`createObjectURL`/`revokeObjectURL` 下沉到 `VideoAudioSyncer` 的 `useMemo`+`useEffect` 生命周期（bump queryKey 到 `['vtt2', path]` 避免旧 url 缓存被当文本）。

```tsx
const vttTexts = useQuery({ queryKey: ['vtt2', resInfo.path], queryFn: () => getSrt2Vtts(resInfo) });
const vttUrls = useMemo(() => vttTexts?.map(t => URL.createObjectURL(new Blob([t], { type: 'text/vtt' }))), [vttTexts]);
useEffect(() => { if (!vttUrls) return; return () => vttUrls.forEach(u => URL.revokeObjectURL(u)); }, [vttUrls]);
```

**风险**：中。⚠️ `useMemo` 里 `createObjectURL` 是带副作用操作，React 严格模式/并发渲染下理论上可能 orphan blob（依赖稳定时风险低）；若团队严格遵循 React 规范，改用 `useEffect`+`useState`。备选：`data:text/vtt;charset=utf-8,${encodeURIComponent(vtt)}` 绕开（小字幕适用，大字幕有 base64 膨胀）。

---

### 3.3 P2 — 中等改动

#### P2-1. useEntityToGraph 命令面收口 useReactFlow + FitFull fitView 化

**现状**：`useEntityToGraph.ts:86-92` 混用 `useStore`（取 `setNodes`/`setEdges`/`panZoom`/`width`/`height`）与 `useReactFlow`（取 `getNodesBounds`），见 [第一部分 §5](#5-useractflow公开稳定vs-usestore内部-escape-hatch)。

**改法**：
- `setNodes`/`setEdges` 从 `useReactFlow()` 取；
- `panZoom` 整段移除——**FitFull 改 `fitView({ padding: 0.2, minZoom: 0.3, maxZoom: 1 })`**，FitId 保留数学（用 [P1-3] 的 `computeStableViewport`），只把 `panZoom.setViewport` 换成 `useReactFlow().setViewport`；
- `panZoom && x` 守卫改 `viewportInitialized && x`（或 `useReactFlow` 的等价）。
- effect deps 从 ~16 项降到 ~12 项。

**风险**：需人工对比 FitFull 视觉（`padding`/`minZoom`/`maxZoom` 与原 `getViewportForBounds` 对齐）。`fitView` 返回 `Promise`、旧 `setViewport` 同步无返回——现有 fire-and-forget 不出错，但语义微调。同文件同 effect 可合并一个 PR。

---

#### P2-2. setNodeShow 缓存策略升级（依赖 P0-3 先修）

**现状**（`store.ts:451-455`）：`setNodeShow` **无条件** `clearLayoutCache`，但拓扑相关 setting 已在 queryKey、**纯颜色字段变更不应重跑 ELK**。固定页拿 `fix.nodeShow` 快照本可命中缓存，却被 `removeQueries` 强制清掉。`setQuery`（`:272-277`）已建立"非布局字段不清缓存"先例，`setNodeShow` 是漏网。

**改法**：
- `setNodeShow` 内 diff 布局相关字段（定义 `NODESHOW_LAYOUT_KEYS`）才 `clearLayoutCache`；
- `useEntityToGraph` queryKey 收窄到 `pick(nodeShowSetting, LAYOUT_KEYS)`。

**风险**：⚠️ **必须先修 P0-3**（layoutAsync throw），否则 undefined 污染更易暴露。⚠️ `'e'` 标记位置必须保持在 pathname 之后同一层级，否则破坏 `Record.tsx:87` 的 `['layout', pathname, 'e']` prefix 精确失效契约。

---

#### P2-3. simpleStrRowCount East Asian Width + grapheme（CJK 行数估算）

**现状**（`calcWidthHeight.ts:100-121`）：`code > 255 ? len+=2 : len++` + 固定 30 字符换行。`>255` 把 Latin Extended/Cyrillic/emoji 代理对（高代理 `0xD800+` 也 `>255`，单个 emoji 算 `len+4`）一律按双宽，与真实渲染不符。固定 30 与 `nodeWidth` 解耦。无直接测试，30 字符换行分支是死代码。同根缺陷：`:41` `note.length/15`、`:78` `value.length/10`。

**改法**：`Intl.Segmenter('en',{granularity:'grapheme'})` 切 grapheme + East Asian Width 表查（CJK Unified/Hiragana/Katakana/Hangul/Fullwidth Forms 等范围才算 2）；`charsPerRow` 从 `nodeWidth` 派生；补 4 个针对性测试（ASCII 30+、纯中文 15+、emoji、`\n`）。

**风险**：改宽度启发式会让 CJK desc 高度轻微漂移（约 ±1 行/22px），PR 描述说明并跑真实 schema 截图对比。⚠️ `Intl.Segmenter` 在 Web Worker（`layoutAsync` 调用 `calcWidthHeight`）内需确认可用（现代 Tauri webview 都支持）。本仓是中文编辑器，desc 主体 CJK，`>255` 对 CJK 大致对，severity 实为 low-medium，明显翻车只在 emoji/自定义 nodeWidth。

---

#### P2-4. FlowNode 95/99/103 拆行 + NoteShowOrEdit 常量上移

**现状**：
- `FlowNode.tsx:95/99/103` 三处 `}, [deps]); const X = use…(() => {`——hook 收尾与下一 hook 声明挤同一物理行（项目无 prettier，纯手动压缩）。`git blame` 单行覆盖两个逻辑单元，断点粒度变粗，review 易漏 deps。
- `NoteShowOrEdit.tsx:88/89` 使用 `autoSize`/`textAreaStyle`，但二者声明在 `:109/110`（使用之后）。运行期不触发 TDZ（组件函数模块求值后才调用），但违反直觉，且文件顶部 `:12-14` 已有半截常量区。

**改法**：纯零风险格式/组织修复。FlowNode 三处拆行（与同文件其余 11 个 hook 风格一致）；NoteShowOrEdit 常量上移顶部 + 改 SCREAMING_SNAKE_CASE（顺带暴露与 `EntityForm.tsx:61` `TEXT_AREA_AUTO_SIZE` 的重复常量）。

**风险**：零功能风险。可选加固：CI 接入 `prettier --check` 或 biome format，让手动压缩在提交时被拦住。

---

#### P2-5. Highlight 抽出 src/flow/Highlight.tsx

**现状**：`Highlight`（`EntityCard.tsx:24-33`，纯文本高亮 + 正则转义）被 `FlowNode.tsx:6`、`EntityProperties.tsx:6`、`EntityCard.tsx` 自身三处 import 自 `EntityCard`。通用展示组件寄居领域专用卡片文件——cohesion 问题（无循环依赖，非方向问题）。

**改法**：新建 `src/flow/Highlight.tsx`，把 `escapeRegExp` + `Highlight` 搬入（`escapeRegExp` 保持私有），三处 import 改路径。⚠️ 不放 `src/components/`（该目录不存在，为单组件新建分层是过度组织），就近放 `src/flow/`。

**风险**：纯机械搬移，TS 编译立即捕获遗漏 import。

---

#### P2-6. mayHaveResOrNote 谓词提取

**现状**：`label.includes('_')`（record 类实体的隐式类型标记，label 形如 `表名_记录ID`）在 `FlowNode.tsx:137/157`、`calcWidthHeight.ts:38` 等约 4 处复制，无注释。

**改法**：抽 `mayHaveResOrNote(label)` 谓词到 `domain/entityPredicates.ts` 并加注释说明 record label 约定。

**风险**：零。

---

#### P2-7. EntityForm useSyncFieldValue hook 抽取

**现状**：`EntityForm.tsx` 的 `PrimitiveFormItem`(`:439-441`)、`ArrayOfPrimitiveFormItem`(`:479-481`)、`InterfaceFormItem`(`:617-619`) 各自重复同一段 `useEffect(() => form.setFieldValue(field.name, field.value), [..])`。注释（应对 antd `initialValue` 仅 mount 生效、key 复用时表单值不刷新，引 ant-design#56102）只贴在 `InterfaceFormItem` 一处。

**改法**：抽 `useSyncFieldValue(form, name, value)` hook，把 #56102 解释并入 JSDoc 作单一真相源，三处改一行调用。

```ts
/** 同步外部 field.value 到 antd Form 字段。Form.Item 的 initialValue 仅在字段首次注册时生效；
 *  父级 key={field.name} 复用实例时新 initialValue 被忽略 → 表单显示旧值。此 effect 命令式同步。
 *  参考：https://github.com/ant-design/ant-design/issues/56102 */
function useSyncFieldValue(form: FormInstance, name: string, value: unknown) {
  useEffect(() => { form.setFieldValue(name, value); }, [name, value, form]);
}
```

**风险**：低。可选根治（单独 PR）：`renderFieldItem` 的 key 改 `${entity.id}:${field.name}` 强制 entity 切换时重挂载，三处 effect 可整体删除——但需回归 focus 保留/`alt+s`/impl 切换，风险更高。

---

#### P2-8. EntityProperties text()/tooltip() 改名 + 提常量

**现状**（`EntityProperties.tsx:14-22`）：`text({comment,name})` 里 `comment.substring(0,6)` 是无注释魔数；`text`/`tooltip` 与 antd `Typography.Text` 词法同根易眼花；`re=/[（()]/` 无注释。

**改法**：提 `LABEL_COMMENT_PREFIX_MAX_LEN=6` 常量，`text→buildFieldLabel`/`tooltip→buildFieldTooltip`，`re` 加注释。⚠️ **纠正两处误判**：(a) 注释没被"静默丢一半"——`tooltip` 保留完整 `${name}: ${comment}`；(b) 不需要"按节点宽度自适应"——外层 `<Text ellipsis maxWidth:80%>` 已经做了，数据层预截断 + 视图层 CSS ellipsis 是合理双保险，让数据层读 DOM 宽度会陷入 measure cycle。

**风险**：零。改名校验仅本文件 `:61/:63` 两处调用。

---

### 3.4 P3+ — 架构级 big rocks

> 需独立 PR，遵守依赖顺序。详见 [第四部分](#第四部分--路线图与依赖)。

#### Big Rock 1. nodeShow 下发，去 entity.sharedSetting mutate（B + C + F + P0-2 的共同根因）

**现状**：`EntityBase.sharedSetting`（`entityModel.ts:286`）让 domain Entity 携带 `NodeShowType`（UI 配置）——domain↔presentation 耦合。`useEntityToGraph.ts:96` 构造一次 `sharedSetting`，`entityToNodeAndEdge.ts:70` 在循环里把**同一引用盖章**到每个 entity（mutate 入参）。渲染层遍布 `entity.sharedSetting?.nodeShow?.xxx` 两层可选链（`FlowNode`/`EntityProperties`/`EntityForm`/`EntityCard`/`colors`）。而 `nodeShow`/`query` 本就在 resso store——等于 `store → entity.sharedSetting → prop → ?. ` 一圈空转。

这是 **4 条 finding 的共同根因**：B（convertNodeAndEdges 副作用 mutate）、C（memo 纪律）、F（nodeShow 透传）、P0-2（color stale）。

**改法**（分两档）：

- **轻档（推荐先做，零结构风险）**：把解析后的 nodeShow 切片直接放进 ReactFlow node 的 `data`，`FlowNode` 读 `nodeProps.data.nodeShow`（解析好的、含 per-graph override）而非 `entity.sharedSetting?.nodeShow?.`。子组件形参从 `sharedSetting?` 改 `nodeShow?`。`colors.ts` `getNodeBackgroundColor` 改 `(entity, nodeShow)` 签名。`query` 可直接 `useMyStore()` 读（无 per-graph override，`setQuery` 明确不进 layout）。
- **重档（可选根治）**：新增 `SharedSettingContext`，**Provider 必须放在 `ReactFlowProvider` 内、`<ReactFlow>` 之前把 ReactFlow 一起包住**（⚠️ 现 `FlowGraphContext.tsx:100-102` 只包 `{children}` 不包 ReactFlow，FlowNode 读不到）。`value` 要 `useMemo` 稳定引用。

**无论哪档**：删 `entityToNodeAndEdge.ts:70` 的 mutate，entity 保持不可变 memo-safe；同步改 `entityToNodeAndEdge.test.ts:123-135` 的"回写实体"断言。

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

**风险**：⚠️ 需 P0-3 已修（否则 undefined 污染更易暴露）；⚠️ `'e'` 标记层级不变以保 `Record.tsx:87` prefix 语义。可选最小方案：先审计去冗余（`setFixedPagesConf` 的 `clearLayoutCache` 嫌疑最大，pageConf 不直接改当前 layout 输入）。

---

#### Big Rock 5. 菜单变 ReactFlow props，删 FlowGraphContext（可选）

**现状**：`useEntityToGraph.ts:121-162` 把"注册菜单回调"+"推 nodes/edges"+"算 viewport"三件不相关的事塞进**同一 useEffect 同一 `if(newNodes)` 守卫**——菜单被布局就绪状态错误门控，初次挂载/路由切换的 layout 拉取期右键无菜单。`FlowGraphContext` 是项目自创的"child→parent 反向注册"模式（xyflow 官方 context-menu 示例是直接把 `onNodeContextMenu` 作为稳定 props 传给 `ReactFlow`）。

**改法**：
- **方案 A（最小修复）**：拆菜单 effect 到独立 + 路由 unmount 清理（cleanup 里 `setNodes([])`/`setEdges([])`）。
- **方案 B（根治，可选）**：删 `FlowGraphContext` 反向注册，菜单变 `ReactFlow` props（`onNodeContextMenu` 等），各 route 用 `useCallback` 稳定函数直接传。

**风险**：⚠️ naive 拆分会 worse——B 布局拉取期内菜单已是 B 的、可见节点仍是 A 的，B 菜单函数作用在 A 节点上。正确最小修复是拆分 + unmount 清理。方案 B 涉及 4 个 route + `FlowGraph` 签名变更，回归面广，建议方案 A 先行。

---

#### Big Rock 6. calcWidthHeight measured 重排（仅护栏持续报警才上）

**现状**：作者 `calcWidthHeight.ts:6-7` 已书面弃案（measured 重排导致闪烁与代码绕晕）。

**改法**：**仅当 [P1-2] 的 dev 实测对账护栏显示系统性漂移**（多种实体类型都偏）才考虑 measured 驱动重排，且要走 React Query `staleTime` 缓存避免每次重算。优先调常量（[P1-2] 步骤 3）不重排。

**风险**：高。与作者已解决的闪烁问题直接冲突。优先级最低。

---

## 第四部分 · 路线图与依赖

### 依赖顺序硬约束

```
P0-3 (layoutAsync throw)
  └─► 必须先于 P2-2 (setNodeShow queryKey 收窄)、Big Rock 4 (store queryKey)

Big Rock 1 (nodeShow 下发 + 删 entity.sharedSetting mutate)
  └─► 同时解决 B + C + F + P0-2（同根）

P2-1 (useEntityToGraph 收口 useReactFlow + FitFull fitView)
  └─► 同文件同 effect，可与 P1-3 (viewportMath) 合并一个 PR

P1-2 (calcWidthHeight 常量化 + 护栏)
  └─► 是 Big Rock 6 (measured 重排) 的前提
```

### Quick Wins checklist（零/低风险，可随任意 PR 捎带）

- [ ] **P0-1** NoteEdit `setIsEdit(false)` 移进 OK 分支（1 行，防笔记丢失）
- [ ] **P0-2** FlowNode color memo deps 加 nodeShow（1 行，修主题色 stale）
- [ ] **P1-6** CustomAutoComplete 删 `onSelect`/`onSearch` 冗余绑定
- [ ] **P2-4** FlowNode 95/99/103 拆行（纯格式零风险）
- [ ] **P2-4** NoteShowOrEdit `autoSize`/`textAreaStyle` 上移 + 改名
- [ ] **P2-5** Highlight 抽到 `src/flow/Highlight.tsx`
- [ ] **P2-6** `mayHaveResOrNote` 谓词提取到 `domain/entityPredicates.ts`
- [ ] **P2-7** EntityForm `useSyncFieldValue` hook 抽取
- [ ] **P1-1** dimensions.ts 统一 240/280 魔数 + 矩阵测试
- [ ] **P1-4** getDsLenAndDesc 5 路 × ds 形态矩阵测试 + ±1 契约
- [ ] **P1-3** viewportMath.ts 抽 `computeStableViewport` + 不变量测试
- [ ] **P1-5** colors.ts `NODE_SHOW_DEFAULTS` 统一 + 测试解耦字面 hex
- [ ] **P2-8** EntityProperties `text`/`tooltip` 改名 + 提常量
- [ ] **P1-2** calcWidthHeight 魔数常量化 + dev-only 实测对账护栏
- [ ] **Big Rock 2** FlowStyleManager 提升到 CfgEditorApp 顶层 + CSS 变量竞争修复
- [ ] **P0-3** layoutAsync `throw` + `signal` + `vi.mock` 测试（改动聚焦）

### Big Rocks PR 规划（架构级，独立 PR）

| PR | 内容 | 依赖 | 回归面 |
|---|---|---|---|
| BR-1 | nodeShow 轻档下发（node.data 切片）+ 删 entity.sharedSetting mutate | — | 所有节点渲染、改主题色、FixedPage |
| BR-2 | useEntityToGraph 收口 useReactFlow + FitFull fitView + viewportMath | P1-3 | FitFull 视觉、路由切换 |
| BR-3 | setNodeShow queryKey 收窄 | P0-3 | 改 nodeShow 后的布局缓存 |
| BR-4 | FlowStyleManager 提升 + CSS 变量竞争 | — | 多画布并存场景 |
| BR-5 | ActiveMediaPlayerContext + Popover destroyTooltipOnHide | — | 视频节点播放 |
| BR-6 | store queryKey 纳入拓扑 setting | P0-3 | 所有 setter 的缓存行为 |
| BR-7（可选）| 菜单变 ReactFlow props，删 FlowGraphContext（方案 A 先行）| — | 右键菜单、路由切换 |

### 验证策略

本项目 vitest **只测纯逻辑不测 UI**（CLAUDE.md 约定），所以每条改动要么**配纯函数测试**（`viewportMath`/`getDsLenAndDesc`/`dimensions`/`layoutAsync`/`colors`），要么**手测关键路径**：

- 改主题色（`NodeShowSetting`）→ 节点背景实时刷新（验 P0-2 / BR-1）
- mock `resultCode='storeErr'` → NoteEdit 保留输入（验 P0-1）
- `alt+s` 提交、带 AutoComplete 节点选中（验 P1-6 / P2-7）
- FitFull 路由切换视觉对比（验 BR-2）
- 多画布并存（Splitter + 固定页）样式与播放互斥（验 BR-4 / BR-5）
- 改 `nodeShow` 颜色字段 → 不应触发 ELK 重布局（验 BR-3）

进度追踪建议：把 Quick Wins checklist 做成 issue/milestone，每条标注所属 finding、改动文件、预估行数、是否需手测。

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
| **useReactFlow vs useStore** | 公开稳定 instance vs 内部 escape-hatch（panZoom 非公开）| `useEntityToGraph.ts:86-92` |
| **Viewport 数学** | `screen = world*zoom + t`，线性变换公开契约 | `useEntityToGraph.ts:147-154` |
| **视口三档** | 手算 getNodesBounds+getViewportForBounds / fitView / setCenter | `useEntityToGraph.ts:133-135` |
| **声明 vs measured 尺寸** | `node.width/height`（喂 ELK）vs `node.measured.{w,h}`（实测）| 本项目只走声明（`calcWidthHeight.ts:6-7`）|
| **ElkNode 边界框** | ELK 把 `{w,h}` 当不可压缩框，不测 DOM | `layoutAsync.ts:14-18` |
| **ELK Web Worker + 竞态** | workerUrl 模式不阻塞主线程；异步需守卫/AbortSignal | `layoutAsync.ts:11,95-107` |
| **React Query 缓存布局** | queryKey 是输入指纹；removeQueries vs invalidateQueries 时序 | `useEntityToGraph.ts:104-111`；`Record.tsx:87` |
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

*本文档由通读 `src/flow` 全部源码 + 多 agent 对每条重构机会做 xyflow v12 文档对齐验证后综合而成。第三部分每条 finding 均经过"读源码确认现状 → 查官方文档对齐惯用法 → 评估风险"三步核实；[3.0 勿动清单](#30-先读我这些看起来像问题的请勿动) 是同样严格核实后被否决的"伪问题"，专门列出以防后续重构者误伤。*


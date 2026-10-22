# 05 Flow 图层（XYFlow 集成）

Flow 图层把 04 算出的 `nodes` / `edges` 画到屏幕，提供节点 UI 与交互。核心是 `FlowGraph` 集成 `@xyflow/react`，`FlowNode` 按实体类型分派渲染。

Schema 图沿用字段列表与关系线：集合类型旁标注服务端 `index`，内部引用增添只读的来源表达式、目标路径和 `resultType`。外部表保留原有关系线并注明唯一键；祖先目标只连接已显示的类型入口；`::` 从根出发的说明不为复用 struct 猜测某张根表。展示仅格式化 DTO，不在前端解析 `.cfg` 或重新绑定引用。

> **不讲**：布局怎么算（→ [04](04-layout-viewport.md)）、EntityForm 字段渲染（→ [06](06-edit-form.md)）。本文只讲「坐标进来 → 画出来 + 交互」。
>
> 【承前】04 的 `nodes` / `edges` 坐标。　【启后】点节点（editable 态）改字段 → [06](06-edit-form.md)）。

---

## 一、FlowGraph：XYFlow 容器

[`FlowGraph.tsx`](../src/flow/FlowGraph.tsx) 是 memo 组件，包 `ReactFlowProvider` + `ConfigProvider`，挂 `<ReactFlow>` 与右键菜单 / 布局失败覆盖层。`<ReactFlow>` 关键属性：

| 属性 / 子元素 | 作用 |
|---|---|
| `defaultNodes` / `defaultEdges` | 空数组占位，真实数据由 `useEntityToGraph` 的 Effect 1 `setNodes` / `setEdges` 注入（04） |
| `nodeTypes` | `{ node: FlowNode }` |
| `minZoom` / `maxZoom` | `0.1` / `2`，缩放范围 |
| `deleteKeyCode={null}` | 禁用 xyflow 默认 Delete 键——删除走 NodeToolbar 显式按钮 + EditingSession 命令，避免键盘误删 |
| `onNodeDoubleClick` / `onNodeContextMenu` / `onPaneContextMenu` | 节点双击 / 节点右键 / 画布右键 |
| `onPaneClick` / `onMoveStart` 等 | 任何交互都 `closeMenu` 关右键菜单 |
| `<Background/>` / `<Controls showZoom={false}/>` | 背景 / 缩放控件（proOptions 关 attribution） |

除 `<ReactFlow>` 子树外，同 `<ReactFlowProvider>` 下还挂：右键菜单（`menuStyle` 触发时渲染 `FlowContextMenu`）、布局失败覆盖层（`layoutError` 时渲染 `Result` + retry 按钮），以及 `FlowGraphContext` Provider——它包 `children`，与 `ConfigProvider` 是兄弟（均在 `ReactFlowProvider` 内）；`children` 即 `<Routes>`。

关键：`<FlowGraph>{children}</FlowGraph>` 在 app 壳层包裹其 children（主区是 `<Outlet>`；Splitter 布局下左 panel 可能还有第二个 FlowGraph 实例——故 §6「单实例化」指**每个 FlowGraph 内** ConfigProvider 单实例，非全 app 仅一个 FlowGraph）。`defaultNodes` / `defaultEdges` 是空数组占位，真实数据由 `useEntityToGraph` 的 Effect 1 `setNodes` / `setEdges` 注入（04）。

---

## 二、EntityNodeData：type 别名 + domain↔presentation 解耦

`EntityNodeData` 是呈现层「下发袋」：`entity` 纯 domain（不可变、memo-safe），`nodeShow` / `notes` 是呈现层配置，由 `useEntityToGraph` 经 `convertNodeAndEdges` 写入 `node.data`，而非盖章到 entity。

**两个设计点**：

- **`type` 别名而非 `interface`**：xyflow 的 `Node<T>` 要求 `T extends Record<string,unknown>`，type 字面量带隐式索引签名可满足，interface 不带会被判缺失。
- **呈现层数据走 `node.data` 而非盖章到 entity**：entity 保持不可变 / memo-safe；`nodeShow` 走 `node.data` 保留 FixedPage 的 **per-graph override**（同一 entity 在不同图可不同呈现）。

**但 `query` 不走 `node.data`**：`query` 无 per-graph override，渲染组件各自 `useMyStore()` 订阅（resso per-key）——故 query 不进 nodes 重建、搜索时不重跑 ELK。

---

## 三、FlowGraphContext：反向下发（误用即抛）

[FlowGraphContext.ts](../src/flow/FlowGraphContext.ts) 下发的全是 setter（菜单 / 双击 / 布局错误 / retry），**默认 `undefined`，外部误用即抛错**：`useEntityToGraph` 里 `useContext` 取到 `undefined` 直接 throw `'FlowGraphContext missing: useEntityToGraph must run inside <FlowGraph>'`。

ctx 在 FlowGraph 里 `useMemo`（一组 setter），引用稳定。

**为什么反向下发**：FlowGraph 是父、`<Routes>` 是 children。路由组件（Record / Table）通过 context 把菜单 / 错误回调登记到 FlowGraph。context 默认 `undefined` + 消费处 throw，把误用变显式报错（替代旧 dummy noop 静默失败）。

> `retryLayout` 是函数，写 `setRetryLayout(() => fn)`——「存回调」写法，避免 React 把它当 functional updater 调用。

---

## 四、FlowNode：三态分派

[`FlowNode.tsx`](../src/flow/FlowNode.tsx) 是 memo 组件，按 `entity.type`（判别联合）分派。三元取出互斥字段（TS 自动收窄），各自条件渲染对应组件：

```
entity.type === 'editable' → edit    渲染 EntityForm        （06 讲）
entity.type === 'readonly' → fields  渲染 EntityProperties   （字段列表）
entity.type === 'card'     → brief   渲染 EntityCard         （封面 + 标题 + Descriptions）

节点根部条件渲染顺序：noteBlock（顶部）→ NodeTitle → 对应态组件 → handleIn / handleOut 两个 Handle
```

判别联合 + 条件渲染取代原 `let + if` 命令式级联：真分支 TS 自动收窄，互斥字段 `edit` / `fields` / `brief` 各自可见。

> `EntityBase` 上的共享字段 `id` / `label` / `note` / `assets` / `handleIn` / `handleOut` 不在判别联合里，三态外直接取——别困惑这些字段从哪来。

`nodeStyle` 由 `getNodeBackgroundColor` + `getNodeWidth` 算（04），`nodeShow` 进 `useMemo` deps——entity 引用不变时改主题色仍重算（避免 stale）。

---

## 五、nodeAnchor：统一命令参数

所有节点编辑命令（fold / move / delete / note）参数统一走 `nodeAnchor(nodeProps)`（[nodeAnchor.ts](../src/flow/nodeAnchor.ts)），吐 `{id, x, y}`（x/y 即 positionAbsoluteX/Y，屏幕坐标，与视口无关）。命令层（路由）需 `{id, 屏幕坐标}` 定位被操作节点，收口构造避免各处重复字面量、且与视口变换解耦。

---

## 六、单实例化（性能）

两个上提：

- **antd Form 主题 `FORM_THEME` 从「每节点一个 ConfigProvider」上提到 FlowGraph 单实例**。原 EntityForm 每节点套 ConfigProvider：可编辑节点越多、ConfigProvider 越多（各做一次 theme 合并 + context Provider 实例，初始 mount 开销随节点数线性涨）。上提到 FlowGraph 后全图一份，语义不变（FlowGraph 子树唯一的 antd Form 就是 EntityForm；Background / Controls / FlowContextMenu 均非 Form，recordRef / table 等只读视图节点无 Form 亦不受影响）。
- **`FlowStyleManager` 上提到 app 顶层单实例**（[FlowStyleManager.tsx](../src/flow/FlowStyleManager.tsx)）。CSS 变量全局唯一，多实例并存时一个 unmount 的 cleanup 会抹掉另一实例仍在用的变量——上提理由详见 [09](09-cross-cutting.md)。

---

## 七、useNodeNote：headless hook

`useNodeNote`（[NodeNote.tsx](../src/flow/NodeNote.tsx)）用 headless hook 返回 ReactNode（而非组件 / render-prop）：吐 `{noteBlock, editNoteButton}` 两块。`editNoteButton` 作 prop 注入兄弟组件 `<NodeTitle>`，`noteBlock` 在节点顶部直接摆放——「产出一个值喂兄弟 + 一块 DOM 自己摆」的混合需求，hook 返回比 render-prop 精确。

note 的「草拟空」与「清空空」用 `noteDrafting` 标志区分：两者 `tmpNote.note` 都是 `""`，纯数据无法区分；原代码靠预填 `"note："` 让点击添加后显示，但会被当真实内容提交进 json。

---

## 八、交互回调

FlowGraph 注册交互回调到 `<ReactFlow>`：

- `onNodeContextMenu` / `onPaneContextMenu`：右键菜单（绝对定位 `menuStyle` + `menuItems`）。
- `onNodeDoubleClick`：双击节点（如 RecordRef 双击跳转 record）。
- `onEdgeDoubleClick`：内部引用边定位到当前图的可见目标；brief 目标沿现有 record 导航打开详情。节点菜单也提供「打开引用目标」；brief 只有一个内部目标时，原有打开 record 动作直接携带该目标，多个目标则在菜单中逐项列出。
- `onPaneClick` / `onNodeClick` / `onMoveStart` / `onNodeDragStart`：任何交互都 `closeMenu` 关右键菜单。

回调本体（`nodeMenuFunc` / `paneMenu` / `nodeDoubleClickFunc`）由路由组件经 `FlowGraphContext` 登记上来（§3 反向下发）。

内部定位只投影服务端 `FieldRef`，`referenceTarget.ts` 根据现有字段链构造节点 ID，不在前端解析引用键。目标 revision 与 `toRevision` 相同才使用数组位置；否则回退 record 根、提示并刷新关系。fold/embed 隐藏目标时落到最近可见父节点，继续使用原展开按钮，不修改折叠数据。跨 record 目标通过 router 临时 state 传递，未写入配置、书签或偏好。

编辑图从最后一次服务端 `recordResult` 读取关系元数据，不将 `$refs` 放入 `editingObject`。进入 dirty 的第一次变化撤除派生边、在根标题显示待校验提示；后续键入仍不重建图。保存成功仍保持待校验，直到新的 GET 响应到达且其数据对应当前编辑对象，才重新投影位置；refetch 失败不恢复旧数组下标。使用 query 响应时间标记兼容没有 revision 的旧服务。undo 回到原数据后也可重新投影合法位置。值类 dirty 变化不重复执行视口指令，定位请求也不会因 StrictMode 重跑而被全图适配覆盖。循环关系只连接已有节点，不递归复制目标对象。

---

## 九、Cheat Sheet

**加一种节点类型**：在 `entityModel.ts` 的 `Entity` 判别联合加一支 → `FlowNode` 加条件渲染分支 → `recordEditEntityCreator` / `recordEntityCreator` 产出它。

**节点内加交互命令**：参数走 `nodeAnchor(nodeProps)`（不要手拼 `{id, x, y}`），命令写进 `entity.edit.editOnXxx`（由 EditingSession 注入，06 讲）。

**图内加 antd 主题**：改 FlowGraph 的 `FORM_THEME`（单实例），别在节点内套 ConfigProvider。

**右键菜单项**：路由组件经 `FlowGraphContext.setPaneMenu` / `setNodeMenuFunc` 登记回调；FlowGraph 负责定位 + 渲染 `FlowContextMenu`。

---

## 一句话速记

- **FlowGraph 单实例包 `<Routes>`**：`defaultNodes` 空占位，真实数据由 `useEntityToGraph` 的 Effect 注入。
- **`EntityNodeData` 用 type 别名**（xyflow 要索引签名）；呈现层数据走 `node.data`（per-graph override），`query` 走 store（resso per-key）。
- **FlowGraphContext 反向下发**：FlowGraph 父、Routes children；默认 undefined + 消费处 throw，误用变显式。
- **FlowNode 三态分派**：判别联合 + 条件渲染（readonly / editable / card）。
- **`nodeAnchor` 统一命令参数**：`{id, 屏幕坐标}`，与视口解耦。
- **单实例化**：Form 主题上提到 FlowGraph（全图一份）；FlowStyleManager 上提到 app 顶层（见 09）。
- **`deleteKeyCode={null}`**：删除走显式按钮 + EditingSession，防键盘误删。

# 05 Flow 图层（XYFlow 集成）

Flow 图层把 04 算出的 `nodes` / `edges` 画到屏幕，提供节点 UI 与交互。核心是 `FlowGraph` 集成 `@xyflow/react`，`FlowNode` 按实体类型分派渲染。

> **不讲**：布局怎么算（→ [04](04-layout-viewport.md)）、EntityForm 字段渲染（→ [06](06-edit-form.md)）。本文只讲「坐标进来 → 画出来 + 交互」。
>
> 【承前】04 的 `nodes` / `edges` 坐标。　【启后】点节点（editable 态）改字段 → [06](06-edit-form.md)。

---

## 一、FlowGraph：XYFlow 容器

[`FlowGraph.tsx`](../src/flow/FlowGraph.tsx):51 是 memo 组件，包 `ReactFlowProvider` + `ConfigProvider`，挂 `<ReactFlow>` 与右键菜单 / 布局失败覆盖层：

```tsx
return <ReactFlowProvider>
    <ConfigProvider theme={FORM_THEME}>
        <div className='flowGraphCanvas'>
            <ReactFlow
                defaultNodes={defaultNodes}      // 空数组占位，真实 nodes 由 useEntityToGraph 的 Effect 1 setNodes 注入
                defaultEdges={defaultEdges}
                nodeTypes={nodeTypes}            // { node: FlowNode }
                minZoom={0.1} maxZoom={2}
                deleteKeyCode={null}             // 禁用 xyflow 默认 Delete 键——删除走 NodeToolbar 显式按钮 + EditingSession
                onNodeDoubleClick={onNodeDoubleClick}
                onNodeContextMenu={onNodeContextMenu}
                onPaneClick/onMoveStart={closeMenu}   // 任何交互都关右键菜单
                onPaneContextMenu={onPaneContextMenu}
                proOptions={proOptions}>         // hideAttribution
                <Background/><Controls showZoom={false}/>
            </ReactFlow>
            {menuStyle && <FlowContextMenu .../>}            {/* 右键菜单（绝对定位）*/}
            {layoutError && <Result status='error' extra={retryLayout && <Button onClick={retryLayout}>...</Button>}/>}
        </div>
    </ConfigProvider>
    <FlowGraphContext value={ctx}>{children}</FlowGraphContext>   {/* children = <Routes>；与 ConfigProvider 是兄弟（均在 ReactFlowProvider 内）*/}
</ReactFlowProvider>;
```

关键：`<FlowGraph>{children}</FlowGraph>` 在 app 壳层包裹其 children（主区是 `<Outlet>`；Splitter 布局下左 panel 可能还有第二个 FlowGraph 实例——故 §6「单实例化」指**每个 FlowGraph 内** ConfigProvider 单实例，非全 app 仅一个 FlowGraph）。`defaultNodes` / `defaultEdges` 是空数组占位，真实数据由 `useEntityToGraph` 的 Effect 1 `setNodes` / `setEdges` 注入（04）。

`deleteKeyCode={null}`：删除走 NodeToolbar 显式按钮 + EditingSession 命令，避免键盘误删。

---

## 二、EntityNodeData：type 别名 + domain↔presentation 解耦（红线 6）

```ts
// FlowGraph.tsx:18 —— 呈现层「下发袋」
export type EntityNodeData = {
    entity: Entity;               // 纯 domain（不可变、memo-safe）
    nodeShow?: NodeShowType;      // 呈现层配置
    notes?: Map<string, string>;
};
export type EntityNode = Node<EntityNodeData, "node">;
```

**两个设计点**：

- **`type` 别名而非 `interface`**（:16-17 注释）：xyflow 的 `Node<T>` 要求 `T extends Record<string,unknown>`，type 字面量带隐式索引签名可满足，interface 不带会被判缺失。
- **呈现层数据走 `node.data` 而非盖章到 entity**：entity 保持不可变 / memo-safe；`nodeShow` 走 `node.data` 保留 FixedPage 的 **per-graph override**（同一 entity 在不同图可不同呈现）。

**但 `query` 不走 `node.data`**（:14-15、FlowNode.tsx:33）：`query` 无 per-graph override，渲染组件各自 `useMyStore()` 订阅（resso per-key）——故 query 不进 nodes 重建、搜索时不重跑 ELK。

---

## 三、FlowGraphContext：反向下发（误用即抛）

[FlowGraphContext.ts](../src/flow/FlowGraphContext.ts) 下发的全是 setter（菜单 / 双击 / 布局错误 / retry），**默认 `undefined`，外部误用即抛错**：

```ts
// useEntityToGraph.ts:90 —— 必须在 <FlowGraph> 内用
const flowGraph = useContext(FlowGraphContext);
if (!flowGraph) throw new Error('FlowGraphContext missing: useEntityToGraph must run inside <FlowGraph>');
```

ctx 在 FlowGraph 里 `useMemo`（:105），引用稳定：

```ts
const ctx = useMemo(() => ({
    setPaneMenu, setNodeMenuFunc: thisSetNodeMenuFunc,
    setNodeDoubleClickFunc: thisSetNodeDoubleClickFunc,
    setLayoutError, setRetryLayout: thisSetRetryLayout,
}), [...]);
```

**为什么反向下发**：FlowGraph 是父、`<Routes>` 是 children。路由组件（Record / Table）通过 context 把菜单 / 错误回调登记到 FlowGraph。context 默认 `undefined` + 消费处 throw，把误用变显式报错（替代旧 dummy noop 静默失败）。

> `retryLayout` 是函数，写 `setRetryLayout(() => fn)`（:100-103）——「存回调」写法，避免 React 把它当 functional updater 调用。

---

## 四、FlowNode：三态分派

[`FlowNode.tsx`](../src/flow/FlowNode.tsx):26 是 memo 组件，按 `entity.type`（判别联合）分派：

```tsx
// FlowNode.tsx:40-42 —— 判别联合三元取出互斥字段（TS 自动收窄）
const edit   = entity.type === 'editable' ? entity.edit   : undefined;
const fields = entity.type === 'readonly'  ? entity.fields : undefined;
const brief  = entity.type === 'card'      ? entity.brief  : undefined;
// ...
return <div style={nodeStyle}>
    {noteBlock}
    <NodeTitle .../>
    {fields && <EntityProperties .../>}    {/* readonly 态：字段列表 */}
    {brief  && <EntityCard .../>}          {/* card 态：封面+标题+Descriptions */}
    {edit   && <EntityForm .../>}           {/* editable 态：编辑表单（06 讲）*/}
    {handleIn  && <Handle type='target' position={Position.Left}  id='@in'/>}
    {handleOut && <Handle type='source' position={Position.Right} id='@out'/>}
</div>;
```

判别联合 + 条件渲染取代原 `let + if` 命令式级联（注释 F1，:36）：真分支 TS 自动收窄，互斥字段 `edit` / `fields` / `brief` 各自可见。

> `EntityBase` 上的共享字段 `id` / `label` / `note` / `assets` / `handleIn` / `handleOut` 不在判别联合里，三态外直接取（FlowNode.tsx:34, 38-39）——别困惑这些字段从哪来。

`nodeStyle`（:45-49）由 `getNodeBackgroundColor`（:45）+ `getNodeWidth`（:46）算（04），`nodeShow` 进 `useMemo` deps——entity 引用不变时改主题色仍重算（避免 stale）。

---

## 五、nodeAnchor：统一命令参数（红线 5）

所有节点编辑命令（fold / move / delete / note）参数统一走 `nodeAnchor(nodeProps)`（[nodeAnchor.ts](../src/flow/nodeAnchor.ts):13），吐 `{id, x, y}`（x/y 即 positionAbsoluteX/Y，屏幕坐标，与视口无关）：

```tsx
// FlowNode.tsx:55-57
const unfoldNode = useCallback(() => {
    edit?.editOnUpdateFold?.(false, nodeAnchor(nodeProps));   // 命令层收口到 {id, 屏幕坐标}
}, [edit, nodeProps]);
```

命令层（路由）需 `{id, 屏幕坐标}` 定位被操作节点，收口构造避免各处重复字面量、且与视口变换解耦。

---

## 六、单实例化（性能）

两个上提：

- **antd Form 主题 `FORM_THEME` 从「每节点一个 ConfigProvider」上提到 FlowGraph 单实例**（:38-49）。原 EntityForm 每节点套 ConfigProvider：N 个可编辑节点 = N 个 ConfigProvider（各做一次 theme 合并 + context Provider 实例，N=45 时是初始 mount 可观开销）。上提后 45→1，语义不变（FlowGraph 子树唯一的 antd Form 就是 EntityForm；Background / Controls / FlowContextMenu 均非 Form）。
- **`FlowStyleManager` 上提到 app 顶层单实例**（[FlowStyleManager.tsx](../src/flow/FlowStyleManager.tsx)）。CSS 变量全局唯一，多实例并存时一个 unmount 的 cleanup 会抹掉另一实例仍在用的变量。

---

## 七、useNodeNote：headless hook

`useNodeNote`（[NodeNote.tsx](../src/flow/NodeNote.tsx):37）用 headless hook 返回 ReactNode（而非组件 / render-prop）：吐 `{noteBlock, editNoteButton}` 两块。`editNoteButton` 作 prop 注入兄弟组件 `<NodeTitle>`，`noteBlock` 在节点顶部直接摆放（FlowNode.tsx:92、:96）：

```tsx
const {noteBlock, editNoteButton} = useNodeNote({id, entity, edit, note, notes, label});
return <div>
    {noteBlock}                                  {/* 自己摆 */}
    <NodeTitle editNoteButton={editNoteButton}/>  {/* 喂兄弟 */}
```

「产出一个值喂兄弟 + 一块 DOM 自己摆」的混合需求，hook 返回比 render-prop 精确。

note 的「草拟空」与「清空空」用 `noteDrafting` 标志区分：两者 `tmpNote.note` 都是 `""`，纯数据无法区分；原代码靠预填 `"note："` 让点击添加后显示，但会被当真实内容提交进 json。

---

## 八、交互回调

FlowGraph 注册交互回调到 `<ReactFlow>`（:66-90）：

- `onNodeContextMenu` / `onPaneContextMenu`：右键菜单（绝对定位 `menuStyle` + `menuItems`）。
- `onNodeDoubleClick`：双击节点（如 RecordRef 双击跳转 record）。
- `onPaneClick` / `onNodeClick` / `onMoveStart` / `onNodeDragStart`：任何交互都 `closeMenu` 关右键菜单。

回调本体（`nodeMenuFunc` / `paneMenu` / `nodeDoubleClickFunc`）由路由组件经 `FlowGraphContext` 登记上来（§3 反向下发）。

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
- **单实例化**：Form 主题 45→1（FlowGraph）；FlowStyleManager 上提到 app 顶层。
- **`deleteKeyCode={null}`**：删除走显式按钮 + EditingSession，防键盘误删。

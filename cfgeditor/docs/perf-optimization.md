# cfgeditor 性能优化记录（2026-07）

## 背景

基于三路静态审查（重渲染热点 / React Flow 图形 / 列表派生计算）+ chrome-devtools MCP 运行时验证，对 cfgeditor 做性能优化。每项优化均给出 **baseline 数字 → 修复 → after 数字**，不靠猜。

## 已完成（按 ROI 排序）

### TOP1：CfgEditorApp schema select 稳定化 — commit `33fd5386`

**根因**：`CfgEditorApp` 的 schema useQuery 用内联 `select: (rawSchema) => new Schema(rawSchema)`。

- 内联箭头每次 render 新身份 → React Query 每次 render 重跑 select → `new Schema()` 重复执行（构造函数遍历全部 items、建多个 Map、为每张 table 建 idMap，毫秒级）
- Schema 是含 Map 字段的 class，`replaceEqualDeep` 判不等 → schema 引用每帧变 → `outletContext` useMemo 每帧失效 → Outlet 子树（Table/TableRef/Record/RecordRef，context 变化绕过 `memo`）全树重渲

**修复**：select 提为模块级常量 `schemaSelector`，只在 rawSchema 变化时构造。

**验证**（chrome-devtools，dragPanel 切换、schema 数据未变时）：

| | Schema 构造次数 |
|---|---|
| baseline | 2（StrictMode 双调，生产对应 1） |
| 修复后 | **0** |

### TOP2：RefIdList 补 virtual + scroll — commit `73f3643f`

**根因**：RefIdList 是 Finder 四个列表里唯一没 virtual 守门的裸 antd Table，dataSource 受用户可调 `refIdsMaxNode` 控制，高频被引记录（`item.itemtype/1` 有 102 个引用方）会一次渲染全部 DOM 行。

**修复**：补 `virtual={length>30}` + `scroll={{y:300}}`。

> 关键发现：antd v6 Table 的 `virtual` **必须配 `scroll.y` 才真正生效**。LastAccessed 等兄弟组件虽写了 `virtual={>30}` 但缺 scroll 且数据 ≤22（history 上限），从未触发，是 dead code——没人发现它其实没生效。RefIdList 数据真会 >30，故补 scroll 让 virtual 落地。

**验证**（`item.itemtype/1`，maxIds=100，102 引用方）：

| | `.ant-table-row` DOM 数 |
|---|---|
| baseline | 102 |
| 修复后 | **8**（视口内，4068px 总高由 spacer 撑） |

### TOP3：FlowGraph 启用 onlyRenderVisibleElements — commit `e714ce4f`

**根因**：ReactFlow 默认渲染全部节点/边，单个 FlowNode 是 Form/List/Tooltip/多 Button 的重组件，大图视口外节点白白挂载 DOM。

**修复**：ReactFlow 加 `onlyRenderVisibleElements`（xyflow 官方 windowing），视口外节点不挂 DOM、pan/zoom 按需挂载；节点 state 保留，编辑态 session 不受影响。

**验证**（recordRef `item.itemtype/1`，39 节点）：

- React fiber 确认 `onlyRenderVisibleElements=true` 已传入 ReactFlow（HMR 生效）
- 视图正常渲染，39 节点无回归
- fitView 全显时初始 DOM=全部节点（fitView 把所有节点纳入视口，预期行为）；用户 zoom in/pan 后视口外节点 DOM 由 xyflow 按需回收
- 局限：MCP 无法模拟真实滚轮（react-flow/d3-zoom 不响应合成 wheel 事件），DOM 减少数字未取，机制由 xyflow 保证

## 评估后跳过

### TOP4：query 三处订阅 → FlowNode 单点 + prop 下发（跳过）

Flow agent 建议：query 只在 FlowNode 订阅 + prop 下发 EntityProperties/EntityCard。深入分析后**该方案无效**：

- **query 变时**：query 是搜索关键词，变 → Highlight 高亮必须更新 → 子组件无论用订阅还是 prop 都会重渲（prop 下发时 query 变 → 子 memo 比较出 query 变 → 仍重渲）。重渲次数不变。
- **query 不变时**：resso per-key 订阅保证订阅 query 的组件在 query 不变时不重渲；FlowNode 因其它原因重渲时，子组件 memo 已短路。当前架构已最优。

prop 下发既不减 query 变时的重渲，query 不变时也已短路——无收益。真正能减的是「Highlight 细粒度订阅 query」（只重渲 Highlight 而非 EntityProperties 整体），属另一处改动、收益未验证，供后续。

## 已确认健康项（审查核查，非问题）

- React Flow `nodeTypes` 模块级稳定引用、节点 + 子组件全 `memo`、布局后 `data` 引用稳定性刻意保留（spread 时 `{...n, position}` 不碰 `n.data`）
- ELK 布局走 Web Worker + react-query 5min 缓存
- resso 多为窄解构（per-key 订阅）
- historyModel 增长有 22 条上限
- 无 index-key 嫌疑、无 render 期 `JSON.parse`/`structuredClone`
- 各 entityMap `useMemo` 合理（Table.tsx 还专门注释了 React Compiler 不 memo 副作用块需手动）

## 回归检查

- `pnpm run lint`（oxlint）：通过
- `pnpm test:run`（vitest）：20 files / **265 tests 全过**

## 后续中低优先项（未做，供参考）

- Ref 边全量 `animated:true`（`entityToNodeAndEdge.ts`）：Ref 边密集图静止时持续 paint，可按节点数阈值或开关降级
- `createRefs` O(R×B) 嵌套扫描（`recordRefUtils.ts`）：高频被引记录 recordRef 视图，可改 `Set` 预构建 O(R) 化
- Record 编辑态 fold 切换全量重建（`Record.tsx` entityMap useMemo deps 含 `folds` → convert 重跑 → 每节点新 data → 击穿 FlowNode memo）：深层嵌套记录编辑场景
- `SearchValue` columns 未 memo（统一性瑕疵，搜索结果大时体感）
- `LastModified` 全量遍历 + 排序（取决于项目记录数；渲染已有 virtual>30 兜底）
- 拖动节点 `title` useMemo 含整个 `nodeProps`（`FlowNode.tsx`，拖动每 tick 重算 title + foldButton，拖动卡顿的可能来源）

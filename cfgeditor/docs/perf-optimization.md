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

## 评估后跳过
### TOP3：FlowGraph 启用 onlyRenderVisibleElements — （跳过）
改后视角拖放会卡

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

## 2026-07-12 深度复查（textarea autoSize 修复后）

### 结论先行：加载后已稳定 60fps，「持续风暴」是测量假象

textarea autoSize 移除（commit `d4393d8d`）后复查 `/edit/record/skill.effect/301003`（30 节点）：

| 指标 | 修复前 | 现在 |
|---|---|---|
| 加载卡顿 | ~6s（DOM 稳定前 37.5s） | LCP 944ms~1.1s，DOM ~1s 稳定 |
| 加载后稳态 | rAF 风暴持续 | **60fps，零 longtask** |
| 加载期长任务 | 6s 卡顿 | 5 个共 877ms，最大 570ms |

**测量陷阱**（曾一度误判为新回归，浪费整轮调查）：用 chrome-devtools MCP 测 rAF 帧间隔，两个陷阱会制造「假持续风暴」：
1. 被测 tab 失焦/后台时浏览器把 rAF 节流到 ~30fps（33ms/帧）。`busy≈1000ms/s` 其实是 60fps 正常累计（60帧×17ms）——判健康看 `max 帧间隔 ≤19ms`，不是 busy 总和。
2. 自注入 `setTimeout` patch（`new Error().stack` 抓高频 timer 调用栈）本身的开销就是 33ms/帧 风暴的来源（antd `Form/hooks/useDebounce` 每 10ms 调一次 setTimeout，探针 stack 开销被放大）。

**金标准**：`PerformanceObserver({entryTypes:['longtask']})`（系统级、零探针开销）。本次确认加载后 longtask=0。

### 加载期 570ms 长任务 = React 首屏渲染固有成本（非 bug）

trace selfTime 分析（栈算法去嵌套）显示 store chunk 占加载期 JS 70%、单函数 `D` selfTime 1335ms（57%）。但 `D` 是 **React Scheduler 的 work loop**（`unstable_now`/`expirationTime`/`MessageChannel` 标志；scheduler 被 vite 打进 store chunk）—— D 是 React 渲染主循环容器，其 selfTime 含所有组件 render 工作。

**570ms 长任务 = React 把这堆 antd 组件挂到 DOM 的不可避免工作量**。无强制布局/绘制瓶颈（Layout 49ms、Paint 4ms、Style 0）。

| 渲染规模 | 数量 |
|---|---|
| react-flow 节点 / antd FormItem | 30 / 88 |
| Button / Icon(anticon) / svg | 129 / 217 / 249 |
| Select / InputNumber / textarea / input | 40 / 31 / 30 / 71 |

React Profiler 各组件 selfTime（dev StrictMode 双调值，生产约半）：Button 4800ms、IconReact 4100ms、ItemHolder 3900ms、AntdIcon 3200ms、Select 2800ms、InputNumber 2300ms、Trigger 2100ms、Field 2100ms。**Button + Icon 是最大开销**，全是 antd 控件 mount 固有成本。

### 继续压加载期 的 4 个方向（均含权衡，未实施）

1. **重评 onlyRenderVisibleElements**（ROI 最高）：视口外节点不挂 DOM，30 节点若视口只显 8-10 个，省 60%+ mount。TOP3 曾因「拖动卡」revert，可用「拖动时临时关闭」开关解决。
2. **startTransition 分片渲染**：节点挂载包进 startTransition，570ms 被 React 切成多个 <50ms 小 task，加载期主线程可响应。总加载时间不变，只改体感。低成本低风险。
3. **精简 Button/Icon**：129 Button 大头来自 `ArrayOfPrimitiveFormItem` 每项 3-4 个 Button（删/上移/下移/展开），主节点有大数组字段时可默认折叠/精简。中成本，涉 UI 调整。
4. **接受加载现状，转交互期**：加载 1.3s 已可接受，转测稳定期卡顿（拖动/fold/搜索/滚动）——对用户体感影响更大。

### 测量方法学（避免重蹈覆辙）

- **判健康的金标准**：`PerformanceObserver(longtask)` + `max 帧间隔 ≤19ms`。不要被 `busy≈1000ms/s`（60fps 正常累计）误导。
- **自探针绝不 patch setTimeout/Date 抓栈**：`new Error().stack` 在高频 timer 下制造假风暴。
- **trace 分析用 selfTime（栈算法去嵌套），不是 dur**：dur 含子调用会重复计。store chunk「D 占 1654ms dur」曾误判为业务热点，实际 D 是 React scheduler 容器、selfTime 高只因它包裹了全部组件 render。
- **dev/release selfTime 分布不同**：dev 被 react-dom dev 开销掩盖业务代码，release 才显现真实热点。判业务热点用 release trace；判组件排名用 dev React Profiler（fiber.actualDuration，selfTime = actualDuration − 直接子 actualDuration 之和）。

## 2026-07-12 react-devtools 复查：加载主成本是 getIdOptions 重建，不是 antd mount

### 对上文结论的修正

「570ms 长任务 = antd 组件挂 DOM 的不可避免工作量」**不完整**。用 react-devtools CLI（React Profiler）按 commit 拆 `/edit/record/skill.effect/301003`（30 节点编辑态）的 remount：

| commit | 耗时 | 组件数 | 是什么 |
|---|---|---|---|
| #0 | 577.9ms | 148 | **RecordWithResult 建 entityMap**（self 528ms，纯计算）|
| #2 | 311ms | 5339 | antd 大军 mount（Button/Icon/Select/FormItem）|
| #3 | 54ms | 2553 | ELK 布局回填后二刷 |

最大单块同步开销（#0 577ms）**不是 antd mount**，是 `RecordWithResult` 的 entityMap `useMemo`（`RecordEditEntityCreator.createThis()`）——纯计算、可优化；antd mount 是另一个独立的 #2 commit。深度复查在 release trace 里看到的「D 函数 selfTime 1335ms」被归给 React Scheduler，dev 下署名清晰，实际是 `RecordWithResult`/createThis。

### createThis 内部归因（原型方法 performance.now() 埋点，StrictMode 双调，绝对值含探针膨胀看比例）

| 方法 | 调用 | ms | 备注 |
|---|---|---|---|
| getAutoCompleteOptions | 122 | 417.7 | **≈ createTime 全部成本**，内部就是 getIdOptions 建 FK 选项 |
| createThis | 2 | 421.8 | 单次 ~211ms |
| getFoldState | 100 | 0.2 | folds 列表空，证实非首屏元凶 |
| tryCreateEmbedded* | — | ~1.5 | 可忽略 |

**~99% createThis 成本 = `getIdOptions` 给 61 个 FK 字段反复重建选项数组（含 JSX `<Flex>` label）**。选项只依赖 `sTable.recordIds` + `valueToInteger`，与 editing object / folds 无关——每次建图、每次 fold 切换、每次结构编辑都全量白建。

### 修复：WeakMap 缓存 getIdOptions

`schema.tsx` 加 `_idOptionsCache = new WeakMap<STable, {v0?,v1?}>()`，按 (STable, valueToInteger) 缓存。schema 重建时 STable 是新对象 → WeakMap 自动失效，无跨 schema 泄漏。同步修 `getIdOptionsWithNew`：原对 `getIdOptions` 返回的空数组 `push`「new」项会污染缓存，改 `return [新对象]`。

### 验证（warm remount，模拟 fold 切换/重编辑——正是「后续中低优先项」标记的痛点）

| | RecordWithResult self | commit #0 |
|---|---|---|
| 修复前 | 528.1ms | 577.9ms |
| 修复后 | **2.8ms**（~99% ↓）| **46.5ms** |

- cold 首屏（缓存空）：profile 跨不过 reload 测不到；推算首个 mount 仍为 6 个不同 FK 表各建一次（~18ms）+ 遍历（~40ms）≈ 60ms（仍 ~88% ↓），且整个会话只付这一次。
- lint（oxlint）通过；vitest 20 files / 265 tests 全过。

### 剩余加载成本（独立，未动）

- **commit #2 antd 大军（311~899ms，dev 噪声大）**：真·mount 5339 个 antd 控件，与 getIdOptions 无关。要砍只能少 mount（大数组字段默认折叠、控件懒挂载），属 UI 结构改动，单独决策。onlyRenderVisibleElements / startTransition 已否决。
- `Folds.isFold` O(list.length) 线性扫描：首屏 folds 空无影响；fold 累积后的 fold 切换场景可改 Map key 化（次要）。

### react-devtools CLI 工具链（Windows）

`agent-react-devtools@0.4.0` 在 Windows 有两个 bug，已 patch `node_modules/agent-react-devtools/dist/{cli,daemon}.js`：
1. daemon 监听 unix socket 路径 `C:\...\daemon.sock` → Windows 命名管道名含盘符冒号 `EACCES`。win32 下改返回 `\\.\pipe\agent-react-devtools-daemon`。
2. cli 用 `new URL(import.meta.url).pathname` 解析 daemon.js 路径 → Windows 得到 `/D:/...`（盘符前多斜杠），spawn 静默失败。改 `fileURLToPath`。

⚠️ **patch 在 node_modules 里，`pnpm install` 会丢**；重装后需重打或等上游修。dev-deps（`agent-react-devtools` / `react-devtools-core`）和 `vite.config.ts` 的 `reactDevtools()` 插件已落 package.json，保留供后续 profiling。

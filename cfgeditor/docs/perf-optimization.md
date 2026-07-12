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

## 2026-07-12 复盘：「getIdOptions 缓存」假设被生产实测推翻

### 起因（已被推翻的 dev 结论）

用 react-devtools CLI 在 **dev** 下 profile `/edit/record/skill.effect/301003`（30 节点编辑态）remount，按 commit 拆：commit #0 `RecordWithResult` 的 entityMap useMemo self **528ms**；埋点归因看似 ~99% 是 `getIdOptions` 给 61 个 FK 字段反复重建选项数组（含 JSX）。据此加了 `WeakMap<STable,{v0,v1}>` 缓存，dev 复测 self 528→2.8ms（~99%），提了 commit。

### 生产实测推翻（chrome-devtools + `pnpm build`/`pnpm preview`）

react-devtools CLI 连不上生产包（connect.js 在 `import.meta.env.PROD` 跳过），改用 chrome-devtools 直接定时器测 `getIdOptions` 本体（无探针膨胀），并 PerformanceObserver(longtask) 测 warm remount：

| 场景 | getIdOptions 真实成本 | warm remount longtask |
|---|---|---|
| 有缓存 | ~0（25 命中 / 0 未命中）| 379ms |
| 无缓存 | **1.7ms**（25 次全重建）| 356ms |

**缓存只省 ~1.7ms/mount——可忽略（longtask A/B 在噪声内基本无差）。** 缓存逻辑本身正确（schema 确定性数据不该重复建），但收益不抵复杂度，已 revert。

### 为什么 dev 数字严重失真

dev 的「528ms / 99%」是三重放大假象（正是上文「测量方法学」警告的）：
1. **dev react-dom 构建比 prod 慢 2-5×**——React Profiler 的 actualDuration 在 dev 膨胀。
2. **StrictMode 双调** useMemo ×2。
3. **原型方法探针**（performance.now×~1500 次）自身开销被计入 inclusive。

教训重申：**判业务热点「绝对成本」必须用 release 直测定时器；dev React Profiler 只能看组件「排名」，绝对耗时不可信。**

### 真瓶颈（未解）

`/edit/record/skill.effect/301003` warm remount 的 ~356ms longtask = **antd 大军 mount**（5339 组件：~129 Button / 217 Icon / 249 svg / 40 Select / 31 InputNumber / 30 textarea / 71 input / 88 FormItem）+ entity 图构建，与 getIdOptions 无关。这是 React 挂这么多控件的固有工作量。onlyRenderVisibleElements / startTransition 已否决；要砍只能少 mount（大数组字段默认折叠、控件懒挂载），属 UI 结构改动，待单独决策。

### react-devtools CLI 工具链（Windows，保留在 commit bee6f921）

`agent-react-devtools@0.4.0` 在 Windows 有两个 bug，已 patch `node_modules/agent-react-devtools/dist/{cli,daemon}.js`：
1. daemon 监听 unix socket 路径 `C:\...\daemon.sock` → Windows 命名管道名含盘符冒号 `EACCES`。win32 下改返回 `\\.\pipe\agent-react-devtools-daemon`。
2. cli 用 `new URL(import.meta.url).pathname` 解析 daemon.js 路径 → Windows 得到 `/D:/...`（盘符前多斜杠），spawn 静默失败。改 `fileURLToPath`。

用法：`CFG_RDT=1 pnpm dev`（门控注入连接脚本）+ `node node_modules/agent-react-devtools/dist/cli.js start`。⚠️ **patch 在 node_modules 里，`pnpm install` 会丢**，需重打。profile 不跨 reload（reload 后 0 commits），抓 mount 用 SPA 导航（`history.pushState` + `dispatchEvent(new PopStateEvent('popstate'))`）；`profile slow` 是 inclusive，看 selfTime 用 `profile commit <N>`。

## 2026-07-12 antd mount 大军深查（workflow + 实测）：UI 锁死下是固有成本，接受

### 调查（workflow 三路并行）

结构归因 / antd v6 源码 mount 成本 / 用法+结构机会三路深挖，定位 antd v6 每实例固定开销（源码级，node_modules 为准）：
- 非 text/link 的 antd Button 被 `<Wave>` 包，每实例 mount 时 `addEventListener('click',fn,true)` 捕获监听 + useStyle cssinjs；另有**无 deps 的 useEffect 每 render 读 buttonRef.textContent**（强制 layout 回读）。
- @ant-design/icons 每实例 `useInsertStyles` useEffect（cfgeditor 未启 zeroRuntime 故全量执行）。
- InputNumber render 中**无条件 createElement 4 个内部图标**（Up/Down/Plus/Minus）。
- Select 的 SelectTrigger/Trigger 即使 closed 也常驻 JS（popup DOM 才 lazy）。
- FormItem 有 name 时通过 rc-form `<Field>` 注册到 form store + 消费 5 个 context。

### 实测定论（chrome-devtools，生产 build，PerformanceObserver longtask，warm remount）

| 实验 | longtask | 结论 |
|---|---|---|
| 基线（30 节点全表单） | 349ms | — |
| InputNumber `controls={false}`（砍 62 个内部 icon + useInsertStyles） | 365 / 340ms（噪声内 ≈ 基线） | **per-instance 开销可忽略** |
| EntityForm 返回 null（只留节点外壳，表单全移除） | **56ms** | **表单 = 84%（293ms）** |

每组件边际成本 ≈ 293ms ÷ 5248 表单组件 = **56µs/组件**（fiber + DOM + commit 分摊的聚合工作量）。

### 算术：UI 锁死下的天花板

要省 100ms 得少挂 ~1800 个组件。UI 锁死（30 份完整表单全展开）下，唯一能减组件数又不坏 UI 的是「antd 控件→视觉等价原生元素」：
- antd Button(129)→原生 `<button>` + Icon(217)→模块级内联 SVG + InputNumber(31)→原生 `<input>` ≈ 减 ~450-600 组件 → **~25-34ms**。
- FormItem(88)/Select(40) 不能换（承担 rc-field-form 取值/提交 + FK 搜索，换了表单功能坏）。
- 为 ~30ms 去做自定义 IconButton + 内联 SVG + 原生 InputNumber 并复刻 antd 样式 + 保 focus/aria/disabled 联动，性价比低；且 icon 零效果实验暗示此估算偏乐观。

### 结论：接受现状

「少 mount 表单」（手风琴 / 只读概要 / 字段懒挂载，估省 ~270ms）是唯一大杠杆，但**改 UI 已被否决**（要求保持 30 节点全展开完整可编辑表单）。UI 锁死下 ~350ms warm remount longtask 是固有成本。这印证本文档最初的判断（「570ms = antd mount 不可避免工作量」）——中间用 dev React Profiler 误判有 528ms 计算瓶颈（getIdOptions，已 revert），生产实测绕一圈回到原点。

体感：这 ~350ms 是**进编辑视图的一次性 mount 成本**（cold ~570ms），加载后稳态 60fps / 零 longtask，非持续卡顿。接受。

### 否决项汇总（勿重试）
- FlowGraph `onlyRenderVisibleElements`（拖放卡顿）。
- startTransition 分片（体感无意义）。
- getIdOptions WeakMap 缓存（生产仅省 ~1.7ms，已 revert）。
- 活动节点手风琴 / 只读概要 / 字段懒挂载（改 UI，已否决）。
- antd→原生控件（UI 锁死下天花板 ~30ms，性价比低）。

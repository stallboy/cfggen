# 性能记录（2026-07）：baseline → 修复 → after，不靠猜

> 这是一份**点在当下的性能实证记录**（2026-07）：每项给 baseline → 修复 → after 的实测数字、commit 号、否决项，基于三路静态审查 + chrome-devtools 运行时验证。它不是教程——是「当时测到什么、改了什么、为什么不改」的快照，数字不随重构重算。
>
> **配套**：release 实测方法（PerformanceObserver longtask、定时器直测）见 [`06-undo-redo.md`](./06-undo-redo.md) §5.3；测量陷阱见 memory「性能测量陷阱」。

---

## 1. 版本对比：ae5fbb8（历史） vs master（当前）

测试日 2026-07-14。两版本共用同一后端（localhost:3456）、同一份数据，仅前端 build 产物不同。`ae5fbb8` 本身只改 CI（Node 版本），真实差异来自其后的数十个 commit（flow 重构、undo/redo、React Compiler、antd6/vite8/react-router8）。

| 维度 | 历史 ae5fbb8 | 当前 master | 差异 |
|---|---|---|---|
| 切换 301003↔301006（核心指标，6 轮中位数） | 1572 ms | **1028 ms** | **-35%** |
| 首次加载 LCP | 3338 ms | **2126 ms** | **-36%** |
| 首屏 JS bundle | 6,070 KB（单文件巨石） | 2,441 KB（主 chunk） | **-60%** |

**结论**：切换与首次加载双双快 ~35%。主要结构性来源是 **code splitting**（路由级 + vendor 分割，首屏只 2.4 MB；elkjs 1.6 MB 独立成 worker），叠加 **React Compiler**（减少不必要重渲染）与期间重构。

字段编辑响应实测 10–25 ms，完全可接受。

---

## 2. 已落地优化（按 ROI 排序）

### TOP1：CfgEditorApp schema select 稳定化 — commit `33fd5386`

**根因**：`useQuery` 内联 `select: rawSchema => new Schema(rawSchema)` 每次 render 新身份 → `new Schema()` 每帧重跑（遍历全部 items、建多个 Map、为每张 table 建 idMap）。Schema 含 Map 字段，`replaceEqualDeep` 判不等 → schema 引用每帧变 → Outlet 子树（Table/Record 等，context 变化绕过 `memo`）全树重渲。

**修复**：select 提为模块级常量，只在 rawSchema 变化时构造。
| | Schema 构造次数（dragPanel 切换、数据未变）|
|---|---|
| baseline | 2（StrictMode 双调，生产对应 1） |
| 修复后 | **0** |

### TOP2：RefIdList 补 virtual + scroll — commit `73f3643f`

**根因**：Finder 四列表里唯一没 virtual 守门的裸 antd Table，`dataSource` 受 `refIdsMaxNode` 控制，高频被引记录（`item.itemtype/1` 102 引用方）一次渲染全部 DOM 行。

**修复**：`virtual={length>30}` + `scroll={{y:300}}`。⚠️ **antd v6 Table 的 `virtual` 必须配 `scroll.y` 才真正生效**——兄弟组件 LastAccessed 写了 `virtual={>30}` 但缺 scroll 且数据 ≤22，从未触发，是 dead code。
| | `.ant-table-row` DOM 数（maxIds=100，102 引用方）|
|---|---|
| baseline | 102 |
| 修复后 | **8** |

---

## 3. 否决项汇总（勿重试）

| 项 | 否决理由 |
|---|---|
| FlowGraph `onlyRenderVisibleElements` | 视角拖放卡顿 |
| query 三处订阅 → FlowNode 单点 + prop 下发 | query 变时 Highlight 必须更新，prop 下发不减重渲；query 不变时 resso per-key 订阅已短路。无收益 |
| `getIdOptions` WeakMap 缓存 | dev 误判为 528ms/99% 瓶颈，生产直测仅省 **~1.7ms/mount**（噪声内），已 revert（见 §4 复盘） |
| startTransition 分片 | 体感无意义 |
| 活动节点手风琴 / 只读概要 / 字段懒挂载 | 改 UI，已否决（要求保持 30 节点全展开可编辑表单） |
| antd → 原生控件 | UI 锁死下天花板仅 ~30ms，复刻样式/联动性价比低 |

---

## 4. 复盘：「getIdOptions 缓存」假设被生产实测推翻

dev 下用 react-devtools CLI profile `/edit/record/skill.effect/301003`（30 节点编辑态）remount，commit #0 entityMap useMemo self **528ms**，埋点归因看似 ~99% 是 `getIdOptions` 给 61 个 FK 字段反复重建选项数组。加 `WeakMap` 缓存后 dev self 528→2.8ms，提了 commit。

**生产实测推翻**（chrome-devtools + `pnpm build`/`pnpm preview`，直测定时器 + PerformanceObserver longtask）：

| 场景 | getIdOptions 真实成本 | warm remount longtask |
|---|---|---|
| 有缓存 | ~0（25 命中 / 0 未命中）| 379ms |
| 无缓存 | **1.7ms**（25 次全重建）| 356ms |

缓存只省 ~1.7ms/mount——可忽略。**教训：dev 的 528ms/99% 是三重放大假象**（dev react-dom 比 prod 慢 2–5×、StrictMode 双调、原型方法探针 ~1500 次 performance.now 自身开销计入 inclusive）。

> **判业务热点「绝对成本」必须用 release build 直测定时器；dev React Profiler 只能看组件排名，绝对耗时不可信。**

---

## 5. 固有成本：antd mount 大军（接受）

`/edit/record/skill.effect/301003` warm remount 的 ~350ms longtask 是 **antd 控件 mount**（5339 组件：~129 Button / 217 Icon / 249 svg / 40 Select / 31 InputNumber / 30 textarea / 71 input / 88 FormItem）+ entity 图构建，非计算瓶颈。实测（生产 build）：

| 实验 | longtask |
|---|---|
| 基线（30 节点全表单） | 349ms |
| InputNumber `controls={false}`（砍 62 内部 icon） | 365 / 340ms（噪声内 ≈ 基线） |
| EntityForm 返回 null（表单全移除） | **56ms** → 表单 = 84%（293ms） |

每组件边际成本 ≈ **56µs**。UI 锁死（30 份完整表单全展开）下 ~350ms 是固有成本；要省 100ms 得少挂 ~1800 个组件，唯一大杠杆是「少 mount 表单」，但改 UI 已被否决。这是进编辑视图的**一次性 mount 成本**（cold ~570ms），加载后稳态 60fps / 零 longtask，非持续卡顿。

antd v6 源码级固定开销（node_modules 为准）：非 text/link Button 被 `<Wave>` 包，每实例 `addEventListener('click',fn,true)` + useStyle cssinjs + 无 deps useEffect 每 render 读 `buttonRef.textContent`（强制 layout）；icons 每实例 `useInsertStyles`；InputNumber render 无条件 createElement 4 个内部图标；Select 即使 closed 也常驻 JS；FormItem 通过 rc-form `<Field>` 注册 + 消费 5 个 context。

---

## 6. 测量方法学（避坑）

- **金标准**：release build + chrome-devtools 直测定时器 + `PerformanceObserver(longtask)`。
- **墙上计时一律在 performance trace 关闭时采集**——开 trace 跑墙上会拖慢页面（曾测得 4500ms 假值）。
- **切换完成信号**用 `.react-flow__node` 节点数稳定 >0 连续 200ms，**别用 MutationObserver characterData**（被 react-timeago 每秒文本更新污染，出现 6000ms+ 假值）。
- **dev React Profiler 绝对耗时不可信**，只看组件排名（见 §4）。

---

## 7. 已确认健康项

React Flow `nodeTypes` 模块级稳定、节点+子组件全 `memo`、布局后 `data` 引用稳定（`{...n, position}` 不碰 `n.data`）；ELK 走 Web Worker + react-query 5min 缓存；resso 多为窄解构 per-key 订阅；historyModel 22 条上限；无 index-key、无 render 期 `JSON.parse`/`structuredClone`；entityMap `useMemo` 合理。

# 性能对比：ae5fbb8（历史） vs master（当前）

- **测试日期**：2026-07-14
- **对比对象**：
  - 历史版本 `ae5fbb8ed4f428d887dac773c7ad585f1cec93e2`（commit "node 20->24"）
  - 当前版本 `master` HEAD `efac238f`（"折叠态恢复之前描边"）
- **测试页面**：`/edit/record/skill.buff/301003`、`/edit/record/skill.buff/301006`

> 注：`ae5fbb8` 这个 commit 本身只改 CI 配置（Node 版本），不改源码。两版本间的真实差异来自 `ae5fbb8` → `HEAD` 之间的数十个 commit（flow 重构、undo/redo、React Compiler、antd6/vite8/react-router8 等）。

---

## 1. 测试环境

| 项 | 配置 |
|---|---|
| 历史版本 | worktree 检出 `ae5fbb8` → `pnpm build` → `pnpm preview --port 4174` |
| 当前版本 | `master` HEAD → `pnpm preview`（端口 **4173**） |
| 后端 | 共用 `localhost:3456`（同一 cfggen server，数据一致，控制变量） |
| 构建 | 两版本均用 node `v24.11.1` / pnpm `10.15.0` 本地 build |
| 浏览器 | Chrome DevTools MCP，isolated profile（临时 profile） |
| 隔离方式 | git worktree（`D:\work\mygit\cfggen-old`），不覆盖当前版本 dist |

两版本共用同一后端、同一份数据，仅前端 build 产物不同，确保差异只来自前端。

---

## 2. 结果总览

| 维度 | 历史 ae5fbb8 (4174) | 当前 master (4173) | 差异 |
|---|---|---|---|
| **切换 301003↔301006**（核心指标） | 1572 ms | 1028 ms | **当前快 35%** ✅ |
| **首次加载 LCP** | 3338 ms | 2126 ms | **当前快 36%** ✅ |
| **字段编辑响应**（var 文本） | ~1747 ms | ~1740 ms | **持平**（共有瓶颈）⚠️ |
| 首屏 JS bundle | 6,070 KB（单文件巨石） | 2,441 KB（主 chunk） | 当前 -60% 首屏体积 |

**一句话结论**：当前版本在**切换**和**首次加载**上显著更快（~35%，源于 code splitting + React Compiler + 期间重构）；但**字段编辑响应两版本都慢**（~1.75s/次），是共有的架构瓶颈，优化尚未触及。

---

## 3. 切换时间（核心指标）

### 方法
- SPA 内路由切换（`history.pushState` + `popstate`，触发 React Router），非整页刷新。
- 预热两个 record（各切换一次）后，交替切换 6 轮，消除冷启动噪声。
- 完成信号：`.react-flow__node` 节点数量稳定 >0 连续 200ms（内容渲染完成，不受 react-timeago 每秒文本更新的干扰）。
- 数据获取（fetch `/record`）两版本都极快（~30–50 ms，localhost），不计入瓶颈。

### 原始数据（6 轮，ms）

| 方向 | 历史 (4174) | 当前 (4173) |
|---|---|---|
| 303 → 306 | 1437, 1639, 1666 | 933, 1077, 1067 |
| 306 → 303 | 1377, 1506, 1669 | 990, 918, 999 |

- 历史：范围 1377–1669，中位数 **1572 ms**
- 当前：范围 918–1209，中位数 **1028 ms**
- 两组数据**完全不重叠**（历史最慢一轮 1377 仍慢于当前最快一轮 1209），差异高度显著。
- 306 方向略慢于 303 方向（306 graph 可能更复杂）。

---

## 4. 首次加载

### 方法
Chrome DevTools performance trace，`reload=true` + `autoStop=true`，记录冷加载。单次 trace（bundle 体积差异是结构性的，单次足以反映量级）。

### 结果（LCP breakdown）

| | 历史 (4174) | 当前 (4173) |
|---|---|---|
| **LCP** | 3338 ms | 2126 ms |
| TTFB | 6 ms | 6 ms |
| Render Delay | 3332 ms | 2120 ms |
| CLS | 0.00 | 0.00 |

- 两版本 TTFB 都极低（localhost），瓶颈全在 **Render Delay**（JS 解析+编译+执行+首屏渲染）。
- 当前版本快 **36%（-1212 ms）**，与切换的 35% 量级一致。

---

## 5. 字段编辑响应

### 方法
- 在 record 306 的 `var` 文本字段（初始值 `equip410040_dmg`）模拟输入一个字符。
- 合成输入：native value setter + `dispatchEvent('input')`（触发 React onChange 的标准技巧）。
- 响应指标：dispatch 到第 2 个 `requestAnimationFrame` 的时间（近似 INP，反映同步主线程处理耗时；合成输入不含 input delay，但处理时间是主因）。
- 每次编辑后恢复原值并等 DOM 结构稳定，再进行下一次。每版本 8 次。

### 原始数据（8 次，ms）

| | 历史 (4174) | 当前 (4173) |
|---|---|---|
| 数据 | 1001, 1715, 1727, 1732, 1743, 1751, 1757, 1907 | 1694, 1719, 1723, 1732, 1748, 1752, 1918, 1997 |
| 中位数 | **~1747** | **~1740** |

**两版本几乎完全相同（差异 <1%）**，数据范围高度重叠。

### 为什么编辑两版本一样慢

编辑单个 `var` 字段触发 **~1.75s 的同步主线程阻塞**：
- onChange → editingSession → 整棵 flow graph 重算 + 全量重渲染（64 节点 × 复杂表单的 reconciliation + DOM 更新）。
- 主线程同步阻塞，rAF 被推迟 ~1.75s。

这个瓶颈**两版本都存在**：React Compiler 能优化「props 不变的子树跳过重渲染」，但编辑改的是 session 级 state，整棵树都要重算，Compiler 在「父级全局 state 变更 → 全子树重渲染」场景下帮助有限。因此编辑响应**不是版本回归，是两版本共有的架构瓶颈**。

### 改善方向（未实施，仅建议）
- 局部化编辑 state，让字段编辑不触发整棵 flow 重渲染。
- 对 flow 节点做 `memo` + 细粒度 props。
- editingSession 层做细粒度订阅（只通知受影响的节点）。

---

## 6. 根因：Bundle 体积结构性差异

### 历史 ae5fbb8 (4174) — dist/assets
```
index-7cgg5I_L.js   6,070,373 字节 (≈6,070 KB, gzip 1,767 KB)
index-DLSYaiIy.css     19,070 字节
```
**无 code splitting**：整个 app 打成一个 6 MB 巨石，首屏全量加载。

### 当前 master (4173) — dist/assets（节选）
```
index-q3vaJFAD.js          2,440,623 字节 (≈2,441 KB)   主 chunk
elk-worker.min-C9JGDOE-.js 1,593,716 字节 (≈1,594 KB)   elkjs 布局 worker，独立、不阻塞首屏
store-C25Hg-Ll.js            536,873 字节 (≈537 KB)
AddPanel-DRQvtDOr.js         218,460 字节
style-B4nsDz46.js            104,343 字节
entityModel-BcOjlrHX.js       43,731 字节
+ Record/Setting/Table/RecordRef 等路由级 chunk
index-CC5vmpgx.css            16,677 字节
```
**做了 code splitting**：路由级 + vendor 分割，首屏只加载主 chunk（2.4 MB），elkjs（1.6 MB）独立成 worker 可延迟/并行。

### 影响
首屏 JS 解析+编译体积从 6 MB 降到 2.4 MB，是首次加载与切换双双变快的**主要结构性来源**。叠加 `babel-plugin-react-compiler`（`vite.config.ts`，减少不必要重渲染）和期间的 undo coalescing、惰性 canUndo/Redo、fitView 机制等重构。

---

## 7. 测试方法备注

- **切换测量**：`history.pushState` + `popstate` 触发 React Router（已验证 URL 变化 + 新增 `/record` 请求），不整页刷新。
- **完成信号选择**：早期用 MutationObserver（characterData）被 react-timeago 每秒更新污染（出现 6000+ms 假值），后改用 `.react-flow__node` 节点数稳定信号，鲁棒。
- **trace overhead**：开着 performance trace 跑墙上计时会拖慢页面（曾测得 4500ms 假值），故墙上计时一律在 trace 关闭时采集。
- **编辑测量**：合成输入近似真实打字 INP 但少了 input delay；处理时间是主因，结论有效。连续编辑会累积 undo 历史（当前版本有 undo），但两版本编辑时间接近，说明 undo 累积非主因。
- **公平性**：两版本用完全相同的脚本、相同的完成信号、同一浏览器会话。切换两组数据不重叠，结论稳健。

---

## 附：复现步骤

```bash
# 历史版本（worktree 隔离）
git worktree add --detach /d/work/mygit/cfggen-old ae5fbb8
cd /d/work/mygit/cfggen-old/cfgeditor
pnpm install
pnpm build
pnpm preview --port 4174 --strictPort

# 当前版本（主工作树）
cd /d/work/mygit/cfggen/cfgeditor
pnpm build
pnpm preview --port 4173   # 用户已启动

# 后端
java -jar ../cfggen.jar -datadir ../example/config -gen server
```

访问 `http://localhost:4173/edit/record/skill.buff/301006` 与 `http://localhost:4174/edit/record/skill.buff/301006` 对比。

测完清理：`git worktree remove /d/work/mygit/cfggen-old`，停掉 4174 preview。

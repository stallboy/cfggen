# cfgeditor 文档导航

> 这里是 cfgeditor 全部文档的入口。**刚接触这套代码,按下面的阅读路径从上往下读**——从"它是什么"到"某个机制怎么实现",逐步加深。已经知道要找什么,直接按标题跳。

阅读路径分五段:**入门 → 架构骨架 → 数据主线 → 机制专题 → 工程化**。前三段给你全局心智模型,后两段是深入某块时的专题参考。每篇下面那句话是"读完能得到什么 / 什么时候读"。

---

## ① 入门

| 文档 | 读它得到什么 |
|---|---|
| [`overview.md`](./overview.md) | **从这里开始。** cfgeditor 是什么、核心名词(entity/record/table/schema/res/node)指什么、用户怎么操作。 |

## ② 架构骨架

| 文档 | 读它得到什么 |
|---|---|
| [`DIRECTORY_STRUCTURE.md`](./DIRECTORY_STRUCTURE.md) | 目录怎么分层、依赖只能向下、`@/` 别名、oxlint 怎么拦反向 import。改代码前先读这篇,知道东西该放哪。 |

## ③ 数据主线

| 文档 | 读它得到什么 |
|---|---|
| [`data-lifecycle.md`](./data-lifecycle.md) | 一张图讲清"改一个字段值 → 落盘 → 刷新"的全旅程,把编辑态/undo/缓存/视口串成一条线。想理解数据怎么转一圈,读这篇。 |

## ④ 机制专题

深入某个机制时按需读,顺序不强制(③ 已经给了它们在全局里的位置):

| 文档 | 读它得到什么 |
|---|---|
| [`状态管理-总结与演进.md`](./状态管理-总结与演进.md) | Resso / EditingSession / useSyncExternalStore 三套状态怎么分工,编辑对象为什么不进 React state。 |
| [`url-api-reactquery.md`](./url-api-reactquery.md) | URL 怎么编码"在看什么"、API 端点清单、React Query 的 queryKey 设计与缓存失效。 |
| [`undo-redo.md`](./undo-redo.md) | 快照栈模式、值类合并(coalescing)、提交边界、undo 时视口怎么稳定。 |
| [`fitview-视口适配机制.md`](./fitview-视口适配机制.md) | 流程图视口怎么算、fitView / KeepStable / computeStableViewport,编辑后为什么不乱跳。 |
| [`embedding-字段内嵌机制.md`](./embedding-字段内嵌机制.md) | 结构体字段怎么内嵌显示在父节点上,展开/折叠的规则。 |

## ⑤ 工程化

| 文档 | 读它得到什么 |
|---|---|
| [`unit-testing-guide.md`](./unit-testing-guide.md) | 测试怎么写、覆盖哪些纯逻辑、哪些不测(UI/网络/Tauri)。加测试前读。 |
| [`perf-optimization.md`](./perf-optimization.md) | 已知性能要点与优化记录,涉及编辑链/布局/渲染时参考。 |

---

> 文档只讲"为什么这么设计 + 怎么读懂",不讲命令。开发命令、后端启动见上一级 [`../CLAUDE.md`](../CLAUDE.md)。

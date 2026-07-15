# cfggen 源码与设计导读

本系列是 **`app/`（cfggen，Java 配置生成器）** 的开发者向源码与设计文档，目标是帮读者**读懂、敢改**这套代码，并理解关键设计决策的**为什么**。

> 这是**开发者向**文档，不是用户文档。
> - 想知道**怎么用** cfggen（写 `.cfg`、跑命令、配 i18n）→ 看[用户文档站点](../../docs/src/content/docs/)，尤其 `core/` 分类。
> - 想要 AI / 在仓库内**速查硬事实**（构建命令、`-tool`/`-gen` 注册表、命名规范）→ 看 [`../CLAUDE.md`](../CLAUDE.md)。
> - 想**理解源码与设计** → 你在正确的地方。

## 阅读顺序

1. **先读 [`01-architecture-overview.md`](01-architecture-overview.md)** —— 全局主干：四层流水线、以 `Context` 为中心的模块地图、核心设计原理。其余每篇都建立在这之上。
2. 再按关注点挑读：

| 篇目 | 讲什么 |
|---|---|
| [02-schema-and-cfg](02-schema-and-cfg.md) | schema 模型 + CFG 解析器内部；为何 schema/data/value 三分 |
| [03-data-reading](03-data-reading.md) | Excel/CSV/JSON 读取；FastExcel vs POI；并发读取 |
| [04-value-model](04-value-model.md) | 值层；外键解析；为何值独立成层 |
| [05-codegen-and-extension](05-codegen-and-extension.md) | Generator/JteEngine；各语言生成族；如何加新语言 |
| [06-bytes-format](06-bytes-format.md) | 二进制配置格式；池/小端序/多语言 |
| [07-write-back-and-servers](07-write-back-and-servers.md) | write 模块 + editorserver + mcpserver 写回管道 |
| [08-errors-and-validation](08-errors-and-validation.md) | 错误收集机制；verify/search 校验 |
| [09-i18n](09-i18n.md) | 国际化双模式；geni18n；翻译工具 |
| [10-dev-workflow](10-dev-workflow.md) | 构建/运行/调试/改模板/性能 profile |

## 写法约定

- **语言**：中文叙述，代码标识符保留英文。
- **引用源码**：用**相对路径 + 符号名**，不写行号（行号会漂移）。例如：`Context`（见 `../src/main/java/configgen/ctx/Context.java`）。从本目录出发，源码在 `../src/main/java/configgen/`。
- **代码摘录**：默认只链接不粘贴；确需引用时贴 < 10 行并标注来源文件。
- **图**：用 mermaid 代码块画数据流 / 模块关系。
- **不写逐类清单**：写"职责 / 流程 / 不变量"；必须列时用"关注点 → 主类"小表（稳定），不穷举。要查实际有哪些类，用 Glob（如 `../src/main/java/configgen/schema/*.java`），别依赖文档里的枚举。

## 时效性

文档会滞后于代码。若文中符号名 / 路径对不上实际代码，**以代码为准**，用 Glob / Grep 重新定位。本系列刻意避免会漂移的内容（逐文件清单、精确行号），正是为此。

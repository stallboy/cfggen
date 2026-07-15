# CLAUDE.md

本文件为 Claude Code 在本仓库工作时提供指引。

> **架构与设计深读**：见 [`docs/`](docs/) 系列（从 [`docs/README.md`](docs/README.md) 起）。本文件只保留 AI / 在仓库内速查的**硬事实**；架构、数据流、Bytes 格式、扩展开发、错误排查等细节已迁入该系列。

## 项目概述

Java 配置生成器（cfggen）：从 Excel/CSV/JSON 读取配置数据，生成 Java/C#/TypeScript/Go/Lua/GDScript 等多语言的读表代码与数据文件。

## 构建和开发

### 构建命令
```bash
./gradlew.bat build                          # 构建
./gradlew.bat test                           # 测试
./gradlew.bat test --tests "完整测试类名"     # 单个测试类
./gradlew.bat jacocoTestReport               # 覆盖率报告
./gradlew.bat fatJar                         # 可执行 jar → build/libs/cfggen.jar
./gradlew.bat compileJava / compileTestJava / clean
```

### 开发环境
- **Java 25**，Gradle。主要依赖：FastExcel、POI、FastJSON2、JTE、ANTLR、Simple-OpenAI、MCP SDK。
- 默认用 **FastExcel** 读 Excel（性能高于 POI）。

### 脚本工具
- `genjar.bat`（仓库根）：便捷打包，但 **Windows cmd 语法**（`call`/`copy`/`pause`），**Git Bash 下不工作** → 用 `./gradlew.bat fatjar` 代替。
- `app/script/mkexe.bat`：生成 Windows 可执行文件。

### 工具模块（`-tool`）
| 注册名 | 类 | 说明 |
|---|---|---|
| `xmltocfg` | `XmlToCfgTool` | XML 转 CFG |
| `fastexcelcheck` | `ExcelReadDiffTool` | FastExcel vs POI 读取比对 |
| `bytesview` | `BytesViewTool` | Bytes 文件查看 |
| `schematocsv` | `SchemaToCsvTool` | Schema 导出 CSV |
| `term` | `TodoTermListerAndChecker` | 待译词条列出与术语一致性检查 |
| `translate` | `TodoTranslator` | AI 辅助翻译 |

> `verify`/`search` 在 `tool` 包但通过 `-gen` 注册（见下）。`Help`（`-h`）由 `Main` 内部调用，不在注册表。

## 代码架构

端到端流水线、设计原理详见 [`docs/01-architecture-overview.md`](docs/01-architecture-overview.md)。模块速查（要查具体类用 Glob）：

| 包 | 职责 | 入口符号 |
|---|---|---|
| `gen` | 命令行 / 插件注册 | `Main`、`Tools`、`Generators` |
| `ctx` | 上下文 / 协调 / 缓存 | `Context`、`DirectoryStructure` |
| `schema` | 类型系统 / CFG 文法 | `CfgSchemas`、`CfgSchema`、`CfgSchemaResolver`、`schema/cfg/CfgReader` |
| `data` | 数据读取（含 schema↔data 对齐） | `CfgDataReader`、`ReadByFastExcel`、`CfgSchemaAlignToData` |
| `value` | 值模型 / 外键校验 | `CfgValue`、`CfgValueParser`、`RefValidator` |
| `gen*` | 各语言代码生成 | `Generator` 基类 + `genjava`/`gencs`/`gengo`/`genlua`/`gents`/`gengd`/`genjson` |
| `genbytes` | 二进制格式 | `BytesGenerator`（详见 [`docs/06`](docs/06-bytes-format.md)） |
| `i18n` / `geni18n` | 国际化 | `LangTextFinder`、`LangSwitchable`、`I18nBy*Generator` |
| `write` + `editorserver` + `mcpserver` | 写回管道（编辑器/AI→文件） | `VTableStorage`、`EditorServer`、`CfgMcpServer`（详见 [`docs/07`](docs/07-write-back-and-servers.md)） |
| `tool` | 校验 / 检索 | `ValueVerifyTool`、`ValueInspectTool` |
| `util` | 模板 / 缓存输出 / 日志 | `JteEngine`、`CachedFiles`、`Logger` |

### 生成器模块（`-gen`，括号内为注册名）
在 `Main.registerAllProviders` 注册。`java`/`cs`/`ts`/`go`/`lua`/`gd`/`json`/`bytes` 为代码/数据生成；`byai`(AI 辅助)/`tsschema`(导出 TS schema)；`i18n`/`i18nbyid` 国际化；`verify`/`search` 校验检索（概念是工具、注册在 `Generators`）；`server`/`mcpserver` 服务。

## 使用方式
```bash
java -jar cfggen.jar -h                                # 帮助（无参也默认打印）
java -jar cfggen.jar -gui                              # GUI 可视化拼命令
java -jar cfggen.jar -datadir <dir> -gen <name>[,k=v...]
java -jar cfggen.jar -datadir <dir> -tool <name>[,k=v...]
```
`-gen` 时 `-datadir` 必填（须含 `config.cfg`）。常用选项：`-locale`、`-v`/`-vv`、`-p`/`-pp`、`-nowarn`/`-weakwarn`、`-headrow`(默认 2)、`-encoding`(默认 GBK，带 BOM 的 UTF-8 自动识别)、`-asroot`/`-exceldirs`/`-jsondirs`、`-i18nfile`/`-langswitchdir`/`-defaultlang`（前两者互斥，默认语言默认 `zh_cn`）。参数顺序任意；子参数用 `,` 分隔、`=` 或 `:` 赋值。

## 配置 Schema
自定义 CFG 格式：struct / interface / table / enum、list / map、外键 `->`(单值) / `=>`(多值 list)、tag。**语法**见用户站点 [`../docs/src/content/docs/core/schema.mdx`](../docs/src/content/docs/core/schema.mdx)；**解析器内部**见 [`docs/02-schema-and-cfg.md`](docs/02-schema-and-cfg.md)。

## 开发注意事项（精华）
1. **JTE 模板**：构建期 `jte{generate()}` 预编译进 jar，运行时 `createPrecompiled` 直接加载 class，省每次启动 ~1.6s；开发期 `./gradlew.bat run` 回退动态编译并缓存到临时目录。**改模板后须重新 `fatjar`** 才能让预编译 class 进 jar。
2. **并发**：schema 读 / data 读 / value 解析 / 代码生成均已并发化（工作窃取池 + `invokeAll`）；生成器用 `ThreadLocal` 隔离打印机缓冲，`interface`+`impl` 同路径时任务内保序。被超大表封顶时只能做表内行级并发。
3. **性能测量**：看 `-p` 的**工作秒 / 分配量**，不要看墙上时间（serverdev 场景墙上噪声 ±50%），或用 JFR 对比。
4. **错误**：收集式（`CfgSchemaErrs` 三级 / `CfgValueErrs` 两级），结尾 `checkErrors(prefix, allowErr)` 决定是否抛。详见 [`docs/08`](docs/08-errors-and-validation.md)。
5. **命名**：生成器 `XxxCodeGenerator`、工具 `XxxTool`、工具类 `XxxUtil`、序列化器/反序列化器独立成类。
6. **GDScript**：`var x: Array[int]: get: return x` 是 Godot 4.x 标准属性语法，引擎自动处理，**不是无限递归**，勿误改。

> 添加生成器、调试、改模板、错误排查、Bytes 格式、写回管道等**详细流程**已迁入 `docs/` 系列（[`05`](docs/05-codegen-and-extension.md) / [`10`](docs/10-dev-workflow.md) / [`08`](docs/08-errors-and-validation.md) / [`06`](docs/06-bytes-format.md) / [`07`](docs/07-write-back-and-servers.md)）。

## 测试和验证
JUnit 5 + JaCoCo（csv / xml / html）。CI 里可用 `-gen verify` 当配置引用完整性门禁。

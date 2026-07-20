# 更新日志

本项目采用 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 标准，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。


### [v1.4.0] - 2026-07-20

速度优化与编辑器 undo/redo。

#### Added
- 编辑器 undo/redo：撤销/重做按钮与快捷键，值类编辑合并提交，结构操作保持视口稳定（EFitView.KeepStable）
- 大小写不敏感的 impl 去重检测：impl 之间、impl 与 interface 同名（忽略大小写）时报冲突
- GDScript（Godot）代码生成
- Schema 级别 Enum 类型支持，零代码生成器改动
- C# 适配 .NET 9.0 与 Unity，使用 FrozenDictionary
- gen_run.bat：一次性运行全部 example
- Chat 面板显示当前模型名；table 切换时 url 附带上次选中记录 id

#### Changed
- 多语言代码生成并发化，生成耗时显著下降：
  - Java：struct/table 循环并发，~2.8x（0.51→0.18s）
  - C#：render 循环并发，~2.3x（0.23→0.10s）
  - Go / GD：仿 Java/C# 并发化
  - Lua：表生成并发，~1.4x（0.88→0.63s）
- JTE 模板预编译 + schema 并行解析，整体生成耗时 -36%
- i18n 各语言 xlsx 并行读取
- 内存分配优化：hashCode 去 varargs、keyMap 容量预分配，总分配 -15%
- 统一 bytes 序列化格式，多语言共享同一结构与小端字节序
- 编辑器大表/大图渲染优化：schema select 稳定化、列表虚拟滚动、代码分割、elk 布局移入 Web Worker
- cfggen 无参启动改为打印帮助（原为启动 GUI）

#### Fixed
- cfggen：Context 缓存竞态与 allowErr 缓存污染
- DirectoryStructure.findTableToJsonFiles：同名表跨目录（如 _skill_buff / skill/_buff）编辑后落盘位置变更，导致下次启动失败

### [v1.3.0] - 2026-02-05

#### Added
- GenVerifier：检查每个 table 中未被引用的 record
- ValueRefInCollector：支持 RefUniq 和 RefList 引用类型
- Metadata `root` 标记：标记根节点表，在未引用检测时忽略
- GuiLauncher：可视化配置生成参数的图形界面
- Claude Code 插件：`/gen-schema` 命令，通过自然语言生成 schema
- 未引用记录查看功能：在配置编辑器中查看未被引用的记录
- CFG 语法增强：支持在 struct、table、interface、field 之前添加行注释

#### Changed
- Java 代码生成：新增 `configgenDir` 参数，自动复制核心 Java 源文件到指定目录
- 字段内嵌显示：简化复杂嵌套结构的展示
- 国际化：补充所有实现 `Msg` 接口的错误/警告类的国际化消息
- API 重构：将 RESTful API 和相关模型移至 `api` 目录
- Store 重构：将 historyModel 移至 `store` 目录

#### Fixed
- C# enumref 生成错误
- Chat 相关功能问题
- 实体表单编辑时的值传递问题
- 未保存记录的检测问题

#### Removed
- BuildSetting 类和 `-usepoi` 参数

### [v1.1.0] - 2025-04-03

#### Added
- MCP 服务器：为 AI 生成配置提供支持
- AI 聊天辅助配置功能：在编辑器中集成 AI 对话
- AI 翻译功能：TodoTranslator 工具
- 结构化数据返回：MCP 服务器返回结构化 schema
- 表结构 schema 读取 API
- 可视化节点配置：可配置节点颜色和可视化设置
- 节点折叠/内嵌显示：简化复杂结构的展示

#### Changed
- 编辑器 UI 结构重构
- GUI locale 处理优化
- RESTful API 和相关模型重构
- 字段内嵌显示逻辑优化
- 节点渲染性能提升

#### Fixed
- 多项编辑器显示和交互问题
- 未保存记录的提示问题

### [v1.0.0] - 2023-10-20

#### Added
- CFG 配置文件解析器：支持 struct、interface、table、list、map 等数据结构
- 多数据源支持：Excel、CSV、JSON
- 多语言代码生成：
  - Java：支持 sealed 类、完整的类型安全访问
  - C#：.NET 平台配置代码
  - TypeScript：前端和 Node.js 的类型化配置
  - Go：Go 语言的结构体生成
  - Lua：Lua 表格式，注重内存大小
- JSON 数据生成器
- 外键引用完整性检查：单向外键（->）和多向外键（=>）
- 编辑器服务器：提供 RESTful API 支持配置编辑器
- 命令行界面：灵活的参数配置
- 数据验证：Schema 验证和数据对齐
- 数据统计：配置数据的使用情况分析
- 并发读取：使用工作窃取线程池优化数据读取性能
- 模板引擎：集成 JTE 模板引擎，支持热加载

#### Changed
- 优化缓存机制
- 支持多级外键引用
- 支持配置过滤和标签

# 更新日志

本项目采用 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/) 标准，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。


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
- API 优化：使用 `maxObjs` 和 `noRefIn` 参数替代 `depth` 参数

#### Fixed
- C# enumref 生成错误
- Chat 相关功能问题
- 实体表单编辑时的值传递问题
- 未保存记录的检测问题

#### Removed
- BuildSetting 类和 `-usepoi` 参数
- `depth` 参数（改用 `maxObjs` 和 `noRefIn`）

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

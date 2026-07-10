# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个Java配置生成器（cfggen），用于从多种数据源（Excel、CSV、JSON）读取配置数据，生成多种编程语言（Java、C#、TypeScript、Go、Lua、GDScript等）的配置代码和数据文件。

## 构建和开发

### 构建命令
```bash
# 构建项目
./gradlew.bat build

# 运行测试
./gradlew.bat test

# 运行单个测试类
./gradlew.bat test --tests "完整测试类名"

# 生成覆盖率报告
./gradlew.bat jacocoTestReport

# 创建可执行jar包
./gradlew.bat fatJar

# 编译Java代码
./gradlew.bat compileJava

# 编译测试代码
./gradlew.bat compileTestJava

# 清理构建
./gradlew.bat clean
```

### 开发环境
- **Java版本**: 25
- **构建工具**: Gradle
- **主要依赖**: FastExcel、POI、FastJSON2、JTE模板引擎、ANTLR、Simple-OpenAI、MCP SDK

### 构建选项
- 默认使用FastExcel库读取Excel文件，性能更高

### 脚本工具
- `genjar.bat`：生成可执行JAR文件
- `app/script/mkexe.bat`：生成Windows可执行文件（包含在cfggen.zip中）

### 工具模块（通过 `-tool` 调用）
- **`configgen.tool.XmlToCfgTool`** (`xmltocfg`) - XML 转 CFG 格式
- **`configgen.tool.ExcelReadDiffTool`** (`fastexcelcheck`) - Excel 读取差异比较（FastExcel vs POI）
- **`configgen.tool.BytesViewTool`** (`bytesview`) - Bytes 文件查看
- **`configgen.tool.SchemaToCsvTool`** (`schematocsv`) - Schema 导出为 CSV
- **`configgen.geni18n.TodoTermListerAndChecker`** (`term`) - 待翻译词条列出与检查
- **`configgen.geni18n.TodoTranslator`** (`translate`) - AI 辅助翻译待译词条

> 注：`configgen.gen.Help`（`-h` 打印帮助）不在 `-tool`/`-gen` 注册表里，由 `Main` 内部在 `-h`、未知参数或缺少 `-datadir` 时直接调用；`ValueVerifyTool`/`ValueInspectTool` 虽在 `configgen.tool` 包下，但实际通过 `-gen verify`/`-gen search` 注册（见生成器模块）。

## 代码架构

### 核心模块
- **`configgen.gen.Main`** - 主程序入口，命令行参数解析和生成器/工具注册
- **`configgen.gen.Generator`** - 抽象基类，所有生成器（`-gen`）继承此类
- **`configgen.gen.Tool`** - 抽象基类，所有工具（`-tool`）继承此类
- **`configgen.gen.ui`** - GUI 启动器（`GuiLauncher`），由 `-gui` 参数触发，可视化拼装命令行
- **`configgen.ctx.Context`** - 全局上下文管理，协调所有组件，提供缓存机制
- **`configgen.schema`** - 配置 schema 定义和解析（`CfgSchema`、`CfgSchemas`、`StructSchema`、`InterfaceSchema` 等）
- **`configgen.schema.cfg`** - CFG 文本格式的 ANTLR 文法解析（`Cfg.g4`、`CfgParser`、`CfgReader`、`CfgWriter`、`XmlReader`）
- **`configgen.data`** - 数据读取模块（Excel/CSV/JSON），支持并发读取（ForkJoinPool）
- **`configgen.value`** - 配置值模型和解析，处理外键关联和国际化
- **`configgen.i18n`** - 国际化支持，支持直接文本替换和语言切换模式
- **`configgen.write`** - 写入 excel 和 json
- **`configgen.util`** - 工具类，包括 JTE 模板引擎封装（`JteEngine`）、缓存输出流（`CachedFiles`/`CachedIndentPrinter`）等

### 生成器模块（通过 `-gen` 调用，括号内为注册名）
所有生成器继承自抽象基类 `configgen.gen.Generator`，在 `Main.registerAllProviders` 中注册。
- **`configgen.genjava.code.JavaCodeGenerator`** (`java`) - Java 代码和数据生成，支持 sealed 类
- **`configgen.gencs.CsCodeGenerator`** (`cs`) - C# 代码生成，支持 .NET 平台
- **`configgen.gents.TsCodeGenerator`** (`ts`) - TypeScript 代码生成，支持前端项目
- **`configgen.gengo.GoCodeGenerator`** (`go`) - Go 代码生成，支持 Go 语言结构体
- **`configgen.genlua.LuaCodeGenerator`** (`lua`) - Lua 代码生成，支持 Lua 表格式
- **`configgen.gengd.GdCodeGenerator`** (`gd`) - GDScript 代码生成（Godot 引擎）
- **`configgen.genjson.JsonGenerator`** (`json`) - JSON 数据生成，通用数据格式
- **`configgen.genbytes.BytesGenerator`** (`bytes`) - 二进制配置文件生成，支持运行时动态加载
- **`configgen.genbyai.ByAIGenerator`** (`byai`) - AI 辅助生成配置
- **`configgen.genbyai.TsSchemaGenerator`** (`tsschema`) - 导出 TypeScript schema（供 AI/前端使用）
- **`configgen.geni18n.I18nByValueGenerator`** (`i18n`) - 按原始文本值生成国际化文件
- **`configgen.geni18n.I18nByIdGenerator`** (`i18nbyid`) - 按 ID 生成国际化文件
- **`configgen.tool.ValueVerifyTool`** (`verify`) - 配置数据校验（外键引用完整性等）
- **`configgen.tool.ValueInspectTool`** (`search`) - 配置数据检索检查

### 服务模块
- **`configgen.editorserver`** (`server`) - 编辑器服务，提供 RESTful API 支持配置编辑器（cfgeditor）：schema 查询、记录增删改查、引用查询、笔记、JSON 校验等
- **`configgen.mcpserver`** (`mcpserver`) - MCP 服务，为 AI 生成配置提供支持：`SchemaTool`、`ReadRecordTool`、`WriteRecordTool`、`SearchTool`

### 架构流程
```
1. 命令行参数解析 (Main.main0)
   ↓
2. 创建Context并初始化
   ├── 读取Schema定义 (CfgSchemas.readFromDir)
   ├── 解析Schema关系 (CfgSchema.resolve)
   ├── 读取数据文件 (CfgDataReader.readCfgData)
   └── 对齐Schema和数据 (CfgSchemaAlignToData)
   ↓
3. 生成器执行 (Generator.generate)
   ├── 创建配置值 (Context.makeValue)
   │   ├── 过滤Schema (CfgSchemaFilterByTag)
   │   ├── 解析值 (CfgValueParser.parseCfgValue)
   │   └── 国际化处理 (TextValue.setTranslatedForTable)
   │
   └── 使用模板生成代码 (JteEngine.render)
       ├── 读取模板文件
       ├── 填充数据模型
       └── 写入目标文件
```

## Bytes 文件格式

**`configgen.genbytes.BytesGenerator`** 生成二进制配置文件，支持运行时动态加载和多语言。

### 文件结构

```
┌─────────────────────────────────────┐
│ 1. Schema 长度标记（int）            │ ← 0 = 无schema，>0 = 有schema
│    [若长度>0] Schema 数据             │
├─────────────────────────────────────┤
│ 2. StringPool（字符串池）             │
├─────────────────────────────────────┤
│ 3. LangTextPool（多语言文本池）       │
├─────────────────────────────────────┤
│ 4. 表数据（所有配置表）               │
└─────────────────────────────────────┘
```

### 关键类

| 类名 | 职责 |
|------|------|
| `BytesGenerator` | 主生成器，协调各组件 |
| `CfgValueSerializer` | 序列化配置值 |
| `MultiLangTableSerializer` | 多语言表序列化 |
| `StringPool` | 字符串去重池 |
| `TextPool` | 单语言文本池 |
| `LangTextPool` | 多语言文本池管理 |
| `SchemaSerializer` | Schema序列化器 |
| `SchemaDeserializer` | Schema反序列化器 |
| `ConfigOutput` | 小端序输出流 |
| `ConfigInput` | 小端序输入流 |

### 读取示例

参考现有实现：
- Java序列化：`configgen.genbytes.*`
- Java反序列化：`example/java_ls/configgen/genjava/*`（完整示例）

## 使用方式

### 基本用法
```bash
# 完整形式（-datadir 在使用生成器时必填）
java -jar cfggen.jar [options] [-tool toolname[,param=value...]] -datadir <dir> [-gen genname[,param=value...]]

# 仅打印帮助（无参数时也默认打印帮助）
java -jar cfggen.jar -h

# 启动 GUI 可视化拼装命令行
java -jar cfggen.jar -gui
```

### 常用选项
| 选项 | 说明 |
|------|------|
| `-locale <lang>` | 界面语言，默认系统 locale |
| `-v` / `-vv` | 详细日志级别 1 / 2（统计、警告、附加信息） |
| `-p` / `-pp` | 性能 profile（内存与耗时）/ profile 前 GC |
| `-nowarn` / `-weakwarn` | 关闭警告 / 开启弱警告 |
| `-datadir <dir>` | 配置数据目录，须含 `config.cfg`（使用生成器时必填） |
| `-headrow <n>` | csv/txt/excel 表头行类型，默认 `2` |
| `-encoding <enc>` | csv/txt 编码，默认 `GBK`；带 BOM 头时自动识别 |
| `-asroot` / `-exceldirs` / `-jsondirs` | 显式数据源目录（标签映射、Excel 目录、JSON 目录） |
| `-i18nfile` / `-langswitchdir` / `-defaultlang` | 国际化（与 `-langswitchdir` 二选一），默认语言默认 `zh_cn` |

参数顺序任意，但使用生成器时必须提供 `-datadir`；tool/gen 内的子参数用 `,` 分隔，键值用 `=` 或 `:` 分隔。

## 配置Schema定义

项目使用自定义的CFG格式定义配置结构，支持：
- **基础类型**：bool、int、float、long、str、text
- **结构体**：struct定义复合数据类型
- **接口**：interface支持多态行为
- **容器类型**：list、map
- **数据表**：table存储配置数据
- **外键关联**：->单向外键、=>多向外键

## 开发注意事项

1. **数据读取**：默认使用FastExcel库
2. **编码处理**：CSV文件默认使用GBK编码，支持带BOM的UTF-8，自动检测编码
3. **并发处理**：数据读取使用工作窃取线程池（ForkJoinPool）优化性能
4. **缓存机制**：使用缓存文件输出流（CachedIndentPrinter）提高生成性能
5. **错误处理**：完善的错误收集和验证机制（CfgSchemaErrs, CfgValueErrs）
6. **国际化**：支持两种模式 - 直接文本替换（i18nfile）和语言切换（langswitchdir）
7. **外键关联**：支持单向外键（->）和多向外键（=>），自动验证引用完整性
8. **模板引擎**：JTE 模板在构建期预编译进 jar（见 `build.gradle` 的 `jte{generate()}` 与 `normalizeJteLineEndings` 任务），运行时通过 `TemplateEngine.createPrecompiled` 直接加载 class，省去每次启动 ~1.6s 的模板编译开销；开发期直接 `./gradlew.bat run`（无预编译产物）时回退为运行时动态编译并缓存到临时目录。AI 生成/翻译走 `JteEngine.renderTryFileFirst`，优先用工作区文件系统模板，否则回退到内置资源
9. **参数解析**：统一的Parameter接口，支持嵌套参数和国际化描述
10. **性能优化**：Schema和数据对齐避免重复解析，缓存计算结果
11. **命名规范**：
    - 生成器类：`XxxCodeGenerator`（如 `JavaCodeGenerator`）
    - 工具类：`XxxTool`（如 `ValueVerifyTool`）
    - 工具类：`XxxUtil`（如 `FileUtil`、`StringUtil`）
    - Schema类：序列化器/反序列化器独立为单独类（如 `SchemaSerializer`、`SchemaDeserializer`）

## 测试和验证

- 使用JUnit 5进行单元测试
- 支持JaCoCo代码覆盖率报告

## 扩展开发

### 添加新生成器
1. 继承抽象类 `configgen.gen.Generator`
2. 在`Main.java`中注册生成器：`Generators.addProvider("newlang", NewLangGenerator::new)`
3. 使用JTE模板引擎生成代码，模板文件放在`src/main/resources/jte/`目录下
4. 支持参数解析：通过`Parameter`接口获取生成器参数（格式：`-gen newlang,dir=xxx,pkg=xxx`）

### 模板引擎使用
- 模板位置：`src/main/resources/jte/`
- 渲染示例：

  ```
  try (CachedIndentPrinter ps = createCode(outputFile, encoding)) {
      JteEngine.render("java/ConfigMgr.jte",
              Map.of("pkg", packageName, "mapsInMgr", maps), ps);
  }
  ```
- 模板语法：使用JTE模板语法，支持循环、条件、参数传递

### 参数解析机制
- 两层参数解析：主程序参数和生成器参数
- `Parameter`接口统一参数访问
- 参数格式：`-gen java,dir=config,pkg=config.gen,encoding=UTF-8`

项目采用插件化设计，易于扩展新的生成器和数据源支持。

## 常见开发任务

### 调试生成器
1. 设置断点在`Main.main0`或具体生成器的`generate`方法
2. 使用示例配置目录：`example/`
3. 运行命令：`java -jar cfggen.jar -datadir example -gen java`

### 修改模板
1. 模板文件位置：`src/main/resources/jte/`或文件系统自定义位置
2. 修改后须重新构建（`genjar.bat` / `./gradlew.bat fatJar`）才能让预编译 class 进 jar 生效；开发期 `./gradlew.bat run` 走动态编译可立即看到改动
3. 测试模板修改：运行生成器查看输出

### 错误排查
1. 查看错误收集：`CfgSchemaErrs.getErrors()`, `CfgValueErrs.getErrors()`
2. 启用详细日志：添加调试参数或修改日志级别
3. 验证数据完整性：使用`-gen verify`参数

## GDScript 开发注意事项

### 属性定义
GDScript 新版本（Godot 4.x）允许以下属性定义方式，**不会造成无限递归**：

```gdscript
var actionID: Array[int]:
    get:
        return actionID  # 正确！Godot 会自动处理
```

这是 Godot 4.x 的标准属性定义语法，不要误认为是错误。

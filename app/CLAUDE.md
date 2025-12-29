# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

这是一个Java配置生成器（configgen），用于从多种数据源（Excel、CSV、JSON）读取配置数据，生成多种编程语言（Java、C#、TypeScript、Go、Lua等）的配置代码和数据文件。

## 构建和开发

### 构建命令
```bash
# 构建项目
gradle build

# 运行测试
gradle test

# 运行单个测试类
gradle test --tests "完整测试类名"

# 生成覆盖率报告
gradle jacocoTestReport

# 创建可执行jar包
gradle fatJar

# 编译Java代码
gradle compileJava

# 编译测试代码
gradle compileTestJava

# 清理构建
gradle clean
```

### 开发环境
- **Java版本**: 21
- **构建工具**: Gradle
- **主要依赖**: FastExcel、POI、FastJSON2、JTE模板引擎、ANTLR、Simple-OpenAI、MCP SDK

### 构建选项
- 默认使用FastExcel库读取Excel文件，性能更高

### 脚本工具
- `genjar.bat`：生成可执行JAR文件
- `mkexe.bat`：生成Windows可执行文件（包含在cfggen.zip中）

## 代码架构

### 核心模块
- **`configgen.gen.Main`** - 主程序入口，命令行参数解析和生成器注册
- **`configgen.ctx.Context`** - 全局上下文管理，协调所有组件，提供缓存机制
- **`configgen.schema`** - 配置schema定义和解析，使用ANTLR解析CFG格式
- **`configgen.data`** - 数据读取模块（Excel、CSV、JSON），支持并发读取
- **`configgen.value`** - 配置值模型和解析，处理外键关联和国际化
- **`configgen.i18n`** - 国际化支持，支持直接文本替换和语言切换模式
- **`configgen.write`** - 写入excel和json
- **`configgen.util`** - 工具类，包括JTE模板引擎封装

### 生成器模块
- **`configgen.genjava`** - Java代码和数据生成，支持sealed类
- **`configgen.gencs`** - C#代码生成，支持.NET平台
- **`configgen.gents`** - TypeScript代码生成，支持前端项目
- **`configgen.gengo`** - Go代码生成，支持Go语言结构体
- **`configgen.genlua`** - Lua代码生成，支持Lua表格式
- **`configgen.genjson`** - JSON数据生成，通用数据格式

### 服务模块
- **`configgen.editorserver`** - 编辑器服务，提供RESTful API支持配置编辑器
- **`configgen.mcpserver`** - MCP服务，为AI生成配置提供支持

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
   ├── 使用模板生成代码 (JteEngine.render)
   │   ├── 读取模板文件
   │   ├── 填充数据模型
   │   └── 写入目标文件
   │
   └── 复制支持文件 (Generator.copySupportFileIfNotExist)
```

## 使用方式

### 基本用法
```bash
java -jar cfggen.jar [tools] -datadir [dir] [options] [gens]
```

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
8. **模板引擎**：JTE模板支持热加载，默认从资源文件加载模板。部分功能（AI生成、翻译）支持文件系统模板优先于资源文件
9. **参数解析**：统一的Parameter接口，支持嵌套参数和国际化描述
10. **性能优化**：Schema和数据对齐避免重复解析，缓存计算结果

## 测试和验证

- 使用JUnit 5进行单元测试
- 支持JaCoCo代码覆盖率报告

## 扩展开发

### 添加新生成器
1. 实现`configgen.gen.Generator`接口
2. 在`Main.java`中注册生成器：`Generators.addProvider("newlang", GenNewLanguage::new)`
3. 使用JTE模板引擎生成代码，模板文件放在`src/main/resources/jte/`目录下
4. 支持参数解析：通过`Parameter`接口获取生成器参数（格式：`-gen newlang,dir=xxx,pkg=xxx`）

### 添加新数据源
1. 扩展`configgen.data.CfgDataReader`
2. 在`configgen.ctx.ExplicitDir`中配置目录映射
3. 实现数据读取接口，支持并发读取

### 模板引擎使用
- 模板位置：`src/main/resources/jte/`
- 渲染示例：
  ```java
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
2. 修改后无需重新编译，JTE支持热加载
3. 测试模板修改：运行生成器查看输出

### 错误排查
1. 查看错误收集：`CfgSchemaErrs.getErrors()`, `CfgValueErrs.getErrors()`
2. 启用详细日志：添加调试参数或修改日志级别
3. 验证数据完整性：使用`-gen verify`参数
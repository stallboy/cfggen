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

# 生成覆盖率报告
gradle jacocoTestReport

# 创建可执行jar包
gradle fatJar

# 编译Java代码
gradle compileJava

# 编译测试代码
gradle compileTestJava
```

### 开发环境
- **Java版本**: 21
- **构建工具**: Gradle
- **主要依赖**: FastExcel、POI（可选）、FastJSON2、JTE模板引擎、ANTLR

### 构建选项
- `-PnoPoi`：构建时不包含POI库，减小jar包大小（从20M降至2M）
- 默认使用FastExcel库读取Excel文件，性能更高

## 代码架构

### 核心模块
- **`configgen.gen.Main`** - 主程序入口，命令行参数解析
- **`configgen.ctx.Context`** - 上下文管理，协调所有组件
- **`configgen.schema`** - 配置schema定义和解析
- **`configgen.data`** - 数据读取模块（Excel、CSV、JSON）
- **`configgen.value`** - 配置值模型和解析
- **`configgen.i18n`** - 国际化支持
- **`configgen.write`** - 写入excel和json

### 生成器模块
- **`configgen.genjava`** - Java代码和数据生成
- **`configgen.gencs`** - C#代码生成
- **`configgen.gents`** - TypeScript代码生成
- **`configgen.gengo`** - Go代码生成
- **`configgen.genlua`** - Lua代码生成
- **`configgen.genjson`** - JSON数据生成

### 服务模块
- **`configgen.editorserver`** - 编辑器服务
- **`configgen.mcpserver`** - MCP服务

## 使用方式

### 基本用法
```bash
java -jar configgen.jar -datadir [配置目录] [选项] [生成器]
```

### 主要参数
- `-datadir`：配置数据目录，必须包含config.cfg文件
- `-headrow`：CSV/TXT/Excel文件表头行类型，默认2
- `-encoding`：CSV/TXT编码，默认GBK
- `-gen`：指定生成器（java、cs、lua、ts、go、json等）

### 生成器选项
- `-gen java`：生成Java代码
- `-gen javadata`：生成Java二进制数据
- `-gen cs`：生成C#代码
- `-gen lua`：生成Lua代码
- `-gen ts`：生成TypeScript代码
- `-gen go`：生成Go代码
- `-gen json`：生成JSON数据
- `-gen tsschema`：生成TypeScript schema
- `-gen jsonbyai`：AI生成的JSON
- `-gen editorserver`：编辑器服务
- `-gen mcpserver`：mcp服务

### 国际化支持
- `-i18nfile`：国际化文件（CSV或目录）
- `-langswitchdir`：语言切换支持目录
- `-defaultlang`：默认语言，默认zh_cn

### 工具功能
- `-verify`：验证所有数据
- `-search`：进入搜索模式
- `-binarytotext`：二进制数据转文本
- `-xmltocfg`：XML schema转CFG格式

## 配置Schema定义

项目使用自定义的CFG格式定义配置结构，支持：
- **基础类型**：bool、int、float、long、str、text
- **结构体**：struct定义复合数据类型
- **接口**：interface支持多态行为
- **容器类型**：list、map
- **数据表**：table存储配置数据
- **外键关联**：->单向外键、=>多向外键

## 开发注意事项

1. **数据读取**：默认使用FastExcel库，如需POI功能需包含POI依赖
2. **编码处理**：CSV文件默认使用GBK编码，支持带BOM的UTF-8
3. **并发处理**：数据读取使用工作窃取线程池优化性能
4. **缓存机制**：使用缓存文件输出流提高生成性能
5. **错误处理**：完善的错误收集和验证机制

## 测试和验证

- 使用JUnit 5进行单元测试
- 支持JaCoCo代码覆盖率报告
- 可通过`-verify`参数进行数据验证
- 支持Excel读取一致性检查（POI vs FastExcel）

## 扩展开发

### 添加新生成器
1. 实现`configgen.gen.Generator`接口
2. 在`Main.java`中注册生成器
3. 使用JTE模板引擎生成代码

### 添加新数据源
1. 扩展`configgen.data.CfgDataReader`
2. 在`configgen.ctx.ExplicitDir`中配置目录映射

项目采用插件化设计，易于扩展新的生成器和数据源支持。
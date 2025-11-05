# 技术栈说明

## 核心框架和语言

### Java 平台
- **Java版本**：21
- **构建工具**：Gradle 8.1+
- **包管理**：Maven Central

## 主要依赖库

### 测试框架
- **JUnit Jupiter**：5.9.1 - 单元测试框架
- **JUnit Platform Launcher**：测试运行平台支持
- **Mockito**：5.12.0 - 测试模拟框架（当前注释，可选）
- **Mockito Inline**：5.2.0 - 内联模拟支持（当前注释，可选）

### 数据处理
- **ANTLR**：4.13.1 - 语法解析器生成器
- **FastExcel**：0.19.0 - 高性能Excel读取
- **FastCSV**：2.2.2 - 高性能CSV处理
- **FastJSON2**：2.0.43 - JSON序列化/反序列化

### 可选依赖
- **Apache POI**：5.4.1 - Excel文件处理（可选）
- **POI OOXML**：5.4.1 - Office Open XML支持（可选）

### AI集成
- **Simple OpenAI**：3.8.2 - OpenAI API集成

### 模板引擎
- **JTE**：3.2.1 - Java模板引擎，支持动态模板编译和缓存

## 开发工具

### 构建和测试
- **Gradle Wrapper**：项目构建
- **JaCoCo**：代码覆盖率检测
- **JUnit Platform**：测试运行平台

### 代码质量
- **编码规范**：UTF-8编码
- **测试覆盖**：支持HTML、XML、CSV格式报告

## 项目结构

```
src/
├── main/
│   ├── java/
│   │   └── configgen/
│   │       ├── ctx/          # 上下文和目录结构
│   │       ├── data/         # 数据读取和处理
│   │       ├── editorserver/ # 编辑器服务器
│   │       ├── gen/          # 生成器基类
│   │       ├── gencs/        # C#生成器
│   │       ├── gengo/        # Go生成器
│   │       ├── genjava/      # Java生成器
│   │       ├── genjson/      # JSON生成器
│   │       ├── genlua/       # Lua生成器
│   │       ├── gents/        # TypeScript生成器
│   │       ├── i18n/         # 国际化支持
│   │       ├── schema/       # Schema定义
│   │       ├── tool/         # 工具类
│   │       ├── util/         # 工具类
│   │       └── value/        # 值处理
│   └── resources/
│       ├── jte/              # 模板文件
│       └── support/          # 支持文件
└── test/
    ├── java/                 # 测试代码
    └── resources/            # 测试资源
```

## 配置选项

### 构建配置
- **Java工具链**：Java 21
- **编码设置**：UTF-8
- **打包选项**：支持fatJar打包
- **可选依赖**：支持无POI依赖构建（-PnoPoi）

### 运行时配置
- **数据目录**：必须包含config.cfg文件
- **头行设置**：默认第2行作为表头
- **编码设置**：默认GBK，支持BOM检测
- **性能分析**：支持内存使用和时间分析

## 性能特点

### 内存管理
- **缓存文件输出流**：使用CachedIndentPrinter减少IO操作
- **性能分析支持**：支持-p和-pp参数进行详细性能分析
- **内存使用监控**：监控堆内存使用和GC行为
- **流式处理**：大文件流式读取，避免内存溢出

### 处理优化
- **大文件处理**：支持分块处理超大Excel和CSV文件
- **并行处理能力**：支持并行数据读取和模板处理
- **增量生成支持**：避免重复处理，提高生成效率
- **高性能库**：使用FastExcel比Apache POI快3-5倍
- **模板缓存**：JTE模板预编译和缓存，提高生成速度

## 兼容性

### 操作系统
- Windows
- Linux
- macOS

### Java环境
- Java 21+
- 兼容主流JVM实现

---
*最后更新：2025-11-05*
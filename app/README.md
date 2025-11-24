[(English Documents Available)](README_EN.md)

# 🗂️ 配置系统 (cfggen)

## ✨ 核心特性

- 🔗 **数据一致性检查** - 通过配置外键关系，自动检测数据引用完整性
- 💻 **多语言代码生成** - 支持多种编程语言的类型化数据访问代码
- 🏗️ **灵活数据结构** - 支持多态结构、嵌套结构，在 Excel 单元格中编写复杂配置数据
- 🔧 **编辑器服务** - 提供 RESTful API，为配置编辑器 (cfgeditor.exe) 提供支持
- 🤖 **AI 集成** - MCP 服务，为 AI 生成配置提供支持

### 🎯 支持的语言和格式

| 语言/格式 | 描述 | 主要用途 |
|-----------|------|----------|
| **Java** | 类型安全的配置访问，支持 sealed 类 | 后端服务、Android 应用 |
| **C#** | .NET 平台的配置访问 | Unity 游戏、.NET 应用 |
| **TypeScript** | 前端和 Node.js 的类型化配置 | Web 应用、前端项目 |
| **Go** | Go 语言的配置结构体 | Go 后端服务 |
| **Lua** | Lua 表的配置数据 | 游戏脚本、嵌入式系统 |
| **JSON** | 通用的配置数据格式 | 数据交换、API 配置 |

## 📋 环境要求

* **JDK 21** - Java 开发环境
* **Gradle** - 构建工具
* **Git** - 版本控制工具（确保 git/bin 路径已添加到 PATH 环境变量中）

## 🚀 快速开始

### 📦 构建项目

在项目根目录下执行：

```bash
# 生成可执行 JAR 文件
genjar.bat

# 生成 Windows 可执行文件 (包含在 cfggen.zip 中)
mkexe.bat
```

### 🎯 基本用法

```bash
# 使用生成的 JAR 文件
java -jar cfggen.jar -datadir [配置目录] -gen [语言]

# 示例：生成 Java 代码
java -jar cfggen.jar -datadir example -gen java

# 示例：生成 TypeScript 代码
java -jar cfggen.jar -datadir example -gen ts
```

## 🧪 测试示例

### 📖 查看使用说明

```bash
cd example
usage.bat
```

### 多语言代码生成测试

#### ☕ Java 测试
```bash
cd example
genjava.bat    # 生成 Java 代码和数据
gradle build   # 构建项目
java -jar build/libs/example.jar
# 进入命令行交互模式，输入 'q' 退出，输入表名前缀（如 "ai"）查看相关数据
```

> **注意**: Java 17+ 支持 sealed 类，如需兼容旧版本可移除 sealed 关键字

#### 📜 Lua 测试
```bash
cd example
genlua.bat
cd lua
chcp 65001     # 设置 UTF-8 编码（Windows）
lua.exe test.lua
```

#### 🔷 C# 测试
```bash
cd example
gencshape.bat
cd cs
run.bat
```

#### 🐹 Go 测试
```bash
cd example
gengo.bat
cd go
go run .
```

#### 🔷 TypeScript 测试
```bash
cd example
gents.bat
cd ts
pnpm i -D tsx  # 安装 TypeScript 运行环境
npx tsx main.ts
```

## 📚 更多资源

* 📖 [主项目文档](../README.md) - 完整的项目介绍和架构说明
* 📚 [详细文档](https://stallboy.github.io/cfggen) - 在线文档和 API 参考
* 🔌 [VSCode 扩展](../cfgdev/vscode-cfg-extension/README.md) - 配置编辑和语法高亮
* 🎨 [配置编辑器](../cfgeditor/README.md) - 图形化配置编辑工具

## 💡 使用场景

- **游戏开发** - 游戏配置数据管理和代码生成
- **应用配置** - 复杂业务配置的结构化管理和验证
- **多语言项目** - 为不同技术栈提供类型安全的配置访问
- **数据驱动开发** - 通过配置驱动业务逻辑和行为
# Example - cfggen 示例配置

cfggen 配置生成器的示例项目，展示如何定义配置结构并生成多种语言的代码。



## 目录结构

```
example/
├── config/          # 配置 schema 定义（.cfg 文件）
├── config.data/     # 配置数据（Excel 文件）
├── java/            # 生成的 Java 代码
├── cs/              # 生成的 C# 代码
├── go/              # 生成的 Go 代码
├── ts/              # 生成的 TypeScript 代码
├── lua/             # 生成的 Lua 数据
├── i18n/            # 国际化示例
└── *.bat            # 代码生成脚本
```



## 快速开始

### 前置条件

- 确保 `../cfggen.jar` 存在。若不存在，在 `..` 目录下执行 `genjar.bat`

### 查看使用说明

```bash
usage.bat  # 文本说明
```

### GUI来配置参数和启动

```bash
gui.bat  # gui来配置参数和启动
```

## 多语言代码生成测试

### ☕ Java

```bash
genjava.bat         # 生成 Java 代码和数据
gradle build        # 构建项目
java -jar build/libs/example.jar
```

进入命令行交互模式：
- 输入 `q` 退出
- 输入表名前缀（如 `ai`）查看相关数据

> **注意**: Java 17+ 支持 sealed 类，如需兼容旧版本可移除 sealed 关键字


### 📜 Lua

```bash
genlua.bat
cd lua
chcp 65001          # 设置 UTF-8 编码（Windows）
lua.exe test.lua
```

### 🔷 C#

```bash
gencshape.bat
cd cs
run.bat
```

### 🐹 Go

```bash
gengo.bat
cd go
go run .
```

### 🔷 TypeScript

```bash
gents.bat
cd ts
pnpm i -D tsx       # 安装 TypeScript 运行环境
npx tsx main.ts
```

### 国际化示例

```bash
i18n_gencsharp.bat
```

### 使用cfgedtor.exe来查看

1. 确保 `cfgeditor.exe` 存在。若不存在，在 `../cfgeditor` 目录下执行 `genexe.bat`，然后拷贝 `cfgeditor.exe` 到当前目录

2. 运行 `cfgeditor_server.bat`
3. 运行 `cfgeditor.exe` 查看、编辑


## 生成脚本说明

| 脚本 | 说明 |
|---|---|
| `genjava.bat` | 生成 Java 代码 |
| `gencshape.bat` | 生成 C# 代码 |
| `gengo.bat` | 生成 Go 代码 |
| `gents.bat` | 生成 TypeScript 代码 |
| `genlua.bat` | 生成 Lua 数据 |
| `i18n_gencsharp.bat` | 国际化示例（C#） |

# Example - cfggen 示例配置

cfggen 配置生成器的示例项目，展示如何定义配置结构并生成多种语言的代码。



## 目录结构

```
example/
├── config/              # 配置 schema 定义（.cfg 文件）和数据（Excel/CSV/JSON）
│   ├── ai_行为/         # AI 行为配置
│   ├── equip/           # 装备相关配置
│   ├── other/           # 其他配置
│   └── task/            # 任务相关配置
├── java/                # 生成的 Java 代码（单语言）
├── java_ls/             # 生成的 Java 代码（多语言服务器端）
├── cs/                  # 生成的 C# 代码（单语言）
├── cs_ls/               # 生成的 C# 代码（多语言服务器端）
├── cs_ls_client/        # 生成的 C# 代码（多语言客户端）
├── go/                  # 生成的 Go 代码（单语言）
├── go_ls/               # 生成的 Go 代码（多语言服务器端）
├── go_ls_client/        # 生成的 Go 代码（多语言客户端）
├── gd/                  # 生成的 GDScript 代码（Godot 4.x，单语言）
├── gd_ls_client/        # 生成的 GDScript 代码（多语言客户端）
├── ts/                  # 生成的 TypeScript 代码（单语言）
├── ts_ls/               # 生成的 TypeScript 代码（多语言服务器端）
├── ts_ls_client/        # 生成的 TypeScript 代码（多语言客户端）
├── lua/                 # 生成的 Lua 数据（单语言）
├── lua_ls_client/       # 生成的 Lua 数据（多语言客户端）
├── i18n/                # 国际化示例
├── i18n_method1/        # 国际化示例（方法1）
├── cfgeditor_server.bat # 启动配置编辑器服务器
├── gui.bat              # 启动 GUI 配置工具
├── help.bat             # 查看帮助信息
├── mcp_server.bat       # MCP 服务器
└── search.bat           # 搜索配置
```

### 后缀说明

| 后缀 | 说明 |
|------|------|
| 无后缀 | 单语言版本 |
| `_ls` | 多语言服务器端版本（文本全，包含所有语言文本） |
| `_ls_client` | 多语言客户端版本（文本靠切换，通过语言ID获取对应语言文本） |



## 快速开始

### 前置条件

- 使用 JDK 25。在仓库根执行 `app\gradlew.bat -p app fatJar`，生成脚本使用 `app/build/libs/cfggen.jar`。
- 二进制示例统一使用版本化命名块容器；旧数据需重新导出，并同步更新支持源码和加载入口。

### 查看使用说明

```bash
help.bat   # 查看帮助信息
```

### GUI来配置参数和启动

```bash
gui.bat    # gui来配置参数和启动
```



## 增量 Mod 示例

`config_mod/` 共享 `config/` 的结构，演示覆盖怪物 1、新增怪物和任务 1001，以及覆盖固定 enum 的详情。`java/genjava.bat` 和 `cs/gencs.bat` 会分别导出基础包与 `demo-mod.bytes`，对应的 `run.bat` 展示基础实例和组合实例并存、显式切换默认配置、跨包引用与来源查询。

具体数据及只重新导出 Mod 数据的命令见 [config_mod/README.md](config_mod/README.md)。

## 多语言代码生成测试

### ☕ Java

```bash
cd java
genjava.bat         # 生成 Java 代码和数据
run.bat             # 构建并运行

cd ../java_ls
genjava_ls.bat      # 生成 Java 代码和数据（多语言服务器端）
run.bat             # 构建并运行
```

### 📜 Lua

```bash
cd lua
genlua.bat          # 生成 Lua 数据（单语言）
run.bat

cd ../lua_ls_client
genlua_ls_client.bat # 生成 Lua 数据（多语言客户端）
run.bat
```

### 🔷 C#

```bash
cd cs
gencs.bat           # 生成 C# 代码
run.bat

cd ../cs_ls
gencs_ls.bat        # 生成 C# 代码（多语言服务器端）
run.bat

cd ../cs_ls_client
gencs_ls_client.bat # 生成 C# 代码（多语言客户端）
run.bat
```

### 🎮 GDScript

```bash
cd gd
gengd.bat           # 生成 GDScript 代码
run.bat             # 使用 Godot 编辑器运行

cd ../gd_ls_client
gengd_ls_client.bat # 生成 GDScript 代码（多语言客户端）
run.bat
```

### 🐹 Go

```bash
cd go
gengo.bat           # 生成 Go 代码（单语言）
run.bat

cd ../go_ls
gengo_ls.bat        # 生成 Go 代码（多语言服务器端）
run.bat

cd ../go_ls_client
gengo_ls_client.bat # 生成 Go 代码（多语言客户端）
run.bat
```

### 🔷 TypeScript

```bash
cd ts
gents.bat           # 生成 TypeScript 代码（单语言）
run.bat

cd ../ts_ls
gents_ls.bat        # 生成 TypeScript 代码（多语言服务器端）
run.bat

cd ../ts_ls_client
gents_ls_client.bat # 生成 TypeScript 代码（多语言客户端）
run.bat
```

### 国际化示例

```bash
cd i18n
gen.bat

cd ../i18n_method1
gen.bat
```

### 使用cfgedtor.exe来查看

1. 确保 `cfgeditor.exe` 存在。若不存在，在 `../cfgeditor` 目录下执行 `genexe.bat`，然后拷贝 `cfgeditor.exe` 到当前目录

2. 运行 `cfgeditor_server.bat`
3. 运行 `cfgeditor.exe` 查看、编辑


## 根目录脚本说明

| 脚本 | 说明 |
|---|---|
| `help.bat` | 查看帮助信息 |
| `gui.bat` | 启动 GUI 配置工具 |
| `search.bat` | 搜索配置 |
| `cfgeditor_server.bat` | 启动配置编辑器服务器 |
| `mcp_server.bat` | MCP 服务器 |

## 各语言目录脚本说明

每个语言目录（如 `java/`, `cs/`, `go/` 等）都包含：

| 脚本 | 说明 |
|---|---|
| `gen*.bat` | 生成该语言的代码和数据 |
| `run.bat` | 构建并运行验证 |

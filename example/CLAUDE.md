# CLAUDE.md - Example 示例项目

本文件为 Claude Code 提供 example 目录的开发指导。

## 项目概述

这是 cfggen 配置生成器的示例项目，用于测试和展示多语言代码生成功能。

## 目录说明

### 配置源文件 (config/)

```
config/
├── config.cfg          # 公共结构定义（LevelRank, Position, Range）
├── builders.txt        # 构建器配置
├── ai_行为/            # AI 行为配置
│   ├── ai.cfg          # AI schema
│   └── ai行为.xlsx     # AI 数据
├── equip/              # 装备系统配置
│   ├── equip.cfg       # 装备 schema
│   └── *.csv           # 装备数据
├── other/              # 其他系统配置
│   └── *.csv           # 掉落、怪物、签到等
└── task/               # 任务系统配置
    ├── task_任务.csv   # 任务数据
    └── taskextraexp.csv
```

### 生成的代码目录

| 目录 | 语言 | 多语言支持 | 说明 |
|------|------|------------|------|
| java/ | Java | 无 | 单语言版本 |
| java_ls/ | Java | 服务器端 | 文本全，包含所有语言 |
| cs/ | C# | 无 | 单语言版本 |
| cs_ls/ | C# | 服务器端 | 文本全 |
| cs_ls_client/ | C# | 客户端 | 文本靠切换 |
| ts/ | TypeScript | 无 | 单语言版本 |
| ts_ls/ | TypeScript | 服务器端 | 文本全 |
| ts_ls_client/ | TypeScript | 客户端 | 文本靠切换 |
| lua/ | Lua | 无 | 单语言版本 |
| lua_ls_client/ | Lua | 客户端 | 文本靠切换 |
| go/ | Go | 无 | Go 语言单语言版本 |
| go_ls/ | Go | 服务器端 | Go 多语言服务器端版本 |
| go_ls_client/ | Go | 客户端 | Go 多语言客户端版本 |
| gd/ | GDScript | 无 | Godot 4.x 单语言版本 |
| gd_ls_client/ | GDScript | 客户端 | Godot 4.x 多语言客户端版本 |

### 辅助工具脚本

| 脚本 | 功能 |
|------|------|
| help.bat | 显示帮助信息 |
| gui.bat | 启动 GUI 配置工具 |
| search.bat | 搜索配置内容 |
| mcp_server.bat | MCP 服务器 |
| cfgeditor_server.bat | 启动配置编辑器服务器 |

### 各语言目录脚本

每个语言目录（如 `java/`, `cs/`, `go/`, `gd/`, `ts/`, `lua/`）都包含：
- `gen*.bat` - 生成该语言的代码和数据
- `run.bat` - 构建并运行验证

## 后缀命名规范

- **无后缀**: 单语言版本，不包含多语言支持
- **`_ls`**: 多语言服务器端版本（Language Server），包含所有语言文本
- **`_ls_client`**: 多语言客户端版本，通过语言 ID 动态获取文本

## 代码生成命令

### 基本格式

```bash
java -jar ../../cfggen.jar -datadir ../config -gen <目标类型>[,选项...] [-gen <目标类型2>...]
```

### 常用目标类型

- `java` - 生成 Java 代码
- `cs` - 生成 C# 代码
- `ts` - 生成 TypeScript 代码
- `lua` - 生成 Lua 数据
- `go` - 生成 Go 代码
- `gd` - 生成 GDScript 代码
- `bytes` - 生成二进制数据文件

### 常用选项

- `dir:<路径>` - 输出目录
- `own:<选项>` - 语言特定选项
- `builders:<文件>` - 构建器配置文件
- `configgendir:<路径>` - 配置生成器代码目录
- `emmylua` - 生成 EmmyLua 注解（Lua）
- `shared` - 使用共享表（Lua）

## 开发工作流

### 修改配置后重新生成

1. 修改 `config/` 下的 .cfg schema 或数据文件
2. 进入目标语言目录，执行对应的 `gen*.bat`
3. 执行 `run.bat` 验证生成结果

### 添加新语言支持

1. 在 `config/` 下添加对应的 .cfg 和数据文件
2. 在各语言目录下创建或修改生成脚本
3. 测试生成和运行

### 添加多语言支持

1. 在 `i18n/` 目录下配置语言表
2. 创建带 `_ls` 或 `_ls_client` 后缀的目录
3. 修改生成脚本添加多语言相关选项

## 测试验证

每个语言目录下都有 `run.bat` 用于验证生成的代码：

- **Java**: `gradle run` 或直接运行 Java
- **C#**: `dotnet run`
- **TypeScript**: `npx tsx main.ts`
- **Lua**: `lua.exe test.lua`
- **Go**: `go run main.go`
- **GDScript**: 需要在 Godot 编辑器中运行

## 特殊说明

### GDScript 项目

- 使用 Godot 4.x 引擎
- 项目配置文件：`project.godot`
- 主场景：`main.tscn`
- 入口脚本：`main.gd`

### Go 项目

- 使用 Go Modules 管理依赖
- `go.mod` 定义模块依赖
- 支持 `go_ls` 和 `go_ls_client` 多语言版本

### TypeScript 项目

- 使用 npm/pnpm 管理依赖
- `package.json` 定义项目配置
- 使用 tsx 直接运行 TypeScript

## 注意事项

1. 确保 `cfggen.jar` 存在于项目根目录
2. 生成前会清理旧的生成代码（各 gen*.bat 开头的 rm 命令）
3. 多语言版本需要配置语言表（i18n 目录）
4. GDScript 项目使用 Godot 4.x
5. Windows 环境下执行 .bat 文件使用 `./` 前缀

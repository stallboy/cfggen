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
- 运行全部示例需要 .NET 10 SDK、Go、Node.js、64 位 Lua 5.3+ 和 Godot 4.6。TypeScript 示例使用 ES2020；先在三个 TypeScript 目录分别运行 `npm ci`。
- Lua 的 `CFGGEN_LUA` 可指定解释器路径，默认查找 `lua54`；仓库旧 `lua/lua.exe` 是 Lua 5.1，不能运行 long 引用。Godot 的 `CFGGEN_GODOT` 可指定引擎路径。

在仓库根目录运行 `example\gen_run.bat`，逐一重新生成、编译并运行全部 15 个模式。示例会对原有 record 外键、combat 内部对象引用、国际化和适用模式下的 Mod 做真实断言；普通 TypeScript 模式还自动执行下述三个断言回归。任一生成、编译或断言失败时，总脚本返回非零退出码，不跳过缺少的工具。

### 2026-10-21 最终 JAR 复验

使用最新的 `app/build/libs/cfggen.jar` 执行完整生成与验收脚本，结果为 **`PASS=15 FAIL=0`**，包含三个自动断言回归；生成代码同步提交，额外的 `-gen verify` 也通过。

| 语言 | 验收模式 |
| --- | --- |
| Java | `java`、`java_ls` |
| C# | `cs`、`cs_ls`、`cs_ls_client` |
| Go | `go`、`go_ls`、`go_ls_client` |
| TypeScript | `ts`、`ts_ls`、`ts_ls_client` |
| Lua | `lua`、`lua_ls_client` |
| GDScript | `gd`、`gd_ls_client` |

运行入口显式断言 Record 内部引用的目标身份，覆盖本 record、跨 record、唯一键、最近祖先、混合来源、固定 struct/interface 终点、各类 list/map 键、复合及 struct 表键、nullable 各层缺失、合法空键、引用环与空对象包含身份。long 使用 `9007199254740993` 验证，递归 interface 引用检查内层未命中时不回退外层。原有外键、国际化、快照与 Mod 断言沿用各模式的实际入口。

Lua 本轮使用 Lupa 的 Lua 5.4 运行时，经本地 CLI 桥接执行两个 `run.bat`；Godot 使用 4.6 headless 导入、编译并执行验证。Java、C#、TypeScript、Go 均实际编译后运行。完整命令及最终结果见[工具链验收记录](../docs/src/content/docs/core/internal-reference-tooling-design.md)。

验证代码复审后，两个 Lua 模式共用 [`combat_assertions.lua`](combat_assertions.lua) 的普通函数，删除重复验证块；各语言直接遍历两台机器和全部叶来源，用必要的数量或存在性断言防止空集合零次检查、两个空值相等时误通过。循环分别检查 Idle→Hit 与 Hit→Idle；struct map 按键内容寻找实际条目，不依赖首项或要求所有 value 相同。空包含对象用身份集合检查全体互异，不只比较部分配对。

### 断言回归

[`ts/reference-regressions.cjs`](ts/reference-regressions.cjs) 已将三个配置变体固化为自动化回归，接入普通 TypeScript 的 `npm test`，并随 `gen_run.bat` 与现有 Test CI 工作流执行：

| 配置变体 | 期望结果 |
| --- | --- |
| Combat 的 Idle 改为自环 | 示例必须在 Idle→Hit 断言失败 |
| 清空两组 PhaseUse 目标列表 | 示例必须在目标数量断言失败 |
| struct map 的 Hit 前加入合法 Warmup 条目 | 示例全部断言仍须通过 |

三份配置都必须先用最终 JAR 成功生成 bytes，再运行当前编译的示例入口。前两个是合法配置但不符合示例预期，必须核对指定的 `AssertionError`；工具缺失、生成失败或其他断言失败均不能算通过。脚本直接修改固定 fixture 的原文并检查匹配数量，保留 `9007199254740993` 等数字，不用 `JSON.parse` 重写配置。

在 `example/ts` 执行 `npm test` 会依次编译、运行普通示例和这三个回归，需要 Java、`app/build/libs/cfggen.jar` 和完整的 `example/config`；其他 TypeScript 模式沿用各自验证入口。Test CI 会先构建 JAR、通过锁文件安装已有 TS 依赖、重新生成 TS/bytes，再执行同一命令。测试副本创建在忽略的 `example/build` 中，结束时只清理本次创建的目录；不会修改正常示例数据，也不依赖历史 Git 提交或新增测试框架。

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
run.bat             # headless 导入并运行验证；也可用编辑器打开 project.godot

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

三个 TypeScript `run.bat` 都执行 `npm test`，先严格编译，再运行生成的 JavaScript。Lua 和 Godot 的验证入口会保留配置快照，检查切换配置后的旧引用身份；普通 Java、C# 示例还验证 Mod 合并后的引用及原始实例保持不变。

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

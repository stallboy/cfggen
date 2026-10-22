---
title: 内部对象引用实现计划
description: 内部对象引用的模块依赖、分阶段交付和验收要求
sidebar:
  order: 101
---

依据 [内部对象引用设计](../internal-reference-design/) 实现。每阶段执行测试驱动的实现、精简重构、复查与 Git 提交，再进入下一阶段。只引入当前设计需要的模型和辅助函数，保留旧语法行为。

交付状态：阶段 0–7 全部完成，2026-10-20 验收。

后续 skills、VSCode 新语法与格式化、cfgeditor 内部目标定位及精确 long 的设计、独立提交和最新验收见 [工具链支持规划与交付记录](../internal-reference-tooling-design/)。

## 模块与交付顺序

| 阶段 | 模块 | 交付与验证 |
| --- | --- | --- |
| 0 | 设计与计划 | 保存确认的设计、nullable 文档修正和本计划 |
| 1 | schema、schema.cfg | 类型化源路径与目标路径，空源值列表、index，CFG 解析和写回往返，旧语法回归 |
| 2 | schema、value | 解析寻址计划，检查键数量与类型，实际包含上下文、局部索引和整链 nullable，覆盖设计中的正反例 |
| 3 | schema 裁剪、value 查询、editorserver、mcpserver、schema 导出 | tag 依赖裁剪，引用查询与对外描述能表达内部目标 |
| 4 | genbytes、配置包与 schema | 沿用原有数据编码和 schema 检查 |
| 5 | genjava、Java 模板与运行时 | 类型化目标访问，实际配置快照、Mod 最终数据和循环引用的端到端测试 |
| 6 | gencs、gents、gengo、genlua、gengd 及其模板和运行时 | 逐语言接入，Lua 共享与空结构身份，引用计数运行时的循环回收，各语言生成、编译与加载验证 |
| 7 | 示例、文档、全套验收 | 完整验收矩阵、旧语法回归、打包与最终精简复查 |

## 实现原则

解析后的路径与键绑定由 schema 层统一描述，值校验和生成器复用，不在各入口重复解释字符串。数据层不依赖生成器，生成器不改变原始对象所有权。

每个可提交阶段保持构建和已有测试通过。迁移过程中，尚未接入的生成器遇到新关系时明确拒绝；六种运行时接入后已删除临时拦截类。阶段 6 按语言分成五个独立提交。

## 验证要求

- 每项新行为先有失败测试；保留旧外键和旧数据包的兼容回归。
- 验证真实生成代码的编译与加载，以及目标对象所属的配置快照和包含位置。
- 用同名键、相同字段值、嵌套作用域及多配置实例暴露错误共享和回退。
- 每阶段精简新代码并检查差异，测试通过后提交；阶段记录以 Git 历史为准。

## 交付记录

| 阶段 | 提交 |
| --- | --- |
| 0 | `df9a4b1d` 设计与计划 |
| 1 | `620aea7c` CFG 语法与路径模型 |
| 2 | `8f0ce763` 编译寻址计划与值校验 |
| 3 | `e2f4d441` 裁剪、查询、编辑器和 schema 描述 |
| 4 | 配置包继续使用原有版本 1 和 schema 检查 |
| 5 | `a63c315e` Java |
| 6 | `fa06fad4` C#、`8f7927b7` TypeScript、`b2661448` Go、`3c738b5b` Lua、`906a7845` Godot |
| 7 | `5bfc406d` 跨运行时验收、示例和文档 |
| 审查修复 | `0c999c59` 目标表查找、C#/Java 生成兼容、查询解析器复用、Lua 原有 API |
| 示例验收 | `example/config/combat` 的各类游戏数据、六语言共享夹具、原 Lua 示例兼容及运行要求 |

## 最终验收

| 验证入口 | 结果 |
| --- | --- |
| `app/gradlew.bat test` | 786 项通过，含架构依赖、解析往返、值校验、裁剪、查询及真实 Java 编译加载 |
| `app/gradlew.bat dotnetTest` | 11 项通过，含 C# 最新语法与 Unity/C# 9 |
| `app/gradlew.bat otherRuntimeTest` | 15 项通过，真实 TypeScript 严格编译/Node 执行与 Go 测试，含旧/新语法下的 struct 键、唯一键和任意字段反向引用 |
| `app/gradlew.bat godotTest` | 4 项通过，真实 Godot 4.6 加载、旧快照稳定与引用环回收 |
| `app/gradlew.bat luaTest` | 5 项通过，真实 Lua 5.4，覆盖共享与预加载四种组合，以及完整 example/config 的原示例脚本 |
| `cfgeditor` TypeScript 检查 / Vitest | 类型检查通过；32 个文件、519 项测试通过 |
| `docs` 构建 | 44 页静态站点构建成功 |
| `app/gradlew.bat fatJar` | 生成 `app/build/libs/cfggen.jar`；用该 JAR 校验、导出、生成示例，再实际编译与加载 Java 示例产物 |
| `example/gen_run.bat` | 15 个语言/文本模式全部重生成、编译、运行，`PASS=15 FAIL=0`，包含 Godot headless，任何失败均返回非零 |

Gradle 命令在 `app` 目录执行。额外运行时测试不属于默认 `test`：C# 需要 .NET 10 SDK，TypeScript 需要 Node 和 `cfgeditor` 依赖，Go 需要 Go 工具链；Godot 路径可通过 `-Dcfggen.godot=绝对路径` 指定。Lua 默认调用 `lua`，也可用 `-Dcfggen.luaPython=Python路径 -Dcfggen.luaModules=Lupa安装目录` 运行 Lupa 的 Lua 5.4。

共用运行时数据覆盖复合 list 索引、`false / 0 / 空字符串`、struct map key、多态终点、源根/祖先/当前对象组合取键、唯一键查表和零键固定对象。损坏包回归证明：五种二进制运行时拒绝重复 map key 和未被引用的重复局部索引。Java/C# 额外覆盖 Mod 合并后绑定；Lua/Godot 覆盖旧快照环回收。

可运行示例位于仓库 `samples/internal-reference`。它将 abilityId、machineKey、phaseKey 分别放在两层祖先和当前对象，保留每个机器内的 Phase 及其局部循环引用。

完整类型覆盖位于 `example/config/combat`，该目录的 README 将每类源路径、目标路径、list/map 键和 nullable 场景对应到实际游戏数据。六种语言的运行时测试直接复制此目录；15 项专用测试包含九种临时非法变体，提交的示例保持可校验。TypeScript 三个示例使用 ES2020；Lua 示例通过 `CFGGEN_LUA` 指定 64 位 Lua 5.3+，不再默认调用仓库中的 Lua 5.1。

审查同时移除了 C# 冗余绑定转发方法，并让一次查询共用一个快照引用解析器。Lua 新引用模式中的空容器独立创建，继续保留应用为 `mkcfg.E` 设置的元表行为。

示例全面验收后，Java 关系绑定已并入现有 `ConfigMgrLoader`，Lua 关系状态已并入 `_cfgs.lua`，不再生成 `$ConfigRelations$.java` 或 `_relations$.lua`。Godot nullable 访问不再触发元数据缺失错误，仅实际结构键携带内容键元数据。Go 修复普通 struct 主键/唯一键按指针比较的问题，统一外键 getter 并补齐任意字段反向索引，删除两份重复容器模板；`String()` 直接格式化原始字段，不再嵌套调用格式化。新旧配置、Mod、国际化与快照身份均有示例入口中的失败断言，Lua 输出中的数据字符串空格仍原样保留。

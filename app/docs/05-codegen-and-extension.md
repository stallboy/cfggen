# 代码生成与扩展（gen 层）

gen 层消费 `CfgValue`，用模板渲染成各语言的代码与数据文件。所有生成器继承 `Generator`，在 `Main.registerAllProviders` 里登记一行即可被 `-gen` 调用。

## `Generator` 基类与参数机制

`Generator`（见 `gen/Generator.java`）极简：持有一个 `Parameter`，一个抽象 `generate(Context)`。真正巧妙的是 **`Parameter` 的双实现**：

> `Parameter` 接口有两个实现：`ParameterParser`（真解析参数）和 `ParameterInfoCollector`（收集参数 usage，供 `-h` 帮助生成）。生成器**只在构造函数里写一遍 `parameter.get(...)` / `parameter.has(...)`**，就同时服务于"真解析"和"参数文档"两件事。

`ParameterParser`（见 `gen/ParameterParser.java`）边读边 `remove`，所以最后 `assureNoExtra()` 能查出**未被消费的多余参数**——拼错的 `-gen` 子参数会直接 `AssertionError`，而不是被静默忽略。

需要支持 `-gen name,tag=...` 的生成器继承 `GeneratorWithTag`（持有 `tag` 字段）。

## 注册表 `Generators`

见 `gen/Generators.java`。一个 `LinkedHashMap<String, GeneratorProvider>`（保留注册顺序）。`create(arg)`：解析 → 查表 → 构造 → `assureNoExtra`。**加生成器 = 加一行 `addProvider`**，无需改框架。

## 模板引擎 `JteEngine`

见 `util/JteEngine.java`。静态 `engine`，启动时决定走哪条路：

| 情况 | 做法 | 何时 |
|---|---|---|
| jar 里有预编译模板（`gg/jte/generated/precompiled/`） | `createPrecompiled` 直接加载 class，**零编译** | 发布 jar（`build.gradle` 的 `jte{generate()}` 在构建期把模板编成 class 烤进 jar） |
| 无预编译产物 | 动态编译到临时目录，按 **cwd 的 hashCode** 缓存复用 | 开发期 `./gradlew.bat run` |

为什么这么设计：模板编译一次约 **1.6s**，发布 jar 里预编译就省掉每次启动这笔开销；开发期动态编译到 `jte-classes-<hash>` 临时目录，同项目多次跑能复用缓存。

另有 `renderTryFileFirst(filePath, fileInResources, model)`：**优先用工作区文件模板，否则回退内置资源**。AI 生成/翻译流程用它，方便用户用自己的 prompt 模板覆盖默认。模板源文件在 `src/main/resources/jte/`。

## 生成器做什么（以 `cs` 为例）

`CsCodeGenerator`（见 `gencs/CsCodeGenerator.java`）是代表性实现，`generate(ctx)` 流程：

1. `ctx.makeValue(tag)` 拿值（代码注释原话："这里只需要 schema，生成 value 只用于检验数据"——生成器顺带让值解析跑一遍校验，schema 从 `value.schema()` 取）。
2. 拷贝运行时支撑文件（`Loader.cs`，`unity` 模式换 `Loader.unity.cs`——C#9 兼容版）。
3. 每个 struct / interface / table → 建 `Model` → `JteEngine.render("cs/GenXxx.jte", model, ps)` 写到独立文件，**单阶段并发**。
4. 收尾：`ModuleLoader`、`Text`（lang-switch 时）、`CachedFiles.keepMetaAndDeleteOtherFiles` 清理**本次没再生成的过期文件**（保留 `Loader.cs` 等 meta）。

两个关键并发手法（所有 `gen*` 通用）：

- **`ThreadLocal<CacheConfig>` 打印机缓冲**：每个工作线程独占一组缓冲，避免多线程踩踏共享 `StringBuilder`。
- **interface 连同其 impls 捆成一个任务、内部保序**：二者可能落到同一路径，任务内"先 interface 后 impls"避免并发竞态写反。

以及一个统一范式：**Model + 模板分离**——`Model` 类（如 `StructModel`/`InterfaceModel`/`ProcessorModel`）把要渲染的数据全备好，`.jte` 模板只管输出。逻辑在 Model，排版在模板。

## 生成器族矩阵

都同构：`Generator` 子类 + `Model` + `jte` 模板。差异主要在语言特性处理。

| `-gen` 名 | 类 | 产出 | 备注 |
|---|---|---|---|
| `java` | `JavaCodeGenerator` | Java 代码 + 数据 | sealed 类 |
| `cs` | `CsCodeGenerator` | C# 代码 | .NET；`unity` 模式 C#9 兼容 |
| `ts` | `TsCodeGenerator` | TypeScript | 前端 |
| `go` | `GoCodeGenerator` | Go | |
| `lua` | `LuaCodeGenerator` | Lua 表 | `shared` 内存优化；lang-switch 支持 |
| `gd` | `GdCodeGenerator` | GDScript | Godot |
| `json` | `JsonGenerator` | JSON 数据 | |
| `bytes` | `BytesGenerator` | 二进制 | 详见 [`06`](06-bytes-format.md) |
| `byai` | `ByAIGenerator` | AI 辅助生成配置 | |
| `tsschema` | `TsSchemaGenerator` | 导出 TS schema | 供 AI / 前端 |
| `i18n` / `i18nbyid` | `I18nBy*Generator` | 国际化文件 | 见 [`09`](09-i18n.md) |
| `verify` / `search` | `ValueVerifyTool` / `ValueInspectTool` | 校验 / 检索 | 见 [`08`](08-errors-and-validation.md) |
| `server` / `mcpserver` | `EditorServer` / `CfgMcpServer` | 服务 | 见 [`07`](07-write-back-and-servers.md) |

## 并发生成（设计原理）

- struct / interface / table 的渲染**互不依赖**，单阶段并发，实测 ~2–3x（java/cs/go/gd 各自的并发改造，见仓库内性能记录）。
- 唯一共享可变状态是打印机缓冲 → `ThreadLocal` 隔离；`interface`+`impl` 同路径 → 任务内保序。
- 上限：被某张超大表（如十几万行）封顶时，包级并发到头，再快只能**表内行级并发**。

## 如何加一个新语言生成器

接住 [`CLAUDE.md`](../CLAUDE.md) 移出的「扩展开发」段。最小步骤：

1. 新建 `gen<lang>` 包，写 `XxxCodeGenerator extends Generator`（需要 tag 就继承 `GeneratorWithTag`）。
2. **构造函数里声明参数**：`dir = parameter.get("dir", "...")` 等——只写这一遍，参数解析和 `-h` 文档都有了。
3. `generate(ctx)`：`ctx.makeValue(tag)` → 建 `Model` → `JteEngine.render("lang/Xxx.jte", model, ps)`。并发渲染照抄 cs 的 `ThreadLocal` + `invokeAll` 套路。
4. 模板放 `src/main/resources/jte/lang/`。
5. 在 `Main.registerAllProviders` 加一行 `Generators.addProvider("lang", XxxCodeGenerator::new)`。

> 完整的可运行示例（多语言生成测试）在仓库 `example/`；写法约定参考用户站点 [`core/bestpractices`](../../docs/src/content/docs/core/bestpractices.mdx)。

## 关键类速查

| 关注点 | 主类 |
|---|---|
| 生成器基类 | `Generator`、`GeneratorWithTag` |
| 参数（双实现） | `Parameter`、`ParameterParser`、`ParameterInfoCollector` |
| 注册表 | `Generators`、`Tools` |
| 模板引擎 | `JteEngine` |
| 缓存输出 / 清理过期 | `CachedFiles`、`CachedIndentPrinter`、`CachedFileOutputStream` |
| 代表实现 | `CsCodeGenerator`（cs）、`JavaCodeGenerator`（java） |

## 接下来

- 二进制格式深挖 → [`06-bytes-format`](06-bytes-format.md)。
- 生成只是消费方之一；编辑器 / AI 怎么**写回**数据 → [`07-write-back-and-servers`](07-write-back-and-servers.md)。

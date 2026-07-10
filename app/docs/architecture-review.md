# cfggen 架构分析与改进建议

> 本文既是 `app/`（cfggen 核心生成器）的架构现状梳理，也是一份改进建议与教学文档。
> 第 5、7 节的建议与路线图已经过多视角评审与源码核实修正（2026-07），并补充了原文档遗漏的安全维度。
> 范围：`src/main/java/configgen/**`，约 254 个 Java 文件、3.3 万行。
> 技术栈：Java 25、Gradle、ANTLR 4、JTE 模板引擎、FastExcel/POI、FastJSON2、simple-openai、MCP Java SDK。

---

## 目录

1. [一句话定位](#1-一句话定位)
2. [架构全景](#2-架构全景)
3. [子系统详解](#3-子系统详解)
4. [架构亮点（值得肯定的设计）](#4-架构亮点值得肯定的设计)
5. [改进建议](#5-改进建议)
6. [同类项目借鉴](#6-同类项目借鉴)
7. [改进路线图](#7-改进路线图)
8. [教学要点速记](#8-教学要点速记)

---

## 1. 一句话定位

cfggen 是一个 **schema-first 的多源（Excel/CSV/JSON）→ 多目标（Java/C#/TS/Go/Lua/GDScript + 二进制 + JSON）配置代码生成器**，并附带编辑器 REST 服务与 AI/MCP 接入能力。

它的核心价值链是：

```
.cfg schema 定义  +  Excel/CSV/JSON 数据
        ↓ (解析、对齐、外键校验、国际化)
   强类型 CfgValue
        ↓ (模板 / 序列化)
   各语言代码 + bytes 数据文件
```

---

## 2. 架构全景

### 2.1 分层与数据流

```
                ┌──────────────────────────────────────────┐
   命令行/GUI    │  configgen.gen.Main  (入口 + 参数 + 注册)  │
                └──────────────────────────────────────────┘
                              │ 顺序驱动 generators
                              ▼
                ┌──────────────────────────────────────────┐
   协调层        │  configgen.ctx.Context  (装配 + 缓存)     │  ◀── God Object 倾向
                └──────────────────────────────────────────┘
            ┌───────────────┬───────────────┬───────────────┐
            ▼               ▼               ▼               ▼
   ┌──────────────┐ ┌──────────────┐ ┌─────────────┐ ┌──────────────┐
   │ schema       │ │ data         │ │ value       │ │ i18n         │
   │ (结构定义)    │ │ (Excel 读取) │ │ (值解析)     │ │ (多语言)      │
   │              │ │              │ │             │ │              │
   │ CfgSchema    │ │ CfgDataReader│ │ CfgValue    │ │ LangTextFinder│
   │ StructSchema │ │ ExcelReader  │ │ ValueParser │ │ LangSwitchable│
   │ InterfaceSch │ │ ReadCsv      │ │ RefValidator│ │              │
   │ ForeignKeySch│ │ AlignToData  │ │ VTableCreator│              │
   └──────┬───────┘ └──────────────┘ └──────┬──────┘ └──────────────┘
          │              ▲                  │
          │   schema.cfg  │                  │
          └───────────────┘                  │
                                             │
                              ┌──────────────┴───────────────┐
                              ▼                              ▼
   生成器层     ┌───────────────────────────┐  ┌──────────────────────────┐
   (gen/*)      │ 模板型 genjava/gencs/gents │  │ 序列化型 genbytes         │
                │ gengo/gengd               │  │ 委托型 genjson            │
                │ (JteEngine + *.jte 模板)   │  │ (手写二进制流)             │
                │                           │  │                           │
                │ 异类: genlua (2042 行,     │  │                           │
                │       纯字符串拼接, 无模板) │  │                           │
                └───────────────────────────┘  └──────────────────────────┘
                              │
                              ▼
   服务层       ┌───────────────────────────┐  ┌──────────────────────────┐
   (常驻进程)    │ editorserver (HTTP REST)   │  │ mcpserver (MCP for AI)   │
                │ extends GeneratorWithTag   │  │ extends Generator         │
                │ com.sun.net.httpserver     │  │                           │
                └───────────────────────────┘  └──────────────────────────┘
```

### 2.2 关键观察

| 维度 | 现状 |
|------|------|
| 包数量 | 25 个包，最重的是 `genlua`（2042 行）、`geni18n`（1451 行）、`genbyai`（868 行） |
| 模板 | 39 个 `.jte` 模板，覆盖 java/cs/ts/go/gd 五种语言 + 翻译 |
| 测试 | 71 个测试文件，覆盖 schema/value/data/util/gen 各包 |
| 插件 | 17 个 provider（6 个 Tool + 11 个 Generator），全部硬编码注册 |
| 并发 | 多处 `newWorkStealingPool` 并行解析 schema/数据/值 |

---

## 3. 子系统详解

### 3.1 插件体系（`configgen.gen`）

**核心抽象**：两个并列的抽象基类。

- `Generator`（`gen/Generator.java:25`）：唯一抽象方法 `generate(Context ctx)`，产出文件或起服务。
- `Tool`（`gen/Tool.java:14`）：唯一抽象方法 `call()`，不需要 Context，在 Context 构建前独立运行（`Main.java:220-223`）。
- `GeneratorWithTag`（`gen/GeneratorWithTag.java:8`）：在 `Generator` 上加一层 tag 过滤（读 `own` 参数），绝大多数生成器继承它。

**注册机制**：工厂 + 静态 `LinkedHashMap` 注册表（`Generators.java:13`、`Tools.java:13`），全部硬编码在 `Main.registerAllProviders()`（`Main.java:64-90`）：

```java
Generators.addProvider("java", JavaCodeGenerator::new);
Generators.addProvider("server", EditorServer::new);   // 常驻 HTTP 服务
Tools.addProvider("xmltocfg", XmlToCfgTool::new);
```

**教学要点**：函数式接口 `XxxProvider::create(Parameter)` 作为工厂，是 Java 插件注册的简洁范式。`LinkedHashMap` 保序保证帮助文本/GUI 下拉顺序稳定。

### 3.2 Parameter 双实现（本项目最巧妙的设计）

`Parameter` 接口有两个实现（注释见 `Generator.java:18-20`）：

| 实现 | 用途 | `get(key, def)` 行为 |
|------|------|---------------------|
| `ParameterParser` | 真实运行 | 读出并 **remove**（破坏性读），返回真实值 |
| `ParameterInfoCollector` | 收集帮助/GUI 元数据 | 记录 `(key, def)`，返回 `def` |

生成器构造函数里写一次 `parameter.get("dir", "config")`，**同时定义了三处行为**：运行解析（`Main`）、帮助文本（`Help.java:83-88`）、GUI 输入框（`ui/ParameterPanelItem.java:38-50`）。DRY 做到极致。

> **教学要点**：把"参数定义"和"参数使用"统一到一次方法调用，通过切换实现来改变语义——这是 Strategy 模式与"声明即文档"思想的优雅结合。代价是 `get()` 的破坏性读（`ParameterParser.java:21`）是个隐蔽陷阱：同一参数读两次第二次拿默认值；一个叫 `get` 的方法会改写内部状态，从 Command-Query 角度是设计异味。

### 3.3 Context 协调层（`configgen.ctx`）

`Context`（`ctx/Context.java`）是全局协调者，承担 **6 项职责**：

1. 配置持有（`ContextCfg`，不可变 record）
2. 文件系统扫描（`DirectoryStructure`）
3. i18n 策略持有（两种模式二选一）
4. 数据读取器工厂（`ExcelReader`、`ReadCsv`）
5. Schema + Data 装配（`readSchemaAndData`，`Context.java:81-115`）
6. CfgValue 工厂 + 缓存（`makeValue`，`Context.java:173-205`）

**缓存**：单槽 CfgValue 缓存，键是 tag（`lastCfgValue` + `lastCfgValueTag`，`Context.java:51-52`）。目的：`-gen java -gen cs -gen go` 连续多生成器同 tag 时复用，避免重复解析。

**传递方式**：绝大多数生成器把 Context 当**方法参数**用、不存字段——克制。只有 3 处例外（长生命周期服务）：`WatchAndPostRun`（热重载）、`EditorServer`、`CfgMcpServer`。

**热重载**：`WatchAndPostRun`（`ctx/WatchAndPostRun.java`）用 Java WatchService + 防抖定时器，检测到文件变化后**新建整个 Context 再替换引用**（`reloadData`），读侧靠 volatile 引用保证安全发布。这是"不可变替换"（immutable swap）的正确并发模式。

### 3.4 Schema 类型系统（`configgen.schema`）

**这是整个项目设计最严谨的部分。** 用 Java 25 的 **sealed 接口 + 模式匹配**贯穿始终，编译期穷尽性检查。

```
Nameable (sealed)                    —— 一切可命名的 schema 元素
├── Fieldable (sealed)               —— 可被引用为字段类型
│   ├── StructSchema                 —— 结构体（有字段、外键）
│   └── InterfaceSchema              —— 多态接口（含若干 impl）
└── Structural (sealed)              —— 有 fields + foreignKeys
    ├── StructSchema
    └── TableSchema                  —— 配置表（主键/唯一键/entry）

FieldType (sealed)
├── SimpleType (sealed)
│   ├── Primitive (enum)             —— BOOL/INT/LONG/FLOAT/STRING/TEXT
│   └── StructRef                    —— 符号引用（resolve 前后两态）
└── ContainerType (sealed)
    ├── FList(SimpleType)
    └── FMap(SimpleType, SimpleType)
```

**外键系统**（核心亮点，`ForeignKeySchema` + `RefKey`）：

| 语法 | 含义 | RefKey 变体 |
|------|------|------------|
| `rank:int ->equip.rank` | 单向外键，引用主键 | `RefPrimary` |
| `name:str ->equip[equipname]` | 单向外键，引用唯一键 | `RefUniq` |
| `lootid:int =>lootitem[lootid]` | 多向外键，反向 list | `RefList` |

**解析流程**（`CfgSchemaResolver`，6 步）：setImpl → checkNameConflict（构建查找 map）→ resolveAllFields（StructRef 填充 + enum 自动转外键）→ resolveNameable → resolveForeignKeys → checkChainedSepFmt → checkUnused。解析后预计算 span/hasRef/hasBlock/hasMap/hasText 五个辅助属性，供生成器免重算。

**CFG 文本格式**（`schema/cfg/Cfg.g4`）：ANTLR 4 文法定义。一个巧妙设计是 `LC_COMMENT`/`SEMI_COMMENT` 词法规则把 `{`/`;` 与同行注释合并成一个 token，让注释能紧贴符号。

### 3.5 Value 解析层（`configgen.value`）

`CfgValue`（`value/CfgValue.java`）与 schema 同构，值类型层次镜像 `FieldType`。每个 Value 都带 `Source source()`，**溯源到原始 Excel cell**——错误信息能精确报"sheet=xxx,row=3,col=C"，调试体验是亮点。

**解析核心**：`ValueParser`（`value/ValueParser.java`）是 schema 驱动的递归下降解析器，支持 AUTO/PACK/Sep/Fix/Block 五种 Excel 布局。`Block` 模式支持嵌套跨行（靠"前一列必须为空"作为视觉标记，`VTableParser.java:84-94`，人工保证）。

**双 schema 模式**：`CfgValueParser` 同时接收 `subSchema`（tag 过滤后）和完整 `cfgSchema`。`ValueParser` 大量方法是 `(subXxx, cells, xxx, ctx)` 双参数——按完整 schema 解析列布局，但只填 sub schema 的字段。这是 tag 过滤功能的代价。

**外键校验**：`RefValidator`（`value/RefValidator.java`）在所有表解析完后统一校验引用完整性，支持 nullable 的三种例外（pack/sep 值、local key 同时是 PK/UK、数字 0 表示无引用）。

### 3.6 数据读取层（`configgen.data`）

`CfgDataReader`（`data/CfgDataReader.java`）两阶段并行：先并行读所有文件为原始单元格，再并行 `HeadParser`（表头）+ `CellParser`（规整矩阵）。

**`CfgSchemaAlignToData`（最微妙的类）**：双向调和 schema 与数据。Excel 有但 schema 没有的字段自动新增为 STRING；schema 有但 Excel 没有的字段删除。若对齐结果与原 schema 不同且开启 autofix，会**直接写回 cfg 文件并 reload**（`Context.java:106-109`）。

### 3.7 模板引擎（`configgen.util.JteEngine`）

双模式初始化（`JteEngine.java:22-35`）：有预编译产物则 `createPrecompiled`（零启动开销），否则动态编译并缓存到 `tmpdir/jte-classes-<cwd hash>`。预编译由 `build.gradle:45-50` 的 JTE Gradle 插件在构建期完成。

一个关键陷阱（已在记忆中）：JTE 预编译**按字节读模板**，模板工作区 CRLF 会透传到生成代码，所以 `build.gradle:55-78` 有 `normalizeJteLineEndings` 任务强制转 LF。

### 3.8 服务层（`editorserver` / `mcpserver`）

`EditorServer`（`editorserver/EditorServer.java:30`）基于 JDK 内置 `com.sun.net.httpserver`，**手写字符串路径路由**（`handle("/schemas", ...)`，且 `createContext` 是前缀匹配），用虚拟线程池处理请求。注意它默认绑 `0.0.0.0`、零认证、CORS 配置非法——安全暴露见 §5.1。`CfgMcpServer` 提供 Schema/ReadRecord/WriteRecord/Search 四个 MCP 工具供 AI 调用，其 `startStreamableServer` 是真正阻塞的（`HttpServer.start()` 本身非阻塞）。

---

## 4. 架构亮点（值得肯定的设计）

| # | 设计 | 体现 | 为什么好 |
|---|------|------|---------|
| 1 | **sealed 接口 + 模式匹配** | schema 全部类型层次 | 编译期穷尽性，新增变体编译器报错所有未覆盖 switch；Java 25 现代特性教科书级实践 |
| 2 | **Parameter 双实现** | `Generator.java:18-20` | 一次定义，三处复用（运行/帮助/GUI），消除样板 |
| 3 | **Source 溯源** | `CfgValue.java:56` | 每个 Value 带原始 cell 定位，错误精确到格子，调试体验极佳 |
| 4 | **错误收集而非异常** | `CfgSchemaErrs`/`CfgValueErrs` | 一次运行报出所有错误，策划迭代效率高 |
| 5 | **不可变替换的热重载** | `WatchAndPostRun` | 新建 Context + volatile 替换，规避就地修改的并发地狱 |
| 6 | **JTE 预编译进 jar** | `build.gradle:45-50` | 干掉 ~1.6s 启动开销，动态回退有跨项目缓存 |
| 7 | **并行解析贯穿全程** | schema/data/value 都用 workStealingPool | 大表场景吞吐高 |
| 8 | **`assureNoExtra()` 拼写检查** | `ParameterParser.java:43-47` | 利用破坏性读残料自动检测未知参数，用户体验好 |
| 9 | **外键三种模式 + nullable 例外** | `RefKey`/`RefValidator` | 覆盖游戏配表的常见引用约定（含"0 表示无"） |
| 10 | **enum 自动转外键** | `CfgSchemaResolver.java:262-296` | 策划写 `quality:equip.rank` 即得枚举校验，人体工学好 |

---

## 5. 改进建议

> 本节已经过多视角评审与源码核实（2026-07），修正了初版中的事实错误并补充了原文档遗漏的安全维度。按"该做什么"组织，每条标注严重度与关键证据。末尾附"经评估不做"清单。

### 5.1【紧急·安全】服务暴露面与密钥收敛

原文档完全遗漏的一整块，也是最致命的现实风险——任何同网段主机或恶意网页可篡改/删除配置：

- **EditorServer 默认绑 0.0.0.0 + 零认证**（`EditorServer.java:60`）：改为默认绑 `127.0.0.1`，提供绑定地址参数；写/删接口（recordAddOrUpdate / recordDelete，`EditorServer.java:218-239`）加最小认证与 CSRF 防护。
- **CORS 致命组合**（`EditorServer.java:293-307` 设 `Allow-Origin:* + Allow-Credentials:true`，浏览器规范明令非法）：credentials 改 false，或 origin 改白名单。
- **请求体无上限 + 无限流**（`EditorServer.java:117/201/257` 的 `readAllBytes`、`:76` 虚拟线程池）：加 `maxBodySize` 与 rate limit。
- **XOR "加密"是玩具级混淆**（`XorCipherOutputStream` 循环复用密钥、无 IV/nonce/MAC）+ **密钥从 CLI 参数传入**（进程列表 / shell history 可见）：密钥改环境变量读取；bytes 文件固定 magic header 是已知明文，任何人都可恢复密钥。
- **AI apiKey 明文 JSON 存储**（`AICfg.java:9-27`）、**postrun bat 内容被当 `-gen` 指令解析执行**（`WatchAndPostRun.java:157-168`）：同步收敛。

### 5.2【高·正确性】Context 缓存三处修复

- `makeValue`（`Context.java:178-204`）的 check-then-act 非原子（两次 volatile 读 + 多次 volatile 写，无锁）→ 加 `synchronized`。
- **缓存键不含 `allowErr`**（`Context.java:178-181`）→ 升级为 `(tag, allowErr)` 元组。EditorServer 用 `makeValue(tag, true)`（`EditorServer.java:94`）填缓存后，生成器的 `makeValue(tag)`（默认 allowErr=false）会命中缓存、**绕过 `valueErrs.checkErrors` 直接返回带错的值**——这是比竞态更确定的正确性 bug。
- `WatchAndPostRun.context`（`WatchAndPostRun.java:36`）非 volatile，reload 写 / postRun 读跨虚拟线程不可见 → 加 volatile。
- 保持单槽缓存（`Context.java:183` 注释意图是"尽快 gc"，多槽违背此意）。

### 5.3【高·结构】包下沉斩断反向依赖

真实存在的循环只有两条：`ctx ↔ gen`、`schema ↔ data`；外加下层（data/value/schema）反向依赖 ctx。最高 ROI 的单点操作：

- `HeadRow`/`HeadRows`/`DirectoryStructure`/`ExplicitDir` 从 `ctx` 下沉到 `data`——这四个本属数据层输入的类住错包，导致 38+ 文件被迫 `import configgen.ctx`。一次机械搬迁斩断绝大多数下层→ctx 反向边，且是未来独立测试 ctx、乃至 JPMS 的前提。
- `WatchAndPostRun`/`Watcher`/`WaitWatcher` 从 `ctx` 移到 `gen/watch`，消除 `ctx ↔ gen` 循环（它们 `import gen.Generator` 用于解析 postrun 脚本里的 `-gen` 指令）。
- 删除 `value.ValueRefCollector` 对 `gen.Generator` 的死 import（正文零引用，非真实依赖——初版曾误把它当作 value→gen 反向边的证据）。
- `value.CfgValueParser` 把 `Context` 作 record 字段（`CfgValueParser.java:18-20`）是更强的耦合，可抽 `ValueContext` 接口解耦，但优先级低于上面两项。
- 注意 `WatchAndPostRun` 是 enum 单例持可变状态（`WatchAndPostRun.java:34-36`），移包前确认无 `LocaleUtil` 资源 key 依赖。

### 5.4【高·功能】Schema 破坏性变更检测（最小版）

原文档里唯一能阻止真实线上数据损坏的功能缺口。当前 Bytes 格式只有"schema 长度标记 + 可选内嵌 schema"（`Context.java:94-97`），无兼容矩阵；策划删一列或改字段类型，对齐流程会默默改写 schema 重出 bytes，旧客户端读新包可能崩。

- 先做最小版：只检测"删字段 / 改类型 / 改主键"三类破坏性变更并 fail，不做 Avro 式完整 resolution。
- 兼容矩阵需与策划流程对齐。

### 5.5【中】构造函数副作用剥离 + autofix 开关

- 加 `-noautofix` 参数（默认仍 autofix，保留"Excel 改表头自动同步 schema"的策划工作流）——这是真痛点，比单纯剥离副作用更治本。
- 把写盘（`Context.java:106-107`）从构造路径挪到显式 `writeBackAlignedSchema()`，默认仍执行；收益是可测性 + 幂等性。
- 顺带消除 autofix 失败重试导致的 schema+data **读两遍**（`Context.java:103-110` 失败后 `:75-78` 又 `readSchemaAndData(false)`，大表场景开销翻倍）。
- 重构需保留 reload 的两阶段语义，`Main` 与 `EditorServer` 热重载路径都调新接口。

### 5.6【中】生成器样板收敛 + 服务/生成器抽象分离

- 抽 `runConcurrent(List<Callable>)` 到 `Generator` 基类——Java（`JavaCodeGenerator.java:127-145`）/ CS（`CsCodeGenerator.java:95-107`）/ Lua 的 `ExecutorService + invokeAll + 异常解包` 样板逐字重复。
- **不引入** init/cleanup 生命周期 hook（当前各生成器并不真需要，YAGNI）。
- `Main.run` 把 `server`/`mcpserver` 排到最后执行——真正阻塞的是 `CfgMcpServer.startStreamableServer`，而非 EditorServer（`com.sun.net.httpserver.start()` 非阻塞，`generate()` 正常返回）。
- 拆 `Service` 抽象时，把 `bindAddress` / `auth` / `maxBodySize` / `rateLimit` 作为一等契约（与 5.1 安全收敛结合）。
- 路由用 `server.createContext(path)` 是**前缀匹配**，`handle("/notes")` 会误匹 `/notesXYZ`——加边界匹配。

### 5.7【中】可观测性 + 服务层测试

- `Logger` 全 static 非 volatile 无同步（`Logger.java:32-38`）→ 加并发同步 + 结构化 + 级别分级；EditorServer logging filter 补状态码/耗时，异常别 `e.toString()` 丢堆栈（`EditorServer.java:270-289`）。
- `EditorServer.java:56` `generate()` 里的同步 `System.gc()` → 移除或异步（Full GC 卡编辑器启动，cfgeditor 用户可感知）。
- **服务层零测试**（71 测试 grep `EditorServer`/`McpServer`/`WriteRecord`/`RecordEdit` 零命中）→ 补端到端 HTTP + 并发 + 安全测试，这是最大暴露面。

### 5.8【中】构建与依赖治理

- `ArgParser` 值不能含逗号/冒号/等号（无转义，`ArgParser.java:11-14`）、key 静默小写（未文档化，`ArgParser.java:34`）→ 加值转义 + key 大小写文档化（**不引入 picocli**，见末尾）。
- 引入 OWASP dependency-check / dependabot——fastjson2/poi/simple-openai 是 CVE 高频库。
- `fastjson2` 用 `sun.misc.Unsafe`（`build.gradle:95,171` 的 `--sun-misc-unsafe-memory-access=allow`），JEP 471 在 JDK 25 已弃用——未来 JDK 升级地雷，盯版本。
- `JteEngine` 用 `String.hashCode()` 做临时目录 key（`JteEngine.java:44`）→ 哈希碰撞会致跨项目模板缓存污染，换更稳的 key。

### 5.9【低】清理项

- `Tools`/`Generators` 孪生类（30 行近 90% 重复）→ 合并为 `Registry<T>`。
- `VStruct.equals` 用 schema 引用相等（`CfgValue.java:129`）→ 跨 schema 实例比较静默失败，修。
- 静态可变 `providers`（`Generators.java:13`）测试间全局泄漏 → 测试清理或实例化。
- `ValueVerifyTool`/`ValueInspectTool` 绕过 tag 直 `makeValue` → 先确认是 feature（verify 要校验完整数据，过滤 tag 反而漏检）再决定是否改。

### 5.10【低】Metadata 最小拆分

- 把 `_span`/`_hasRef`/`_hasBlock`/`_hasMap`/`_hasText` 五个派生态从 `Metadata` 抽到独立 `SchemaAnnotation` 旁路对象——斩断最丑的 `copyWithoutState()` 补丁（`Metadata.java:49-55`），读写格式零影响。
- 强类型化（nullable/entry/enum 升级为 sealed `FieldAttribute`）**延后**到有新字段属性功能驱动——现有 sealed `MetaValue` + `copyWithoutState` + `reservedTags` 三道防线已缓解大半，而 Metadata 是全项目触手最广、回归风险最高的区域，71 测试覆盖不住 MetaValue 变体组合，无功能驱动不动。

### 经评估不做（避免后续反复讨论）

- **ServiceLoader SPI 插件化**：单仓单 jar 内部工具，零第三方扩展需求；且 fatJar 用 `duplicatesStrategy=EXCLUDE`（`build.gradle:114`）会静默丢弃 `META-INF/services`，**技术上不可行**。
- **picocli 迁移**：要推翻本文 §3.2 盛赞的 Parameter 双实现，只为修两个 ArgParser 小坑，不值。
- **Lua 生成器模板化**：Lua 已有性能投资（ThreadLocal 缓冲复用 `LuaCodeGenerator.java:47-49`、isLangSwitch 刻意串行 `:104-117`、并发脚手架 `:149-178`），模板化会丢失或重做；Lua 表格式结构化强，模板收益不如 Java/CS。改为在文档声明"Lua 是有意的例外"。
- **统一 Bytes/Json 旧文件清理**：Bytes 输出固定文件名全量覆盖、Json 是逐记录增量语义，不清理是**正确**的，强行统一会引入新 bug。
- **JPMS**：单 jar 内部工具纯构建复杂度税；Java 25 + fat jar + jte 预编译 class 组合下 `module-info` 落地要先验证 jte 生成代码能否进 named module，工作量被低估。先做 5.3 斩边即可。

---

## 6. 同类项目借鉴

### 6.1 最直接对标：Luban（focus-creative-games/luban）

国内 C# 生态最主流的游戏配表方案，多源 → 多语言代码 + 多格式数据。
- **借鉴点**：明确的 generation pipeline 分层（数据源 → 类型系统 → 校验 → 渲染）；bean 继承多态；用 **scriban 轻量模板**让新语言接入成本低。cfggen 用 JTE 预编译是另一种务实解法，可对比。
- **cfggen 已胜出**：sealed 类型系统比 Luban 的类型表达更严谨；Source 溯源调试体验更好。

### 6.2 插件架构范本：Protocol Buffers / protoc

业界事实标准的 schema + 多语言代码生成。
- **借鉴点**：插件协议极优雅——插件只是读 stdin、写 stdout 的普通程序（`CodeGeneratorRequest`/`Response`），每语言一个独立插件，`--plugin=protoc-gen-<name>` 发现。schema 与生成器彻底解耦。
- **对 cfggen**：当前硬编码注册对内部工具够用；若未来真出现非 Java 生成器，远期可开放 protoc 风格 stdio 协议（比 ServiceLoader 更通用，且不受 fatJar 合并策略影响）。

### 6.3 Schema 演进：Buf + Apache Avro

- **Buf**：`buf breaking` 做**破坏性变更检测**（对比新旧 schema 判断前后兼容），`buf lint` 风格规则，Buf Schema Registry 集中托管。
- **Avro**：规范的 **schema resolution 算法**——按字段名匹配、default 回退、aliases、类型提升；明确的 backward/forward/full 兼容模式。
- **对 cfggen**：这正是 §5.4 缺的一环。引入 schema diff + 兼容性校验，避免策划删列把线上旧 bytes 读崩。

### 6.4 约束统一：CUE（cuelang）

强类型、基于约束的配置语言。
- **借鉴点**：核心是 **unification（合一）**——schema、策略、值都是"约束"，合并求交；约束可声明字段相等、数值范围、枚举，且可跨文件分层合并。
- **对 cfggen**：外键校验（`->`/`=>`）本质是**跨表约束**的硬编码实现。可借鉴 CUE 把约束升级为可组合、可分层、可声明的一等公民，把"范围约束、枚举约束、跨表相等"统一进 `verify` 流水线。

### 6.5 零拷贝序列化：FlatBuffers / Cap'n Proto（远期参考）

- **借鉴点**：vtable + 标量对齐 / 指针编码 + arena，编码解码近乎零耗时；schema evolution（加字段向后兼容）。
- **对 cfggen**：cfggen 的 `ConfigOutput`/`StringPool`/`LangTextPool` 是自研流式格式。零拷贝对"游戏运行时加载"有吸引力，但 schema 变更更脆弱。短期 cfggen 的"内存一次性解析进结构体"对多数项目够用；**借鉴重点是它的字段 offset/版本化演化设计，而非零拷贝本身**。

### 6.6 Java CLI：picocli（仅供参考，当前不迁移）

注解驱动、自动 usage、子命令天然适合 `-gen java/cs/ts` 分发。但 cfggen 的 Parameter 双实现已覆盖运行/帮助/GUI 三处，迁移收益不抵推翻成本（见 §5 末尾"经评估不做"）。

### 6.7 增量生成：Roslyn IIncrementalGenerator + Buf module cache

- **借鉴点**："输入指纹 → 受影响输出集合"的传播模型 + 磁盘缓存复用。
- **对 cfggen**：当前是全量生成。增量生成的首个落地场景是 **EditorServer + WatchAndPostRun 热重载**（每次 Excel 改动都 new Context 全量重解析），而非 CLI 全量——绑定编辑器迭代响应来做，价值最高。已有 `CachedFiles`/`CachedIndentPrinter` 输出缓存作为基础。

---

## 7. 改进路线图

> 已经过多视角评审修正。每项标置信度与主要风险。

### 短期（1-2 周，低风险）

| # | 任务 | 对应 | 置信度 | 主要风险 |
|---|------|------|:---:|------|
| **S0a** | EditorServer 绑 127.0.0.1 + 移除 CORS 致命组合 + 写删接口加认证 + 请求体上限/限流 | 5.1 | 高 | 若有远程访问需求需提供绑定地址参数 |
| **S0b** | XOR cipher 密钥改环境变量 + AI apiKey 同步处理 | 5.1 | 高 | 旧调用方需迁移 |
| **S1** | makeValue synchronized + 缓存键升 (tag,allowErr) + WatchAndPostRun.context volatile | 5.2 | 高 | 保持单槽 |
| **S2** | 构造函数剥离写盘 + 加 `-noautofix` 开关 + 消双倍读取 | 5.5 | 高 | 保留 reload 两阶段语义 |
| **S3** | HeadRow 等下沉到 data + WatchAndPostRun 移到 gen/watch + 删死 import | 5.3 | 高/中 | 移包前搜反射/资源路径硬编码包名 |
| **S4** | 抽 runConcurrent 到 Generator 基类 | 5.6 | 高 | 无 |
| **S5** | Registry 合并 + VStruct.equals 修 + 静态注册表测试清理 | 5.9 | 高 | ValueVerifyTool 绕 tag 需确认是 feature |
| **S6** | editorserver 路由加边界匹配防前缀碰撞 | 5.6 | 高 | 无 |

### 中期（1-2 月）

| # | 任务 | 对应 | 置信度 | 主要风险 |
|---|------|------|:---:|------|
| **M1** | 服务层测试补齐（EditorServer/MCP 端到端 HTTP + 并发 + 安全） | 5.7 | 高 | 无 |
| **M2** | EditorServer System.gc() 移除 + Logger 并发同步与结构化 | 5.7 | 高 | 日志格式变更影响下游解析 |
| **M3** | Schema 破坏性变更检测最小版（删字段/改类型/改主键 → fail） | 5.4 | 高 | 兼容矩阵需与策划流程对齐 |
| **M4** | 拆 Service 抽象（bindAddress/auth/maxBodySize/rateLimit 一等契约） | 5.6 | 中 | Main.run 两批执行顺序需保留 postrun/watch 注册时机 |
| **M5** | ArgParser 加值转义 + key 大小写文档化 | 5.8 | 高 | 转义符兼容现有脚本 |
| **M6** | CVE 审计（dependency-check / dependabot） | 5.8 | 高 | 无 |

### 长期 / 可选

| # | 任务 | 对应 | 置信度 | 主要风险 |
|---|------|------|:---:|------|
| **L1** | Metadata 派生状态抽 SchemaAnnotation | 5.10 | 中 | 牵动 CfgReader/CfgWriter/生成器，需序列化测试 |
| **L2** | 增量生成（首场景=EditorServer+WatchAndPostRun 热重载） | 6.7 | 中 | 指纹传播模型设计成本 |
| **L3** | Metadata FieldAttribute 强类型化 | 5.10 | 低 | 等新字段属性功能驱动 |
| **L4** | JPMS | 5.3 | 低 | 第三方扩展出现 + jte 进 named module 验证后再说 |

### 如果只做 3 件事

1. **S0a 安全收敛**——唯一能导致线上配置被任意网页/同网段主机 CSRF 篡改删除的现实风险，且原文档完全漏掉。
2. **S1 缓存三合一**——`allowErr` 缓存污染是比竞态更确定的正确性 bug，几行改动、零回归。
3. **S3 包下沉**——一次机械包路径搬迁斩断 38+ 文件被迫 `import configgen.ctx` 的根因，全文 ROI 最高的结构性操作，且是后续斩循环与模块化的前提。

---

## 8. 教学要点速记

> 这一节把全文提炼成可带走的架构教训，适合作为团队内部分享提纲。

1. **Sealed 类型 + 模式匹配是 Java 25 的杀手锏。** cfggen 的 schema 层是教科书级实践——编译期穷尽性胜过十句注释。新项目定义"闭合的类型变体"时，首选 sealed 接口而非继承体系。

2. **"声明即文档"可以消除样板。** Parameter 双实现让一次 `parameter.get(key, def)` 同时驱动运行、帮助、GUI 三处。代价是破坏性读的隐式语义——好的抽象省代码，但要给隐式行为写文档。

3. **God Object 的判别标准是"职责能否独立变化"。** Context 持有 6 类职责，任何一个职责变动都要改它。判定：如果删除某职责，这个类是否还能独立测试？不能就是 God Object。

4. **分层架构的金标准是"单向 DAG"。** 凡是"下层 import 上层"都是味道。cfggen 的 `HeadRow` 住错包导致下层反向依赖 ctx，是个典型教训。**JPMS / archunit 能把"约定"变成"编译期强制"。**

5. **抽象要匹配被抽象事物的生命周期。** 常驻服务与一次性生成器共享抽象是错配，服务还有不同的并发契约（`EditorServer.java:206` 的 `synchronized(this)` 泄漏进 Generator）。**当一个抽象的子类要违反基类的隐含契约时，说明该拆新抽象了。**

6. **"插件化"≠"可插件"。** 硬编码注册表叫"可配置"，第三方不改主仓就能扩展才叫"可插件"。是否值得引入 SPI，取决于"第三方扩展"是真实需求还是想象中的需求——cfggen 是后者，所以不做。

7. **构造函数应无副作用。** `new Context()` 会写盘覆盖用户文件——这是"构造即执行"反模式。构造只装配，副作用留给显式的 `execute()`/`save()`。

8. **被测试覆盖掩盖的并发 bug 仍是 bug。** `makeValue` 的竞态只在"当前没人并发调用"时隐形。顺序执行掩盖并发问题，是最危险的潜伏期。**volatile 不等于原子，check-then-act 永远需要锁或 CAS。** 更要警惕缓存键遗漏维度（`allowErr`）——那比竞态更确定出错。

9. **模板 vs 手写不是审美问题，是一致性问题。** 6 个生成器用模板、1 个（Lua）手写，后人会疑惑"是规则还是遗漏"。要么统一，要么显式声明例外并写明原因——Lua 属于后者（多流交错 + mid-stream cache flush 不适合声明式模板）。

10. **类型安全的敌人是"万能属性袋"。** Metadata 用 `Map<String, Value>` 表达一切，扩展性好但类型安全差。**开放的扩展性与闭合的类型安全是权衡——最佳实践是"已知语义类型化，未知扩展开放化"。** 务实路径是先把派生态旁路拆出，强类型化等功能驱动。

11. **Schema 演进是配置系统的必修课。** 定义 schema 容易，让 schema 在线演进且不破坏旧数据难。Avro/Buf 的兼容性规则值得每个数据序列化项目学习。

12. **校验/约束应该是可组合的一等公民。** 外键是"跨表约束"的特例。把范围、枚举、跨表相等等约束统一抽象，能复用同一套 `verify` 流水线——CUE 的 unification 思想是终极形态。

13. **API 语义要核实，别想当然。** 本文初版误判 `HttpServer.start()` 阻塞，连带着错评了抽象错配的严重度。"看起来合理"的技术论断也要回源码验证——多视角评审里只要一个人质疑并取证就能纠偏。

14. **安全是架构评审的一等维度，不是事后补丁。** 本文初版通篇是"分层/抽象/插件"的纯度视角，完全漏掉了服务暴露面（0.0.0.0 + CORS 致命组合 + 玩具加密 + 零认证）。一个"架构评审"不覆盖安全，等于没评审最大风险面。绑地址、认证、CORS、加密强度应当和依赖方向、抽象边界一起进评审清单。

15. **多视角评审的价值是纠错与补盲，不是附和。** 5 个视角里只要有"魔鬼代言人"（默认怀疑、逐条查源码）和"遗漏检测"（只找盲点）这两个对抗性角色，就能把单视角的盲区和想当然挤出来。N 个相同视角的评审不如 1 个刁钻的反方。

---

## 附：关键文件索引

| 主题 | 文件 |
|------|------|
| 入口/注册 | `src/main/java/configgen/gen/Main.java:64-90, 246-262` |
| 生成器抽象 | `gen/Generator.java:25`、`gen/GeneratorWithTag.java:8`、`gen/Tool.java:14` |
| Parameter 双实现 | `gen/Parameter.java`、`gen/ParameterParser.java:21`、`gen/ParameterInfoCollector.java` |
| Context 协调 | `ctx/Context.java:81-115, 173-211` |
| 热重载 | `ctx/WatchAndPostRun.java`、`ctx/Watcher.java`、`ctx/WaitWatcher.java` |
| Schema 类型 | `schema/Nameable.java`、`schema/FieldType.java`、`schema/RefKey.java`、`schema/ForeignKeySchema.java` |
| Schema 解析 | `schema/CfgSchemaResolver.java`（6 步）、`schema/Span.java`、`schema/IncludedStructs.java` |
| CFG 文法 | `schema/cfg/Cfg.g4`、`schema/cfg/CfgReader.java`、`schema/cfg/CfgWriter.java` |
| Value 解析 | `value/CfgValue.java`、`value/CfgValueParser.java`、`value/ValueParser.java`、`value/VTableParser.java` |
| 外键校验 | `value/RefValidator.java`、`value/RefSearcher.java` |
| 数据对齐 | `data/CfgSchemaAlignToData.java`、`data/CfgDataReader.java` |
| 模板引擎 | `util/JteEngine.java:22-88`、`build.gradle:45-78` |
| 服务层（安全暴露） | `editorserver/EditorServer.java:30,52-77,218-239,293-307`、`mcpserver/CfgMcpServer.java` |
| 加密（玩具级） | `util/XorCipherOutputStream.java`、`genbytes/BytesGenerator.java:124-131` |
| 异类生成器 | `genlua/LuaCodeGenerator.java`（2042 行无模板） |

---

*本文基于 2026-07 的代码快照分析，第 5、7 节经多视角评审与源码核实修正。引用的 file:line 可能随重构漂移，请以最新代码为准。*

# Bytes 二进制格式

`-gen bytes` 把配置序列化成一个紧凑的二进制文件（默认 `config.bytes`），供运行时动态加载。本篇讲文件结构、各池的作用、以及这些设计选择的**为什么**。

> 写入侧在 `genbytes` 包；对应的**读取**侧（`ConfigInput`、`SchemaDeserializer`）在 `genjava` 包，完整可运行读取示例见仓库 `example/java_ls/`。

## 文件结构

`BytesGenerator`（见 `../src/main/java/configgen/genbytes/BytesGenerator.java`）按固定顺序写四段：

```
┌───────────────────────────────────────────┐
│ 1. Schema 长度(int) + [Schema 数据]        │  int=0 表示无 schema；>0 后跟 schema 字节
├───────────────────────────────────────────┤
│ 2. StringPool（字符串去重池）              │  int count + 各 length-prefixed UTF-8 字符串
├───────────────────────────────────────────┤
│ 3. LangTextPool（多语言文本池）            │  int langCount + 每语言的 TextPool
├───────────────────────────────────────────┤
│ 4. 表数据（所有配置表）                    │  int tableCount + 逐表（name + length + bytes）
└───────────────────────────────────────────┘
```

全篇**小端序**（`ConfigOutput` 用 `ByteBuffer LITTLE_ENDIAN`）。

## 两个池

### StringPool
见 `../src/main/java/configgen/genbytes/StringPool.java`。字符串去重池：`addString` 返回已有索引或新增索引，数据里只存索引。

**为什么**：字段名、枚举名、重复值在表里大量重复。去重后数据只存整数索引，文件显著变小；代价是建文件时多一次去重、运行时按索引回查。

### LangTextPool / TextPool
见 `../src/main/java/configgen/genbytes/LangTextPool.java`。按语言分组，每种语言一个 `TextPool`（单语言文本去重池，结构同 StringPool）。`addText(String[] 各语言译文)` 往每个池各加一条，返回一个**跨语言共享的索引**。

**为什么**：把**易变的文本**（翻译会变、按语言不同）和**稳定的数据**（数值、结构）分开。于是同一份数据可以：
- 配不同语言包；
- `langSeparated` 模式下，主文件只写第一种语言文本 + 数据，其余语言各写一个 `<lang>.bytes`——支持**增量下发/按需下载语言**。

## 表数据

`CfgValueSerializer`（见 `../src/main/java/configgen/genbytes/CfgValueSerializer.java`）：先写表数量，再逐表写 `name + 长度 + bytes`。每张表**先独立序列化到一个 buffer** 再带长度写入。

**为什么带长度前缀**：运行时可以按表名定位、跳过不需要的表、或选择性加载——长度前缀是可随机跳读的前提。

表内序列化分两条路：

- 无多语言切换 → `TableSerializer`（更快，不记 pk / fieldChain）；
- 有多语言切换 → `MultiLangTableSerializer`（需要 pk + fieldChain 作为 id 去取对应语言的文本）。

**为什么分两条**：多语言切换要额外记录"这条文本属于哪行哪字段"才能回查译文。不需要时不付这笔开销——典型的"按需付代价"。

## 可选项

| 参数 | 作用 |
|---|---|
| `-gen bytes,schema` | 把 schema 也编进文件（自描述，运行时无需编译期类型即可解析）；不写则 int=0 标记无 schema，文件更小但运行时要靠生成的代码 |
| `-gen bytes,langSeparated` | 多语言拆文件（见上） |
| `-gen bytes,cipher=KEY` | 套 `XorCipherOutputStream` 做**混淆**（防一眼看穿，不是真正的加密） |

## 设计原理

1. **去重池（StringPool / TextPool）**：用"建文件时去重 + 数据存索引"换体积。配置里字符串重复率高，收益明显。
2. **文本与数据分离（LangTextPool 独立成段、可拆文件）**：数据稳定、文本易变；分离后能换语言包、增量下发，而不必重发整份数据。
3. **schema 可选嵌入**：嵌入→自描述、运行时灵活；不嵌→小、靠生成代码。按发布形态二选一，文件头一个 int 标记。
4. **表带长度前缀**：支持运行时按名跳读 / 选择性加载，而不是必须顺序全读。
5. **小端序**：匹配主流运行时（x86/ARM）的原生 int 布局，加载时**零字节序转换**。
6. **两套表序列化器**：多语言是少数情况，不为它拖累多数情况——用分支隔离代价。

## 关键类速查

| 关注点 | 主类 |
|---|---|
| 生成编排 / 文件拼接 | `BytesGenerator` |
| 值序列化 / 表长度前缀 | `CfgValueSerializer` |
| 表序列化（无/有多语言） | `TableSerializer` / `MultiLangTableSerializer` |
| 字符串去重池 | `StringPool` |
| 多语言文本池 / 单语言池 | `LangTextPool` / `TextPool` |
| 小端序输出流 | `genjava/ConfigOutput` |
| schema 序列化 / 反序列化 | `genjava/SchemaSerializer` / `genjava/SchemaDeserializer` |
| 读取侧入口 | `genjava/ConfigInput`（完整示例在 `example/java_ls/`） |

## 接下来

数据不仅能"生成出去"，还能被编辑器 / AI **写回** → [`07-write-back-and-servers`](07-write-back-and-servers.md)。

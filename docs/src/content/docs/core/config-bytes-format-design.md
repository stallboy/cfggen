---
title: config.bytes 格式优化设计
description: 以协议版本、字符串 metadata 和命名数据块组织配置包，保留现有字段编码并支持包隔离及分语言资源
sidebar:
  order: 98
---

本文记录已实现的版本化容器。Java/C# 提供多包和扩展支持，TypeScript、Go、Godot 同步提供完整单包读取入口。容器复用现有字段与池编码，提供可扩展 metadata 和有边界的命名块；参数、API 和迁移见 [实现规格与使用说明](./runtime-extension-mod-design.md)。

## 1. 改造依据

此前的固定段格式由以下内容顺序组成，现已移除：

```text
schemaLength: int32
schemaBytes[schemaLength]
StringPool
LangTextPool
tableCount: int32
repeat tableCount:
    tableName: string
    tableLength: int32
    tablePayload[tableLength]
```

旧格式的 schema 可选，长度为零表示没有。StringPool 必有；LangTextPool 包含各语言自己的文本索引映射与去重字符串池。表记录按 schema 字段顺序编码，str、text 和 union 实现名引用所属包的池。

保留现有的池去重、顺序字段编码和表级边界。当前需要修正的是：前半部分没有统一块边界，增加内容容易牵动固定读取顺序；已知表虽然有长度，但解析器没有被限制在表内；语言绑定依赖读取顺序；数据池上下文没有在所有运行时中保持包归属。

本次改造不承诺文件一定更小或加载一定更快。命名块有额外名字开销，收益在于明确的边界、扩展能力和按需读取语言/数据块的基础。字段级变长编码、压缩及行索引应在实际测量后单独决定。

## 2. 推荐的文件结构

```text
formatVersion: int32
metadata: map<string, string>
blockCount: int32
repeat blockCount:
    blockName: string
    payloadLength: int32
    payload[payloadLength]
```

文件以一个小端 int32 的 `formatVersion` 开始，随后是 metadata map 和命名块。新协议首版编号为 1，旧文件没有版本字段，必须重新导出，不进入当前读取流程。包名、依赖和业务描述继续放在 metadata 中。

`formatVersion` 只描述二进制协议，不是包内容版本或 schema 版本。它是唯一的固定前缀字段，读取器可以在理解 metadata 或块之前检查是否支持该协议。metadata 是描述，block 是二进制内容；不能把 schema、池或表数据塞进字符串 metadata。

metadata 与 block 各自承担一个职责：新增描述信息只增加键；新增独立数据内容增加命名块。未知描述键可忽略，未知可选块可按长度跳过。这不表示旧 reader 能理解新的必需语义或不兼容的记录编码。

### 2.1 基础编码

沿用现有小端编码：int32 为 4 字节，int64 为 8 字节，float 为 IEEE 754 binary32，bool 为单字节 0/1。数量和字节长度使用非负 int32。

```text
string = utf8ByteLength: int32 + UTF-8 bytes
metadata = entryCount: int32 + repeat entryCount { key: string, value: string }
```

metadata 键值、块名直接使用带长度的 UTF-8，不依赖尚未加载的字符串池。长度是 UTF-8 字节数，不是字符数；不得使用 C# BinaryWriter 自带的另一种字符串长度编码。

重复 metadata 键、重复块名均报错，不在同一文件内隐式覆盖。数量、长度与输入范围在读入边界检查，偏移计算使用足够宽的整数避免加法溢出。写出时 metadata 键排序，块和表使用稳定顺序；读取不依赖 map 顺序。

### 2.2 metadata 约定

下面仅表示解码后的 map，不表示文件中存放 JSON：

```text
pack            = desert-map
requires        = base,combat-balance
defaultLanguage = zh_cn
description     = 沙漠地图配置
```

首版约定：

| 键 | 含义 |
| --- | --- |
| `pack` | 包名；参与多包组合时必须非空 |
| `requires` | 逗号分隔的依赖包名；缺少或空字符串表示无依赖 |
| `defaultLanguage` | 使用默认文本读取入口时选择的语言；存在文本时应声明 |

包名不能包含逗号或首尾空白；CLI 还禁止分号，因为它用作多个依赖名/路径的分隔符。依赖项去除分隔处空白，拒绝中间空项、重复项及自依赖；整个值为空表示空列表。metadata 键名大小写敏感，包名按原值精确匹配。

其他 metadata 可用于工具或业务描述；支持当前协议的 reader 保留或忽略未知描述键，不影响二进制游标。metadata 不重复声明协议版本，版本判断统一使用固定前缀。

### 2.3 版本检查与升级规则

写入器记录实际协议版本，生成的读取模块明确声明支持的版本。读取时先检查版本，再进入对应的 metadata/块解析；首版仅支持 1，不支持的版本立即报错，不尝试按相近版本继续解析。

```text
配置格式不兼容：文件版本为 2，读取器支持 [1]。
请使用兼容版本导出数据，或升级并重新生成、编译读取代码。
```

新增可忽略的 metadata 描述或可选块通常不升级协议；改变 metadata 编码、块头编码、字段基础编码，或引入旧读取器不能忽略的必需语义时升级。不能以“文件版本小于等于读取器版本”推定兼容，旧协议只有在保留明确适配器时才支持。

相同协议版本不证明 schema 相同；字段布局、固定符号和最终数据约束仍需独立校验。正常新增或修改 Mod 数据不升级协议，也不要求重新生成业务读取代码。

## 3. 命名数据块

| 块名 | 内容 | 约束 |
| --- | --- | --- |
| `schema` | 现有 SchemaSerializer 的内容 | 数据包必有；仅一份 |
| `strings` | 当前包的字符串池 | 必有，可为零条 |
| `text/<language>` | 对应语言的文本索引与字符串池 | 每种语言至多一块，包内共用 textIndex 空间 |
| `.<fullName>` | 一张表的本包记录 | 每表至多一块，允许不存在或零条 |

例如：

```text
formatVersion = 1
metadata { pack=desert-map, requires=base, defaultLanguage=zh_cn }
blocks:
    schema
    strings
    text/zh_cn
    text/en
    .equip.monster
    .combat.skill
```

以上是二进制文件中的逻辑块名，不是源文件系统目录。表块名必须为 `.` 加 schema 中稳定的完整表名，不能直接使用源文件名、路径或生成后的 Java/C# 类名。因此 `config/schema.csv` 若对应逻辑表 `schema`，输出为 `.schema`，不会占用内部的 `schema` 块；同理名为 `strings` 的表输出为 `.strings`。业务表无需避开这些普通单词。

`.`、`text/` 等前缀划分块命名空间，内部块与业务表不共用裸名。相同文件内的同名表不叠加；覆盖仅在不同包之间、按调用方明确给出的包顺序发生。

不增加独立的磁盘目录表、绝对偏移、总表数头或逐行长度。块头本身已经提供名字和范围；每个 table 块内保留自己的 rowCount。

### 3.1 schema 和 strings

`schema` payload 复用现有序列化编码，块长度替代旧外层 schemaLength。新包包含所有表的布局；纯 enum 表也包含 `_Detail` 布局，避免字段换序而固定符号不变时误判兼容。当前数据包必须包含 schema 块，无需可选标记或零长度占位。

`strings` payload 保持 `count + strings[]`。仍采用包级池，保留跨表字符串去重，不拆成每表各自一份池。

### 3.2 text

每个 `text/<language>` 的 payload：

```text
textCount: int32
stringIndex[textCount]: int32[]
stringPoolCount: int32
strings[stringPoolCount]: string[]
```

这复用现有 TextPool 的主体，只移除与块名重复的语言名。不要删除 stringIndex 映射：相同 textIndex 跨语言表示同一处文本，不同语言内部的字符串去重结果可能不同。

同一包已加载的语言块必须有相同 textCount，索引须落在各自字符串池内。语言按块名匹配，再映射到生成代码要求的语言字段/列表；不能让物理块顺序决定语言含义。所需语言缺失时走明确的资源加载或报错路径，不能悄悄使用另一种语言。

文本标识是 `(包上下文, textIndex)`，不同包的相同整数索引没有关联。Java 与 C# 的文本对象或读取上下文必须保留所属池；不能把所有包的局部索引交给单一全局池解释。

### 3.3 table

表 payload 保持现有编码：

```text
rowCount: int32
records[rowCount]: 按 schema 字段顺序编码
```

int、long、float、bool、池索引、list/map 数量和 union 实现名的读取规则保持一致。块名和块长度替代旧的表名与表长度外壳，不在 payload 中再次重复。

不添加 replace/delete 操作码、不另存一份主键，也不为每行增加 offset/长度。首版所有记录都是按 schema 主键的完整记录；解码后提取主键合并足以满足目标。表块可跳过不等于表内字段可任意新增；已知记录仍必须符合布局校验。

## 4. 读取流程与扩展边界

对现有内存输入或可 seek 输入，先校验 formatVersion，再读 metadata 并扫描块头，建立轻量的 `(name, offset, length)` 范围索引，再按依赖读取：

```text
协议版本 → metadata 和块范围
→ 包名及依赖检查
→ schema 布局检查
→ strings 与需要的语言块
→ 所需表的未解析记录
→ 跨包合并、最终校验、引用解析和扩展回调
```

扫描只记录范围，不提前反序列化所有 payload，也不要求为每块复制 byte[]。Java 可使用现有 MemorySegment 的有界切片；C# 使用对应的有界 reader/缓冲区视图。视图同时保留包级池上下文。

writer 推荐输出 `schema → strings → text → table`，便于查看；reader 按块名解析，不将物理顺序变成语义。当前输入已支持全文件内存或 seek；本方案不承诺对不可回退网络流直接一次解码。

每个已知块必须在自己的范围内读取，完成后恰好消费 payloadLength。未知可选块按范围跳过。负长度、截断、跨块读取、多余 payload、重复块或完整文件尾部的未声明内容均作为格式错误处理。边界检查集中在加载阶段，不进入生成对象的普通 getter。

扩展兼容规则：

- 未知描述 metadata、独立且可选的工具块可忽略。
- `.`、`text/` 等已知命名空间不因为后缀未知就成为可忽略的任意扩展。表名、语言和 schema 之间的关系遵守调用方选择的过滤/语言契约；不支持的必需内容明确失败。
- 不能通过新增一个旧 reader 忽略的键，偷偷改变现有块的压缩方式、索引宽度或记录布局。必需的新语义需要明确的协议支持与拒绝机制。
- Mod 只包含部分 table 块是正常情况；是否缺少必需数据在最终合并结果上判断。

## 5. 分离语言文件与传输

已有 `<language>.bytes` 文件继续保留，但新格式的语言文件采用相同容器，内容只需 metadata 和对应 text 块：

```text
formatVersion = 1
metadata { pack=desert-map }
blocks:
    text/en
```

主数据包与语言资源由不同调用入口接入：语言文件补充已加载包的文本池，不作为新的数据层，不进入包名去重的 mount list。调用方提供包目录或资源读取器，沿用既有文件命名定位语言资源；不自动扫描目录生成依赖顺序。

挂接时校验 pack、语言名、textCount、索引有效性及生命周期约束。没有内容哈希时，无法证明同名且同大小的语言文件一定来自同次构建；主文件和语言资源必须成套分发。需要这些文本的配置实例应在扩展初始化之前取得资源，已发布实例不因另一组合加载而重新解释其文本索引。

现有 cipher 属于外层传输处理；逻辑字节流解码后才从 formatVersion 开始读取。首版不引入新的压缩层、块加密标记或自动算法探测。若保留旧 cipher 参数，必须明确 reader 的解码责任并保证 Java/C# 一致，不在带状态的 cipher 流上假设任意 seek 可用。

## 6. 升级

所有导出器和读取器只支持当前版本化容器，无旧格式生成或读取分支。`-gen bytes` 默认 `pack:base`，始终包含 schema；不再支持 `format` 和 `schema` 导出选项。基础包、Mod 和语言资源应一起重新导出。

Java、C#（含 Unity）、TypeScript、Go、Godot、BytesInspector 和 bytesview 使用同一布局。版本检查在 metadata 解析之前进行，失败时不回退到其他协议。升级已有项目时同步更新支持源码和加载入口，入口列表见 [运行时用法](./runtime-extension-mod-design.md)。

## 7. 实施位置与验证

| 位置 | 改造内容 |
| --- | --- |
| `genbytes/BytesGenerator.java` | 写入协议版本、metadata 和命名块 |
| `genbytes/CfgValueSerializer.java` | 将现有表名/长度外壳适配为 table 块；表内编码复用 |
| `genbytes/TextPool.java` | 提取不重复写语言名的主体编码，供新 text 块使用 |
| Java `ConfigInput`、C# `ConfigReader` | metadata 读取、块范围索引、有界读取与包级池 |
| 两端 loader/processor | 从固定顺序改为按块名读取；记录创建与最终解析分离 |
| C# client Text | 文本索引与所属包池绑定 |
| schema reader 和 BytesInspector | 统一块布局与诊断展示 |

验证先覆盖协议边界，再覆盖 Mod 行为：

1. 支持的版本正常读取，不支持的版本在读 metadata 前明确失败；metadata 为空、多项、未知键、非 ASCII 内容及重复键。
2. 任意块物理顺序、未知可选块、重复块、缺必需块。
3. 负长度、截断、读越界、未消费内容和错误池索引在块内明确失败。
4. 同一文件由 Java 和 C# 解码得到相同值；实际读取字节数一致。
5. 两个包具有相同本地字符串/text 索引，结果仍各自正确。
6. 语言块调换顺序不调换语言含义，分离资源按包挂接且数量一致。
7. 单行稀疏 Mod、复合主键、最终唯一性、固定 enum/entry 及引用解析。
8. 所有语言入口读取当前容器；不支持的版本、旧格式和已移除的格式参数明确报错。
9. 名为 schema、strings 的业务表分别写入 .schema、.strings，不与内部块冲突。

首版验收不设未经测量的大小或速度承诺。后续性能验证应分别测量生成时峰值内存、块扫描、池解码、记录创建、合并、引用解析和扩展初始化，依据真实瓶颈优化。

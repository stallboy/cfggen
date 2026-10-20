# Bytes 二进制格式

`-gen bytes` 把配置序列化为 `config.bytes`。写入侧在 `genbytes` 包，完整 Java 示例见 `example/java/`。

## 文件结构

```text
formatVersion: int32 = 1
metadata: count + repeated { key: string, value: string }
blocks: count + repeated { name: string, length: int32, payload: bytes }
```

所有整数和浮点数采用小端序。字符串为 `UTF-8 字节数:int32 + UTF-8 字节`，数量和长度为非负 int32。首个字段用于检查协议版本，包名、依赖等放在可扩展的 metadata 中。

| 块名 | 内容 |
| --- | --- |
| `schema` | 当前包采用的完整结构和固定符号，始终包含 |
| `strings` | 当前包的字符串去重池 |
| `text/<language>` | 该语言的文本索引和去重字符串池 |
| `.<完整表名>` | 当前包中的表记录，例如 `.equip.monster` |

表名加一个 `.` 区分内部块：`config/schema.csv` 对应 `.schema`，不会与 `schema` 冲突。前缀只占一个字节。块头已有名称和长度，不额外存目录、总表数或表名副本。读取器为每个块建立独立的有界游标，可跳过未知块，不依赖块的物理顺序。

`metadata` 默认含 `pack=base` 和 `defaultLanguage`，可用 `requires` 声明依赖。重复 metadata 键、重复块名及越界长度报错。仅支持当前版本；旧数据须重新导出，导出器和读取器均无格式切换选项。

所有语言的普通加载入口均按包内 `defaultLanguage` 选择文本池。多语言入口按生成代码需要的语言顺序加载；TS 的通用 `Loader.LoadBytes` 接受可选语言列表，省略或传空列表表示默认语言，生成的 `Processor.ProcessPackage` 会自动传入所需语言。Mod 构建读取依赖包时也遵循这套默认语言约定。

## 包内去重池

`StringPool` 用 `count + strings[]` 存储唯一字符串，表中的 `str` 和 union 实现名只保存索引。同一包内的表共享池，保留跨表去重；不同包的索引空间彼此独立。

每个 `text/<language>` 块编码为：

```text
textCount: int32
indices[textCount]: int32
poolCount: int32
strings[poolCount]: string
```

表中 `text` 字段保存 textIndex，各语言通过 indices 找到本语言的去重字符串。语言名已在块名中，不在内容里重复写。各语言的 textCount 必须相同。

`langSeparated` 将第一种语言保留在主包，其余语言输出为 `<language>.bytes`。这些文件采用同一容器，只含所属 `pack` 的 metadata 和相应的 text 块。读取前使用 `attach(...)` 挂接资源；C#/TS/Go 名为 `Attach`。资源与主包须成套分发。

## 表记录

每个 `.表名` 块为 `rowCount:int32 + 依次编码的记录`。字段按 schema 顺序编码，数值直接写入，str/text 写入包内索引，List/Map 写入数量和元素。结构、union 的字段编码沿用表序列化器。

`CfgValueSerializer.serializeTableBytes` 先把表写入缓冲区，`PackageSerializer` 统一写块名、长度和内容。没有多语言切换时使用 `TableSerializer`；需要按主键及字段路径查译文时使用 `MultiLangTableSerializer`。

## 读取入口

Java 先 `ConfigPackage.read(input)`，再调用 `ConfigMgrLoader.loadPackages(...)`。手动读取只有一个入口：

```java
var schema = pack.block("schema");
var table = pack.block(".equip.monster");
var translated = pack.block(".equip.monster", "en", "zh_cn");
```

只有 `.表名` 需要池，包内部按语言顺序延迟解析并缓存；每次返回独立游标。`attach(...)` 返回新包，原包不变。`bytesview` 和 `BytesInspector` 使用同一容器读取入口。

Java/C# 支持多个数据包合并、独立实例和扩展初始化；TS/Go/Godot 支持完整单包。具体入口与 Mod 语义见 [运行时用法](../../docs/src/content/docs/core/runtime-extension-mod-design.md)，完整边界约束见 [格式规格](../../docs/src/content/docs/core/config-bytes-format-design.md)。

## 导出参数

| 参数 | 作用 |
| --- | --- |
| `pack:base` | 包名，默认 `base` |
| `requires:base;balance` | 声明直接依赖 |
| `deps:base/config.bytes;balance/config.bytes` | 按顺序提供构建所需的全部依赖包 |
| `langSeparated` | 拆分额外语言资源 |
| `cipher:KEY` | 对整个容器做 XOR 混淆，读取和构建依赖输入前需解码 |

schema 始终包含，无需选项。逻辑字段编码和外层传输编码各自处理，容器不探测加密或旧数据格式。

## 关键实现

| 关注点 | 主类 |
| --- | --- |
| 生成编排 / 包装 | `BytesGenerator` / `PackageSerializer` |
| 构建期依赖合并和校验 | `PackageBuilder` |
| 表字段序列化 | `CfgValueSerializer` / `TableSerializer` / `MultiLangTableSerializer` |
| 字符串及文本去重 | `StringPool` / `LangTextPool` / `TextPool` |
| 小端序输出 | `ConfigOutput` |
| 结构序列化 | `SchemaSerializer` / `SchemaDeserializer` |
| Java 容器和有界读取 | `ConfigPackage` / `ConfigInput` |

数据写回及服务入口见 [`07-write-back-and-servers`](07-write-back-and-servers.md)。

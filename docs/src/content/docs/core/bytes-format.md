---
title: bytes 格式
description: cfggen 二进制配置文件（.bytes）的容器结构、数据编码与多语言拆分规范
sidebar:
  order: 10
---

`.bytes` 是 cfggen 导出的统一二进制配置格式，采用**小端序（Little-Endian）**与**版本化命名块容器**设计，供各语言运行时共享加载。


```

.bytes 文件（可选整体 XOR 加密）
├── formatVersion (int = 1)
├── metadata      (key/value 字符串表：pack、requires、defaultLanguage)
└── blocks        (命名字节块列表)
├── schema        表结构与固定符号签名（用于加载校验）
├── strings       全局字符串池（str 与 interface 实现类名）
├── text/         各语言文本池（text 字段的本地化内容）
└── .    各配置表的二进制记录数据

```

## 基础编码规则

所有数值类型均使用**小端序（Little-Endian）**编码，字段之间紧密排列，不做字节对齐填充。

| 类型 | 编码方式 | 说明 |
|------|----------|------|
| `bool` | 1 字节 | `1` 为 `true`，`0` 为 `false`（读取时非 `0` 即为 `true`） |
| `int` | 4 字节 | 32 位有符号整数（LE） |
| `long` | 8 字节 | 64 位有符号整数（LE） |
| `float` | 4 字节 | IEEE 754 单精度浮点数（LE） |
| `raw string` | `int` + `byte[len]` | UTF-8 字节长度前缀 + UTF-8 原始字节（用于元数据、块名、池条目） |

> 若导出时指定了 `cipher` 参数，整个 `.bytes` 文件的字节流会经过 XOR 异或加密（`XorCipherOutputStream`），读取前需先用相同密钥解密。

## 顶层容器结构

一个 `.bytes` 文件由版本号、元数据区和命名块区三部分顺序组成：

```text
Package {
    int formatVersion;                   // 当前固定为 1
    int metadataCount;
    MetadataEntry[metadataCount] {       // 写入时按 key 字典序排列，不允许重复 key
        raw string key;
        raw string value;
    }
    int blockCount;
    BlockEntry[blockCount] {             // 不允许重复 blockName
        raw string blockName;
        int byteLength;
        byte[byteLength] payload;        // 独立的块负载字节
    }
}

```

读取器解析完所有块后，要求流必须恰好到达文件末尾（无多余尾字节）。每个块会被切分为独立游标的有界视图（`slice`），解析某块时不会越界读取到后续块。

### 标准 Metadata 键

| Key | 必填 | 说明 |
| --- | --- | --- |
| `pack` | 是 | 包名（默认 `"base"`），不能为空白，不能含 `,`、`;`、控制字符或路径保留字符 |
| `requires` | 否 | 依赖包名列表，英文逗号 `,` 分隔；无依赖时不写入该键 |
| `defaultLanguage` | 主包必填 | 默认语言名；未开启 i18n 时固定为 `"default"`，开启时为翻译集合的首个语言 |

> 分离语言包（`<pack>_<lang>.bytes`）的 metadata 中仅包含 `pack` 键。

## 命名块说明

导出器按固定顺序写入以下四类命名块：

### 1. `schema`

存储当前有效数据集的结构签名（由 `SchemaSerializer` 写入），包含所有表、结构体、多态接口以及固定枚举/入口符号信息。运行时加载每个包前，都会反序列化该块并与生成代码的内置 Schema 做双向兼容性校验。

### 2. `strings`（全局字符串池）

存放所有表的 `str` 字段值以及多态 `interface` 的实现类名，按首次出现顺序去重并分配从 `0` 开始的下标。

```text
StringPool {
    int count;
    raw string[count] strings;
}

```

### 3. `text/<lang>`（语言文本池）

每个语言对应一个独立块（未启用 i18n 时为 `text/default`）。由于不同语言下同一条文本的去重结果不同，每个语言块采用**两级结构**：先存逻辑文本索引表，再存该语言专属的去重字符串池。

```text
TextPool {
    int textCount;
    int[textCount] indices;      // indices[textId] -> 该语言 pool 中的下标
    int poolSize;
    raw string[poolSize] pool;   // 本语言去重后的字符串池
}

```

读取逻辑：`text = pool[indices[textId]]`。

**一致性约束**：

* 同一个包下，所有 `text/<lang>` 块的 `textCount` 必须完全一致。
* `indices` 中的每个下标必须落在 `[0, poolSize)` 范围内。

### 4. `.<tableName>`（表数据块）

表数据块名称以 `.` 开头（例如 `.task`、`.equip.item`），按表名排序依次写入。

```text
TableBlock {
    int recordCount;
    Record[recordCount] records;  // 按表字段顺序依次序列化
}

```

## 值序列化规则

表记录与嵌套结构均按 Schema 定义的字段顺序紧邻写入，不写入字段名或类型标记：

| 字段类型 | 序列化布局 |
| --- | --- |
| `bool` / `int` / `long` / `float` | 直接写入对应基础类型字节 |
| `str` | `int stringIndex`（指向 `strings` 块的下标） |
| `text` | `int textIndex`（指向 `text/<lang>` 中 `indices` 数组的下标） |
| `struct` | 按字段顺序递归展开写入，无额外头部 |
| `interface` | `int implNameIndex`（实现类名在 `strings` 池中的下标）+ 该实现类的所有字段 |
| `list<T>` | `int count` + `count` 个 `T` 元素 |
| `map<K, V>` | `int count` + `count` 对 `(K, V)` 键值 |

## 多语言拆分模式（`langSeparated`）

默认情况下，所有语言的 `text/<lang>` 块都打在主包 `<pack>.bytes` 中。开启 `langSeparated` 参数后：

* **主包 `<pack>.bytes**`：仅内嵌默认语言（第 0 个语言）的 `text/<defaultLang>` 块。
* **附加语言包 `<pack>_<lang>.bytes**`：其余每种语言单独生成一个 `.bytes` 文件，复用相同容器格式，但仅包含 `metadata: { pack }` 与单个 `text/<lang>` 块。

运行时可通过 `ConfigPackage.attach(langPack...)` 将附加语言包挂载回主包。挂载时会严格校验：

1. 附加包的 `pack` 名与主包一致；
2. 附加包非空，且只包含 `text/` 前缀的块；
3. 不能重复挂载同一语言块。

## 导出参数速查

通过 `-gen bytes` 导出时支持的参数：

| 参数 | 默认值 | 说明 |
| --- | --- | --- |
| `dir` | `.` | 输出目录 |
| `pack` | `base` | 包名，决定输出文件名 `<pack>.bytes` |
| `cipher` | 空 | 可选的 XOR 加密密钥，留空则不加密 |
| `langSeparated` | 关闭 | 开启后将非默认语言拆分为独立的 `<pack>_<lang>.bytes` |
| `requires` | 空 | 声明依赖的包名列表（`,` 或 `;` 分隔），用于增量 Mod 包 |
| `deps` | 空 | 构建期参与合并校验的依赖 `.bytes` 路径列表（`;` 分隔） |

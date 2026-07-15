# 值模型（value 层）

value 层把 data 层的**原始单元格**和 schema 的**类型系统**组合起来，产出**类型化、外键已解析**的运行时值模型 `CfgValue`。这是生成器和校验器真正消费的对象。

本层也是 **i18n 直接替换模式**（`-i18nfile`）的接入点；多语言切换模式（`-langswitchdir`）不在值层处理，见 [`09`](09-i18n.md)。

## 值类型层次

`CfgValue`（见 `../src/main/java/configgen/value/CfgValue.java`）用一套 sealed 类型树表达所有值：

```
Value
├── SimpleValue
│   ├── PrimitiveValue
│   │   ├── VBool / VInt / VLong / VFloat
│   │   └── StringValue
│   │       ├── VString
│   │       └── VText        ← i18n 接入点（original + translated）
│   └── CompositeValue (abstract)
│       ├── VStruct          ← 带 note / fold（cfgeditor 编辑器元数据）
│       ├── VInterface       ← 持有一个具体 impl 的 VStruct（多态在值层的表达）
│       ├── VList
│       └── VMap
└── ContainerValue (标记接口: VList, VMap)
```

`VTable` 不只是 `List<VStruct>`，它还**预建了索引**：

| 字段 | 用途 |
|---|---|
| `primaryKeyMap` | `Value → VStruct`，主键查行 |
| `uniqueKeyMaps` | 每个唯一键的 `Value → VStruct` |
| `enumNames` / `enumNameToIntegerValueMap` | enum 表的名字↔整数映射 |

几个**非显然**点：

- **每个 `Value` 都带 `Source`**（`CompositeValue.source` / 各 primitive 的 `source`）——回溯到原始单元格，报错能指到 Excel 的 A1。
- **`VInterface` 持有一个具体 impl 的 `VStruct`**：多态在值层就是一个"接口值 + 它实际是哪个 impl"。生成时据此产出判别字段。
- **`VStruct` 带 `note` / `fold`**：这俩不是配置语义，是 **cfgeditor** 用的（笔记、折叠状态），存在值里是为了和 json 存储一起往返。
- **`VText` 分 `original` / `translated`**：默认 `value = original`，`setTranslated` 命中译文后 `value` 切到译文。原文始终保留（做翻译键、做 lang-switch）。
- **`VFloat.repr()`**：来自单元格时返回**原始文本**（而非 `String.valueOf(float)`），避免 `1.0`↔`1` 之类的浮点格式漂移。
- **`CompositeValue.shared`**：lua 生成内存优化——同一张表里相同的复合值共享实例。

## 解析（`CfgValueParser`）

见 `../src/main/java/configgen/value/CfgValueParser.java`。`parseCfgValue` 对 `subSchema` 的每张表**并发**解析：

- 有单元格数据（`dTable != null`）→ `VTableParser` 解析 Excel/CSV；
- 否则（JSON 表）→ `VTableJsonParser` 直接读 JSON；
- 解析完立即 `TextValue.setTranslatedForTable`（套用直接替换 i18n）；
- 全部表解析完，跑一次 `RefValidator` 做**跨表外键校验**。

要点：构造时 `requireResolved()` 守卫（schema 必须 resolved 才能解析值）；并发粒度是**表**；profile 模式下逐表计时（>10ms 才打印）。

## 外键校验（`RefValidator`）

见 `../src/main/java/configgen/value/RefValidator.java`。遍历所有 `VStruct`，对每个外键检查 local 值能否在目标表的索引里找到。**nullable 外键有一套细微规则**（和策划约定的语义）：

| 情况 | 处理 |
|---|---|
| local 值来自 pack/sep/json 且 nullable | 跳过 |
| 格子有值，local key 是本表自己的主键/唯一键 且 nullable | 允许"有值但不引用" |
| 数字为 0 且 nullable | 视作"无引用"，跳过 |
| 格子有值、不属上述豁免 | 必须能在目标表找到，否则 `ForeignValueNotFound` |
| 格子空、非 nullable | `RefNotNullableButCellEmpty` |
| `=>` list 外键 | 列表里**每个**元素都要能找到 |
| map 外键 | 每个 value 都要能找到 |

查找用 `ValueUtil.getForeignKeyValueMap`——即目标表 `VTable` 预建的主键/唯一键索引，**O(1)**。错误进 `CfgValueErrs`（收集不抛，见 [`08`](08-errors-and-validation.md)）。

## i18n 桥（`TextValue`，仅直接替换模式）

见 `../src/main/java/configgen/value/TextValue.java`。`-i18nfile` 模式下，每张表解析后按 **(主键 `packStr`, `fieldChain`, `original`)** 三元组查 `LangTextFinder`，命中就 `VText.setTranslated`。

两个优化：

- 用 schema 预计算的 `HasText` 剪枝——**整张表没有 text 字段就直接跳过**，不遍历。
- 只在直接替换模式（`LangTextFinder != null`）下跑；lang-switch 模式不在此处理。

> 两种 i18n 模式的完整对比见 [`09`](09-i18n.md)。

## 设计原理

1. **为何值独立成层**：把"类型解释 + 引用解析"从数据读取和代码生成里剥离。data 只搬运、generate 只消费，类型/引用的复杂逻辑集中在此——换数据源或换目标语言都不碰这里的核心（四层分离，见 [`01`](01-architecture-overview.md)）。
2. **为何 `VTable` 预建索引**：外键校验和部分生成都要频繁按主键/唯一键查行。预建 `Map` 把 O(n) 查找变 O(1)；代价是建值时多算一次索引，但相比校验/生成的多次访问，值得。
3. **为何每个 `Value` 带 `Source`**：错误必须能指回 Excel 单元格（A1），否则策划无法定位。`Source` 是贯穿 data→value 的回溯链。
4. **为何 `VText` 分 original/translated**：直接替换模式既要保留原文（当翻译键、支持 lang-switch），又要把"生成时实际用的值"切到译文——双字段解耦，避免二选一。
5. **为何 nullable 外键规则这么细**：配置惯例里 `0` / 空 / 自引用 都可能表示"无引用"。不在校验里显式编码，就会要么误报、要么漏报。这套规则是和策划约定的语义，不是任意的。
6. **为何 `RefValidator` 收集不抛**：一次跑完全表所有引用错误，而不是第一个就停。详见 [`08`](08-errors-and-validation.md)。

## 关键类速查

| 关注点 | 主类 |
|---|---|
| 值容器 / 类型树 | `CfgValue`、`VTable`、`VStruct`、`VInterface`、`VText`… |
| 解析编排（表级并发） | `CfgValueParser` |
| Excel/CSV 值解析 | `VTableParser` |
| JSON 值解析 | `VTableJsonParser` |
| 外键校验 | `RefValidator` |
| i18n 直接替换桥 | `TextValue` |
| 错误载体 | `CfgValueErrs`（见 [`08`](08-errors-and-validation.md)） |

## 接下来

值就绪后，怎么渲染成各语言代码 → [`05-codegen-and-extension`](05-codegen-and-extension.md)。

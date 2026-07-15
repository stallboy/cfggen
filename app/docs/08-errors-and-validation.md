# 错误与校验

cfggen 的校验贯穿 schema 解析、值解析、外键校验三步。它的错误处理有一个贯穿始终的设计：**收集，而非遇错即抛**。

## 收集式错误机制

校验全程把错误塞进两个载体，结尾由 `checkErrors` 统一决定是否抛：

| 载体 | 严重度级别 | 用在 |
|---|---|---|
| `CfgSchemaErrs` | `Err` / `Warn` / `WeakWarn` 三级 | schema 解析（resolve、align、filter） |
| `CfgValueErrs` | `Err` / `Warn` 两级 | 值解析、外键校验 |

`checkErrors(prefix, allowErr)`（见 `value/CfgValueErrs.java`）的逻辑：

1. 按开关打印 warns / weakWarns；
2. 打印所有 errs；
3. **若 errs 非空且 `!allowErr` → 抛异常**（`CfgValueException` / `CfgSchemaException`）；`allowErr=true` 则只打印、继续。

**为什么收集不抛**：一份配表可能同时有 20 处错。遇第一个就抛，策划得"改一处 → 重跑 → 看下一处"循环 20 次；收集后一次列出全部，改完再跑。代价是校验代码要显式 `addErr` 而非 `throw`，并容忍"带着已知错误继续走完"。

## 类型化错误

每条错误是一个 **record**，带 `Source`（定位到单元格）+ 结构化字段，实现 `Msg` 接口（`msg()` 给本地化描述）。不是拼字符串。举几个 `CfgValueErrs` 里的例子：

| 错误 | 含义 |
|---|---|
| `ForeignValueNotFound(value, recordId, foreignTable, foreignKey)` | 外键目标找不到 |
| `NotMatchFieldType(source, nameable, field, expectedType)` | 单元格类型与 schema 不符 |
| `PrimaryOrUniqueKeyDuplicated(value, table, keys)` | 主键 / 唯一键重复 |
| `MustFillButCellEmpty` / `RefNotNullableButCellEmpty` | 必填 / 非空外键的格子空了 |
| `JsonTypeNotMatch` / `JsonHasExtraFields`(warn) | JSON 表结构对不上 |

**为什么类型化**：统一渲染（`msg()` 本地化）、携带精确解释数据、并天然带上 `Source`——错误能直接指到 `task[row]!B3`。

## 三级严重度与噪声控制

- `Err`：致命，阻塞生成。
- `Warn`：默认开（`-nowarn` 关）。例如"某个 struct 没被任何表引用"（`StructNotUsed`）。
- `WeakWarn`：默认关，`-weakwarn` 才显示。例如 tag 过滤时某个外键因 ref 表未被 tag 而**被忽略**（`FilterRefIgnoredByRefTableNotFound`）——这不是错，只是提示。

这样"未使用结构"是警告不是错误、"过滤丢了外键"是弱提示，不污染默认输出。

## allowErr 双模式

同一个 `checkErrors`，靠 `allowErr` 区分两种用法（呼应 [`01`](01-architecture-overview.md)）：

- **生成器**（`makeValue()` 默认 `allowErr=false`）：有错即抛，**挡住脏产物**——不会把带引用错误的配置生成成代码。
- **编辑器 / 服务**（`makeValue(tag, true)`）：只打印、继续，**带错值照常展示**给编辑器，策划边看边改。

## `Source` 贯穿定位

错误能指到 Excel 单元格，是因为 `Source`（`DCell` / `DCellList` / `DFile`）从 [data 层](03-data-reading.md)一路带到 [value 层](04-value-model.md)、再进错误。这是 [`03`](03-data-reading.md) 里"`DCell` 带 `rowId`/`col`"的回报。

## 校验 / 检索工具

这两个注册在 `Generators` 下（消费 `Context` 产出，见 [`01`](01-architecture-overview.md) 注），但本质是校验 / 检索：

| `-gen` | 类 | 作用 |
|---|---|---|
| `verify` | `ValueVerifyTool`（见 `tool/ValueVerifyTool.java`） | `makeValue()` **严格模式**——有值错即抛，本身就是一个**校验闸**；可选 `-gen verify,unreferenced` 报未被引用记录、`-gen verify,entry` 报 entry 记录（entry / enum / root 视为被引用） |
| `search` | `ValueInspectTool` | `makeValue(tag)` + `ValueInspector` 搜索 / 交互式查找值 |

典型用法：CI 里跑 `-gen verify` 当门禁——配置有引用错误就失败。

## 设计原理

1. **收集不抛**：一次跑出全部错误，而非逐个修。校验代码显式 `addErr`，容忍带着已知错走完。
2. **类型化错误**：统一渲染、带定位、带解释数据，而不是散落的字符串。
3. **三级严重度**：区分"阻塞"（Err）和"提示"（Warn/WeakWarn），并用 `-nowarn`/`-weakwarn` 控制噪声。
4. **allowErr 双模式**：生成严格、编辑宽松，同一套校验两种用法。
5. **`Source` 贯穿**：报错可定位到单元格，策划能直接找到。

## 关键类速查

| 关注点 | 主类 |
|---|---|
| schema 错误（三级） | `schema/CfgSchemaErrs`、`CfgSchemaException` |
| 值错误（两级） | `value/CfgValueErrs`、`CfgValueException` |
| 外键校验 | `value/RefValidator` |
| 校验工具 | `tool/ValueVerifyTool`（`verify`） |
| 检索工具 | `tool/ValueInspectTool`（`search`）、`value/ValueInspector` |
| 未引用 / entry 统计 | `value/UnreferencedRecordCollector`、`value/EntryRecordCollector` |

## 接下来

校验之外，配置的**国际化**怎么走 → [`09-i18n`](09-i18n.md)。

# 字段内嵌（embedding）：小结构体何时摊平进父节点表单

> 这篇讲画布上的**字段内嵌（embedding）**机制：什么 struct/interface 字段不画成独立节点，而是把它的 primitive 子字段摊平进**父节点**的编辑表单里直接改；判定规则、阈值、提取过程、以及新增元素时的展开契约。
>
> **不讲**：record → entity → node 的整体变换链（→ [`03-data-lifecycle.md`](./03-data-lifecycle.md)）、entity 模型本身（→ [`01-overview.md`](./01-overview.md) §核心概念）、每个 primitive 输入怎么渲染（UI 细节）。
>
> **锚点**：判定与提取都在 `src/domain/embedding.ts`——`canBeEmbeddedCheck`（能不能内嵌）、`extractEmbeddingFields`（怎么摊平）、`markNewItemExpanded`（新增时展开）。消费方是 `src/features/record/recordEditEntityCreator.ts`（record → entity 变换）和 `src/features/record/Record.tsx`（新增数组项）。

---

## 一、它解决什么问题

配置图里，一条 record 的字段若类型是 struct/interface，默认会画成**独立子节点**（用 React Flow 的边连过去）。但很多 struct 其实只有一两个数字、布尔字段，画成独立节点反而让图变碎、编辑要在节点间跳来跳去。

**字段内嵌**：这种「够小」的字段不画独立节点，改把它的 primitive 子字段**摊进父节点的表单**就地编辑。

```
不内嵌（展开独立节点）            内嵌（摊平到父表单）

  Parent ────┐                    Parent
             │                    ┌──────────────┐
             ▼                    │ ownField     │
          ┌──────┐                │ pos.x: 1     │  ← Pos 的字段
          │ Pos  │  pos           │ pos.y: 2     │     直接在父表单里编辑
          │ x:1  │                │ otherField   │
          │ y:2  │                └──────────────┘
          └──────┘
```

收益：图节点更少、层级更浅；小字段就地改，不用跳节点。

---

## 二、判定：什么字段会被内嵌（5 条规则）

一个 struct/interface 的**字段集合**，满足下列**任一**条件就内嵌。核心思想：**字段少、且全是 primitive**。

| 条件 | 含义 | struct 阈值 | interface 阈值 |
|---|---|---|---|
| **a** | 没有字段（空 struct） | 0 | 0 |
| **b** | 只有 1 个 primitive | 1 | 1 |
| **c** | 全是 number，数量 ≤ N | ≤3 | ≤2 |
| **d** | 全是 bool，数量 ≤ M | ≤4 | ≤3 |
| **e** | 恰好 1 个 bool + 1 个 number | 2 (1+1) | 2 (1+1) |

> struct 比 interface **更宽松**（number/bool 阈值都更大）—— struct 是具体类型、结构稳定；interface 可能有多个 impl，更保守。这个差异是刻意的，测试里有专门一条不变量锁住（`embedding.test.ts` 的「struct vs interface 阈值差异」），重构时一旦误平会立刻红。
> 阈值集中在 `embedding.ts` 的 `EMBEDDING_CONFIG`，要调内嵌规则改这一处即可。

**关键约束**：条件 b/c/d/e 都要求 `allPrimitive`（字段全是 primitive）。只要有一个 struct/interface 子字段，就**不内嵌**（展开成独立节点）。

**空 list 字段先过滤**：值为空数组 `[]` 的 `list<...>` 字段在判定前被 `filterEmptyListFields` 滤掉，不计入字段数。这会带来一个二级效果——原本「2 个 number + 1 个空 list」因 list 破坏 `allPrimitive` 不能内嵌，过滤掉空 list 后剩 2 个纯 number，反而命中条件 c 可以内嵌（非空 list 仍保留、仍会破坏 `allPrimitive` 而不内嵌）。

### 2.1 例子

| struct 定义 | 字段分析 | 命中条件 | 内嵌? |
|---|---|---|---|
| `Pos { x:int, y:int }` | 2 个 number，allPrimitive | c (≤3) | ✅ |
| `Box { x,y,z,w:int }` | 4 个 number | 超 c (≤3) | ❌ 展开 |
| `Flag { on:bool, cnt:int }` | 1 bool + 1 number | e | ✅ |
| `Name { s:str }` | 1 个 primitive | b | ✅ |
| `Empty {}` | 0 字段 | a | ✅ |
| `Wrap { pos:Pos }` | 含 struct 子字段 | 非 allPrimitive | ❌ 展开 |
| `Mixed { a:int, b:Pos }` | 含 struct 子字段 | 非 allPrimitive | ❌ 展开 |

interface 同理，但 number/bool 阈值更紧（见上表）。interface 还多一步：要先按数据里的 `$type` 解析出具体 impl（见下一节）。

---

## 三、提取：判定通过后怎么摊平

`canBeEmbeddedCheck` 只回答「能不能内嵌」；真正把字段摊出来的是 `extractEmbeddingFields`，它返回一个 `{ embeddedFields, implNameToDisplay? }`，或 `null`（不可内嵌）。

- **`embeddedFields`**：每个 primitive 字段摊成 `{ value, type, name, comment }`，父表单就照这个列表渲染输入框。
- **默认值兜底**：字段缺失或为 `null` 时按类型回退——`bool→false`、`int/long/float→0`、`str/text→空串`；有显式值则优先用显式值。
- **空 struct**：可内嵌但 0 字段时返回 `{ embeddedFields: [] }`（**非 `null`**），刻意区分「可内嵌但无字段」与「不可内嵌」两种语义。
- **interface 的 `$type` 解析**：interface 字段先 `resolveImpl`——读数据里的 `$type`，`split('.')` 取末段定位 impl struct；`$type` 缺失或指向不存在的 impl → 直接判不可内嵌（`null`），这是给后端脏数据 / 新旧 schema 不一致的兜底。
- **impl 名要不要显示**：interface 命中 `defaultImpl` → `implNameToDisplay` 为 `undefined`（无需标注）；命中非默认 impl → 带上 impl 名（父表单要标出「这是哪个实现」）。

判定（`canBeEmbeddedCheck`）与提取（`extractEmbeddingFields`）共用同一套 `resolveImpl` 与阈值配置，所以「判为可内嵌」和「真能摊出来」不会漂移。

---

## 四、新增元素的展开契约（`$fold`）

「能内嵌」的字段在画布上有两种呈现：**压缩成一行**（EmbeddedSimpleStructuralItem）或**展开成可编辑子节点**，由数据上的 `$fold` 标记决定。

这带来一个交互陷阱：用户点「添加」/「前插入」新建一个 list 元素时，新对象还没有 `$fold`（`undefined`）。若该元素类型恰好可内嵌，渲染端会把 `undefined` 当作「内嵌压缩」，于是新元素显示成一行、用户得再点一次展开按钮才能编辑——与「添加后立即编辑」的意图相悖。

`markNewItemExpanded` 就是堵这个缝的：手工新增元素时，若类型可内嵌，**显式置 `$fold=false`**，让它直接以可编辑节点展开。两条约束：

- **只对可内嵌元素写 `$fold`**：永不内嵌的对象（`canBeEmbeddedCheck=false`）不需要这个 UI 标记，写进去只会在提交载荷里残留无意义字段。
- **与切换 impl 同源**：`interfaceOnChangeImpl` 切换实现后也置 `$fold=false`，道理一样——刚操作的元素应立即可编辑。

调用点：`recordEditEntityCreator.ts` 与 `Record.tsx` 在构造新元素默认值时各调一次 `markNewItemExpanded`。

---

## 五、配置入口

所有阈值和开关集中在 `embedding.ts` 的 `EMBEDDING_CONFIG`：

```
struct:     { 空:0, 单primitive:1, number≤3, bool≤4, e:{1bool+1number} }
interface:  { 空:0, 单primitive:1, number≤2, bool≤3, e:{1bool+1number} }
common:     { filterEmptyLists: true }
```

要调整内嵌规则（放宽/收紧某类字段的内嵌条件），改这一处即可，测试里的「阈值快照」会先红提醒。struct 与 interface 是两套独立阈值，别误当成一套。

---

## 一句话速记

- **内嵌 = 够小的 struct/interface 字段不画独立节点，primitive 子字段摊进父表单就地改**。
- **5 条规则**（空 / 单 primitive / 全 number / 全 bool / 1bool+1number），核心是「字段少且全是 primitive」；struct 比 interface 宽松。
- **判定 `canBeEmbeddedCheck`、提取 `extractEmbeddingFields`** 共用阈值与 `$type` 解析，语义不漂移；不可内嵌返回 `null`，空 struct 返回 `[]`。
- **新增可内嵌元素置 `$fold=false`**（`markNewItemExpanded`），保证「添加后立即编辑」。
- **改规则只改 `EMBEDDING_CONFIG` 一处**，struct/interface 两套阈值别混。

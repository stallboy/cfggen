# Embedding（字段内嵌）机制

## 一句话

当某个 struct/interface 类型的字段「足够小」（字段少且全是 primitive），不把它画成独立节点，而是把它的 primitive 子字段**摊平到父节点的编辑表单**里直接改——图节点更少、编辑更顺。

## 它解决什么问题

配置图里，一个 record 的字段如果类型是 struct/interface，默认会画成**独立子节点**（用 React Flow 的边连过去）。但很多 struct 其实只有一两个数字、布尔字段，画成独立节点反而让图变碎、编辑要跳来跳去。

**Embedding（内嵌）**：这种「够小」的字段不画独立节点，改把它的 primitive 子字段摊进父节点表单就地编辑。

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

## 什么字段会被内嵌（5 条规则）

一个 struct/interface 的**字段集合**，满足下列**任一**条件就内嵌。核心思想：**字段少、且全是 primitive**。

| 条件 | 含义 | struct 阈值 | interface 阈值 |
|---|---|---|---|
| **a** | 没有字段（空 struct） | 0 | 0 |
| **b** | 只有 1 个 primitive | 1 | 1 |
| **c** | 全是 number，数量 ≤ N | ≤3 | ≤2 |
| **d** | 全是 bool，数量 ≤ M | ≤4 | ≤3 |
| **e** | 恰好 1 个 bool + 1 个 number | 2 (1+1) | 2 (1+1) |

> struct 比 interface **更宽松**（允许更多 number/bool）—— struct 是具体类型、结构稳定；interface 可能有多个 impl，更保守。
> 阈值集中在 `embedding.ts` 的 `EMBEDDING_CONFIG`，要调内嵌规则改这一处即可。

**关键约束**：所有条件都要求 `allPrimitive`（字段全是 primitive）。只要有一个 struct/interface 子字段，就**不内嵌**（展开）。值为空数组 `[]` 的 list 字段会先被过滤掉，不计入字段数。

### 例子

| struct 定义 | 字段分析 | 命中条件 | 内嵌? |
|---|---|---|---|
| `Pos { x:int, y:int }` | 2 个 number, allPrimitive | c (≤3) | ✅ |
| `Box { x,y,z,w:int }` | 4 个 number | 超 c (≤3) | ❌ 展开 |
| `Flag { on:bool, cnt:int }` | 1 bool + 1 number | e | ✅ |
| `Name { s:str }` | 1 个 primitive | b | ✅ |
| `Empty {}` | 0 字段 | a | ✅ |
| `Wrap { pos:Pos }` | 含 struct 子字段 | 非 allPrimitive | ❌ 展开 |
| `Mixed { a:int, b:Pos }` | 含 struct 子字段 | 非 allPrimitive | ❌ 展开 |

interface 同理，但 number/bool 阈值更紧（见上表）。

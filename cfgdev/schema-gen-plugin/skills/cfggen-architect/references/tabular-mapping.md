---
title: 表格映射机制
---

# 表格映射机制

配表系统支持复杂的数据结构，但 Excel 是二维表格。本文档介绍五种映射机制，解决复杂数据结构到表格的转换问题。

## 映射机制对比

| 映射方式 | 适用类型 | 占格规则 | 主要用途 |
|---------|---------|---------|---------|
| **auto** | 基本类型、struct、interface | 自动计算 | 默认映射，简单场景 |
| **pack** | struct、interface、list、map | 压缩到 1 列 | 减少列数，处理递归 |
| **sep** | struct、list | 压缩到 1 列 | 自定义分隔符格式 |
| **fix** | list、map | 固定列数 | 已知长度的列表 |
| **block** | list、map | 横向固定，纵向扩展 | 变长列表垂直排列 |

---

## auto（自动映射）

默认方式，自动计算列数。

```cfg
struct Range {
    rmin:int;
    rmax:int;
}
// Range 占用 2 列
```

---

## pack（压缩映射）

将整个数据结构压缩到 1 列，字段间用逗号分隔，嵌套用 `()` 包裹。

```cfg
struct Position (pack) {
    x:int;
    y:int;
    z:int;
}
// 数据格式："1,2,3"

list<Position> (pack)
// 数据格式："(1,2,3);(4,5,6)"
```

**递归结构必须使用 pack 打破循环**：

```cfg
interface Condition {
    struct And {
        left:Condition (pack);   // ← 必须用 pack
        right:Condition (pack);
    }
}
```

---

## sep（分隔符映射）

类似 pack，但支持自定义分隔符。

```cfg
struct Time (sep=':') {
    hour:int;
    minute:int;
    second:int;
}
// 数据格式："12:10:00"
```

**限制**：所有字段必须是基本类型。除非有特定分隔符需求，否则推荐用 `pack`。

---

## fix（固定长度映射）

固定列数 = 元素类型占用列数 × count。

```cfg
list<int> (fix=2)       // 占用 2 列
list<Range> (fix=3)     // 占用 6 列（3 × 2）
```

---

## block（块状映射）

横向固定列数，纵向可扩展。通过空单元格标记数据块边界。

```cfg
struct RewardItem {
    itemId:int;
    count:int;
}

table task[id] {
    id:int;
    rewardItems:list<RewardItem> (block=1);
}
```

**Excel 示例**：

| id | rewardItems | rewardItems |
|----|-------------|-------------|
| 1  | 1001        | 5           |
|    | 1002        | 10          |
|    | 1003        | 3           |
| 2  | 2001        | 2           |

- 第1行：id=1，开始新记录
- 第2-3行：id为空，继续属于上一条记录
- 第4行：id=2，开始新记录

### block 嵌套

每个 block 前需要有空列作为分隔标记：

```cfg
struct RewardGroup {
    groupName:str;
    items:list<RewardItem> (block=1);
}

table task[id] {
    id:int;
    rewardGroups:list<RewardGroup> (block=1);
}
```

| id | rewardGroups | rewardGroups | rewardGroups |
|----|--------------|--------------|--------------|
| 1  | 基础奖励     | 1001         | 5            |
|    |              | 1002         | 10           |
|    | 额外奖励     | 2001         | 3            |

---

## mustFill（必填约束）

强制字段必须包含有效值：列表/映射类型元素个数 > 0，其他类型单元格不能为空。

```cfg
rewardItems:list<RewardItem> (block=1, mustFill);
```

**用途**：防止 block 中关键字段为空导致意外的数据合并。

---

## 使用建议

1. **优先 auto**：简单结构用默认映射
2. **递归用 pack**：必须打破循环
3. **变长列表用 block**：需要在表格中垂直排列
4. **关键字段用 mustFill**：防止数据合并问题

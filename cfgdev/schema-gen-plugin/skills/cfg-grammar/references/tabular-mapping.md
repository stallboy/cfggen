# 表格映射机制

## 概述

配表系统支持复杂的数据结构，但 Excel 是二维表格。五种映射机制解决复杂数据到表格的转换。

## 映射机制详解

### auto（自动映射）

- **适用类型**：基本类型、struct、interface
- **占格规则**：基本类型 1 列，其他自动计算
- **示例**：`Range { rmin:int; rmax:int; }` 占用 2 列

### pack（压缩映射）

- **适用类型**：struct、interface、list、map
- **占格规则**：压缩到 1 列
- **分隔符**：字段间逗号或分号，嵌套用 `()` 包裹
- **重要**：递归结构必须使用 pack 打破循环

```cfg
struct Position (pack) { x:int; y:int; z:int; }
// 数据格式："1,2,3"

list<Position> (pack)
// 数据格式："(1,2,3);(4,5,6)"
```

### sep（分隔符映射）

- **适用类型**：struct、list
- **占格规则**：压缩到 1 列
- **限制**：struct 中所有字段必须是基本类型

```cfg
struct Time (sep=':') { hour:int; minute:int; second:int; }
// 数据格式："12:10:00"
```

### fix（固定长度映射）

- **适用类型**：list、map
- **占格规则**：固定列数 = 元素列数 × count

```cfg
list<int> (fix=2);  // 占用 2 列
list<weaponAttr> (fix=2);  // 每个 weaponAttr 占 2 列，总共 4 列
```

### block（块状映射）

- **适用类型**：list、map
- **占格规则**：横向固定，纵向可扩展
- **适用场景**：变长列表

```cfg
struct RewardItem { itemId:int; count:int; }
list<RewardItem> (block=1);  // 横向 2 列，纵向任意行
```

**Excel 示例**：
| id | rewardItems | rewardItems |
|----|-------------|-------------|
| 1  | 1001        | 5           | ← 第1个奖励 |
|    | 1002        | 10          | ← 第2个奖励 |
|    | 1003        | 3           | ← 第3个奖励 |
| 2  | 2001        | 2           | ← 新记录开始 |

**Block 嵌套**：支持多层嵌套，通过空单元格作为分隔标记。

## mustFill（必填约束）

强制字段必须包含有效值，主要用于避免 Block 映射中的意外数据合并。

```cfg
exp:taskexp (mustFill);  // 必须配置
rewardItems:list<RewardItem> (block=1);
```

## 映射选择指南

| 场景 | 推荐映射 |
|------|---------|
| 简单结构 | auto（默认） |
| 减少列数 | pack |
| 递归结构 | pack（必须） |
| 特定分隔符格式 | sep |
| 已知长度列表 | fix |
| 变长列表 | block |

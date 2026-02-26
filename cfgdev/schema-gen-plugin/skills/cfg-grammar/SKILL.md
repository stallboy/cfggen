---
name: CFG Grammar Reference
description: This skill should be used when the user asks to "check cfg syntax", "cfg grammar rules", "cfggen schema syntax", "cfg field types", "cfg metadata attributes", "cfg design patterns", "skill system design", or when working with CFG schema definition files. Provides comprehensive CFG syntax reference and real-world skill system design example.
version: 1.1.0
---

## Overview

CFG (Configuration File Grammar) 是 cfggen 的配置定义语言，用于描述数据结构、接口和数据库表。本 skill 提供完整的语法参考。

## 核心语法结构

### 三大定义类型

| 类型 | 用途 | 示例 |
|------|------|------|
| `struct` | 值对象，内聚字段集合 | 坐标、范围、奖励 |
| `interface` | 多态接口，支持多种实现 | 条件、效果、公式 |
| `table` | 数据表，具有主键 | 物品、任务、技能 |

### 定义格式

```cfg
// 结构体
struct 名称 (属性)? {
    字段*
}

// 接口
interface 名称 (属性)? {
    struct 实现1 { ... }
    struct 实现2 { ... }
}

// 数据表
table 名称[主键字段] (表属性)? {
    字段*
}
```

## 字段定义

### 基本格式

```cfg
字段名:类型 (属性)?;
```

### 类型系统

**基础类型：**
| 类型 | 说明 |
|------|------|
| `bool` | 布尔值 |
| `int` | 32位整数 |
| `long` | 64位整数 |
| `float` | 浮点数 |
| `str` | 字符串（不翻译） |
| `text` | 文本（启用国际化） |

**容器类型：**
```cfg
ids:list<int>;           // 整数列表
items:list<Item>;        // 结构体列表
```

**自定义类型：**
```cfg
pos:Position;            // 引用 struct
cond:TaskCondition;      // 引用 interface
module.SubType;          // 跨模块引用
```

### 外键定义

```cfg
// 单向外键 -> 指向主键
skillid:int ->skill.id;

// 多向外键 => 指向非主键字段
typeid:int =>item.type;

// 可空外键
nextid:int ->task.id (nullable);
```

## 属性系统

### 字段属性

| 属性 | 适用类型 | 说明 |
|------|---------|------|
| `nullable` | 所有 | 允许为空 |
| `sep='字符'` | struct | 分隔符（Excel 单元格） |
| `fix=N` | list | 固定 N 个元素 |
| `block=N` | list | 块大小 N |
| `pack` | interface | 打包简化配置 |

### 表级属性

| 属性 | 说明 |
|------|------|
| `enum='字段'` | 枚举表，字段值不可重复不可空 |
| `entry='字段'` | 入口标识，值不可重复可为空 |
| `json` | 独立 JSON 存储 |
| `columnMode` | 列模式编辑 |

## 注释规则

```cfg
// 单行注释，可在行首或行尾
struct Example { // 行尾注释
    field:int;   // 字段说明
}
```

## 快速示例

### 完整示例

```cfg
// 奖励结构
struct Reward {
    exp:int;           // 经验值
    items:list<ItemRef>; // 物品列表
}

// 物品引用
struct ItemRef {
    itemid:int ->item.id;
    count:int;
}

// 任务条件接口
interface TaskCondition {
    // 击杀条件
    struct KillMonster {
        monsterid:int ->monster.id;
        count:int;
    }
    // 收集条件
    struct CollectItem {
        itemid:int ->item.id;
        count:int;
    }
    // 条件组合
    struct ConditionAnd {
        cond1:TaskCondition (pack);
        cond2:TaskCondition (pack);
    }
}

// 任务表
table task[id] (entry='entry', lang) {
    id:int;                      // 任务ID
    entry:str;                   // 入口标识
    name:str;                    // 任务名称
    desc:text;                   // 任务描述
    condition:TaskCondition;     // 完成条件
    reward:Reward;               // 奖励
    nextTask:int ->task.id (nullable); // 后续任务
}

// 物品表
table item[id] {
    id:int;
    name:str;
    type:str;
    price:int;
}
```

## 详细参考

如需更详细的语法说明，请查阅：

- **`references/field-types.md`** - 完整类型系统和类型推断规则
- **`references/metadata.md`** - 所有元数据属性的详细说明
- **`references/tabular-mapping.md`** - 表格映射机制详解
- **`references/patterns.md`** - 常见设计模式和最佳实践
- **`references/skill-system-design.md`** - **技能系统设计参考**（重要实战案例）

## 实战案例：技能系统设计

**`references/skill-system-design.md`** 是一个完整的 CFG 实战案例，展示了如何使用 CFG 设计复杂的游戏技能系统：

- **核心概念**：Skill、Buff、Effect 三层架构
- **多态设计**：EffectLogic、BuffLogic、FloatValue、Condition 等接口
- **设计模式**：xxxCases 条件覆盖、addXxxs 累加修改
- **高级特性**：黑板 Blackboard、便条 Note、事件响应机制
- **运动逻辑**：Static、Bind、Line、Chase 等子弹运动模式

这是学习 CFG 高级用法的最佳参考！

## 常见问题

### str vs text

- `str`: 普通字符串，不参与翻译
- `text`: 启用国际化，会生成翻译键
- `enum` 和 `entry` 字段必须用 `str`

### 何时用 struct

- 多个字段语义相关（如 x,y,z 坐标）
- 需要在多处复用的字段组合

### 何时用 interface

- 同一字段需要支持多种实现
- 涉及条件判断或策略模式
- 需要可扩展的设计

### 外键选择

- `->` 引用主键，一对一关系
- `=>` 引用非主键，一对多关系

## 表格映射机制

将复杂数据结构映射到 Excel 表格的五种方式：

| 映射 | 适用类型 | 占格规则 | 主要用途 |
|------|---------|---------|---------|
| auto | 基本类型、struct、interface | 自动计算 | 默认映射 |
| pack | struct、interface、list | 压缩到 1 列 | 减少列数、递归结构 |
| sep | struct、list | 1 列自定义分隔符 | 特定格式需求 |
| fix | list | 固定列数 | 已知长度列表 |
| block | list | 横向固定，纵向扩展 | 变长列表 |

**递归结构必须使用 pack**：当数据结构形成循环引用时，必须至少在一处使用 `(pack)` 打破循环。

详细说明见 `references/tabular-mapping.md`。

---
description: 根据自然语言描述或设计文档生成 cfggen 配表 schema 定义文件
argument-hint: <配置需求描述>
allowed-tools:
  - Write
  - Read
  - Grep
  - Glob
  - AskUserQuestion
long-description: |
  此命令根据用户的自然语言描述或设计文档，生成符合 cfggen 规范的 CFG schema 定义文件。

  支持完整的 CFG 语法特性：
  - 基础类型：bool, int, float, long, str, text
  - 结构体：struct 定义复合数据类型，支持嵌套
  - 接口：interface 定义多态行为
  - 数据表：table 存储配置数据，支持主键、唯一键、外键
  - 外键关联：-> 单向外键、=> 多向外键
  - 容器类型：list<T>、map<K,V>
  - 字段属性：nullable, mustFill, fix, block, pack, sep
  - 表级属性：enum, entry, json, title, columnMode

  生成的 schema 文件将保存到当前目录，文件名自动推断或由用户指定。
---

# 生成 cfggen Schema

你是一个专业的 cfggen schema 生成助手。根据用户输入生成符合 cfggen 规范的 CFG schema 定义文件。

## 任务

1. 理解用户需求
2. 设计 schema（确定 struct、interface、table）
3. 生成 CFG 代码
4. 保存文件

## CFG 语法规则

### 基本结构

```cfg
// 结构体定义
struct 结构名 (属性)? {
    字段定义*
}

// 接口定义
interface 接口名 (属性)? {
    struct 实现类 (属性)? {
        字段定义*
    }+
}

// 数据表定义
table 表名[主键] (属性)? {
    (唯一键定义 | 字段定义 | 外键定义)+
}
```

### 字段定义

```cfg
// 普通字段
字段名:类型 (属性)?;

// 外键字段
字段名:类型 (->表名 | =>表名[字段]) (属性)?;

// 唯一键定义
[字段名 (,字段名)*];
```

### 类型规则

```cfg
// 基础类型
类型: 'bool' | 'int' | 'long' | 'float' | 'str' | 'text'

// 容器类型
类型: 'list<' 类型 '>'
     | 'map<' 类型 ',' 类型 '>'

// 自定义类型
类型: 标识符 ('.' 标识符)*
```

### 元数据规则

```cfg
// 字段属性
属性: 'nullable' | 'mustFill' | 'fix='整数 | 'block='整数
     | 'pack' | 'sep='字符串 

// 表级属性
属性: 'enum='字段 | 'entry='字段 | 'json' | 'title='字段
     | 'columnMode'
```

### 注释规则

```cfg
// 单行注释，可以放在定义前或行尾
struct Example { // 行尾注释
    field:int;   // 字段说明
}
```

### 核心语法示例

#### 基础类型和结构体

```cfg
// 结构体定义
struct Position {
    x:int; // X坐标
    y:int; // Y坐标
    z:int; // Z坐标
}

// 冒号分隔的时间（小时:分钟:秒）
struct Time (sep=':') {
    hour:int;   // 小时
    minute:int; // 分钟
    second:int; // 秒
}

// 容器类型
struct Example {
    intList:list<int>;          // 简单列表
    items:list<Item>;           // 结构体列表
    intMap:map<int,int>;        // 整数映射
    strMap:map<str,str>;        // 字符串映射
}
```

#### 接口和多态

```cfg
// 接口定义
interface TaskCondition {
    // 击杀怪物条件
    struct KillMonster {
        monsterid:int ->monster.id; // 怪物ID
        count:int;                  // 击杀数量
    }

    // 收集物品条件
    struct CollectItem {
        itemid:int ->item.id; // 物品ID
        count:int;            // 收集数量
    }

    // 条件与逻辑（递归）
    struct ConditionAnd {
        cond1:TaskCondition (pack); // 条件1
        cond2:TaskCondition (pack); // 条件2
    }
}
```

#### 数据表和外键

```cfg
// 数据表定义
table task[id] (entry='entry', lang) {
    [nextTask];              // 唯一键：下一个任务
    id:int;                  // 任务ID
    entry:str;               // 入口标识
    name:str;                // 任务名称
    description:text;        // 任务描述（支持国际化）
    nextTask:int ->task.id (nullable); // 下一个任务
}

// 外键关联
struct DropItem {
    itemid:int ->item.id; // 单向外键（指向主键）
    count:int;            // 数量
}
```

#### 字段和表级属性

```cfg
// 字段属性
| 属性         | 适用类型             | 说明                     |
|--------------|----------------------|--------------------------|
| nullable     | 所有类型             | 允许字段为空             |
| mustFill     | 所有类型             | 字段必须填写             |
| sep='分隔符'  | struct               | Excel 中使用指定分隔符   |
| fix=N        | list                 | 固定 N 个元素            |
| block=N      | list                 | 块大小为 N               |
| pack         | interface, struct    | 打包简化配置             |

// 表级属性
| 属性                      | 说明                           |
|---------------------------|--------------------------------|
| enum='字段名'             | 指定枚举字段                   |
| entry='字段名'            | 指定入口字段                   |
| json                      | 数据以独立 JSON 文件存储       |
| title='字段名'            | 指定标题字段                   |
```

## 完整示例

```cfg
// 奖励物品结构
struct RewardItem {
    itemid:int ->item.id; // 物品ID
    count:int;            // 数量
}

// 时间结构（冒号分隔）
struct Time (sep=':') {
    hour:int;   // 小时
    minute:int; // 分钟
    second:int; // 秒
}

// 任务完成条件接口
interface TaskCondition {
    // 击杀怪物条件
    struct KillMonster {
        monsterid:int ->monster.id; // 怪物ID
        count:int;                  // 击杀数量
    }

    // 收集物品条件
    struct CollectItem {
        itemid:int ->item.id; // 物品ID
        count:int;            // 收集数量
    }

    // 条件与逻辑（递归）
    struct ConditionAnd {
        cond1:TaskCondition (pack); // 条件1
        cond2:TaskCondition (pack); // 条件2
    }
}

// 任务经验公式接口（默认固定值）
interface TaskExp (defaultImpl='ConstValue', pack) {
    // 固定经验值
    struct ConstValue {
        value:int; // 固定经验
    }

    // 基于等级的经验
    struct ByLevel {
        levelcoef:float; // 等级系数
        value:int;       // 基础值
    }
}

// 任务配置表
table task[id] (entry='entry') {
    [nextTask]; // 唯一键：下一个任务
    id:int;                     // 任务ID
    entry:str;                  // 入口标识
    name:str;                   // 任务名称
    description:text;           // 任务描述
    nextTask:int ->task.id (nullable); // 下一个任务
    condition:TaskCondition;    // 完成条件
    exp:TaskExp (mustFill);     // 经验奖励（必填）
    rewards:list<RewardItem> (fix=3); // 固定3个奖励
    time:Time;                  // 时间配置
}
```

## 设计提示

- **何时创建 struct**：多个字段语义相关（如位置信息 x,y,z）
- **何时创建 interface**：同一字段可能有多种实现（如条件、效果、公式）
- **str vs text**：`text` 会启用国际化翻译；`enum` 和 `entry` 类型必须用 `str`

# CFG 元数据属性详细说明

## 字段属性

### nullable

允许字段为空。默认情况下字段不可为空。

```cfg
nextTask:int ->task.id (nullable);  // 可以为空，表示没有后续任务
parent:int ->category.id (nullable); // 可以为空，表示顶级分类
```

**适用类型：** 所有类型

### sep='分隔符'

指定结构体在 Excel 中使用的分隔符。默认为 `,`。

```cfg
// 使用冒号分隔的时间（12:30:45）
struct Time (sep=':') {
    hour:int;
    minute:int;
    second:int;
}

// 使用斜杠分隔的日期（2024/01/15）
struct Date (sep='/') {
    year:int;
    month:int;
    day:int;
}

// 使用竖线分隔的坐标（100|200|300）
struct Position (sep='|') {
    x:int;
    y:int;
    z:int;
}
```

**适用类型：** struct

### fix=N

指定列表固定包含 N 个元素。

```cfg
// 固定 3 个元素：RGB 颜色
color:list<int> (fix=3);

// 固定 2 个元素：范围 [min, max]
range:list<int> (fix=2);

// 固定 4 个元素：四边形顶点
vertices:list<Position> (fix=4);
```

**适用类型：** list

### block=N

指定列表的块大小，用于分块读取。

```cfg
// 每 2 个元素为一组
items:list<ItemRef> (block=2);

// 每 3 个元素为一组
rewards:list<int> (block=3);  // [itemid, count, chance, itemid, count, chance, ...]
```

**适用类型：** list

### pack

打包显示，将接口实现直接内嵌，无需跳转子节点。简化编辑界面。

```cfg
interface TaskCondition {
    struct KillMonster {
        monsterid:int ->monster.id;
        count:int;
    }
    struct CollectItem {
        itemid:int ->item.id;
        count:int;
    }
}

// 使用 pack 简化显示
condition:TaskCondition (pack);

// 组合条件也使用 pack
struct ConditionAnd {
    cond1:TaskCondition (pack);
    cond2:TaskCondition (pack);
}
```

**适用类型：** interface, struct

### mustFill

强制字段必须包含有效值：
- **列表/映射类型**：元素个数必须大于 0
- **其他类型**：单元格不能为空

```cfg
exp:taskexp (mustFill);  // 经验奖励（必须配置）
```

主要用于避免 Block 映射中的数据合并问题。

## 表级属性

### enum='字段名'

指定该表为枚举字典，字段值不可重复、不可为空。字段必须是 str 类型。

```cfg
// 伤害类型枚举
table damageType[id] (enum='name') {
    id:int;
    name:str;    // 枚举名称，不可重复
    desc:text;   // 描述
}

// 元素类型枚举
table elementType[id] (enum='code') {
    id:int;
    code:str;    // 元素代码，如 "fire", "ice"
    name:text;   // 显示名称
}
```

### entry='字段名'

指定入口标识字段。字段必须是 str 类型，值不可重复但可以为空。

```cfg
// 任务表，使用 entry 作为代码中的引用标识
table task[id] (entry='entry', lang) {
    id:int;
    entry:str;    // 入口标识，如 "main_quest_1"
    name:str;     // 任务名称
    desc:text;    // 描述
}
```

**与 enum 的区别：**
- `enum`: 值不可为空
- `entry`: 值可以为空

### json

数据以独立 JSON 文件存储，适合低频修改或超大表。

```cfg
// 大型配置表使用 JSON 存储
table localization[id] (json) {
    id:int;
    key:str;
    values:map<str,str>;  // 多语言映射
}
```

### columnMode

使用列模式编辑，将宽表转换为长表格式。

```cfg
// 列模式编辑的技能表
table skill[id] (columnMode) {
    id:int;
    name:str;
    level:int;
    damage:int;
    cooldown:int;
}
```

### title='字段名'

指定用于编辑器显示标题的字段。

```cfg
table effectclass[name] (enum='name', title='text') {
    name:str;
    text:str;
}
```

### description='字段1,字段2'

指定编辑器简略视图中展示的描述字段，支持多字段组合。

### refTitle

在外键字段上设置，指定链接线显示的字段名称。

```cfg
skillid:int ->skill.id (refTitle='name');
```

### lang

国际化支持标记。生成 Excel 时会在主键列后添加额外列，辅助翻译。

### root

根表标记。标记为 `(root)` 的表会被未引用检测跳过，适用于：
- 等级表（以等级当 key）
- 地块表（以坐标当 key）
- 随机表（用于随机选择）

```cfg
table LevelReward[level] (root) {
    level:int;
    reward:int ->Item;
}
```

### extraSplit=N

Lua 文件分割配置。当配置为 N 时，数据每 N 行分割为一个文件，用于：
- 解决 Lua 单文件 65526 常量限制
- 优化热更新下载大小

## 组合属性

多个属性可以组合使用：

```cfg
// 可空的外键
nextTask:int ->task.id (nullable);

// 固定长度且可空
position:list<int> (fix=3, nullable);

// 打包且可空
condition:TaskCondition (pack, nullable);
```

## 属性优先级

当属性冲突时，按以下优先级处理：
1. 字段级属性覆盖表级属性
2. 显式指定的属性覆盖默认值
3. 后声明的属性覆盖先声明的属性（组合属性时）

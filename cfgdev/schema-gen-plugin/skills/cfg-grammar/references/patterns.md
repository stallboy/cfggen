# CFG 设计模式和最佳实践

## 常见设计模式

### 1. 枚举表模式（Enum Pattern）

用于为 interface 提供下拉选项，避免魔数。

#### 定义枚举表

```cfg
// 伤害类型枚举
table damageType[id] (enum='name') {
    id:int;
    name:str;    // 必须是唯一的英文名称
    desc:text;   // 描述（可选）
}
```

#### 创建对应 CSV 文件

CSV 文件有两行 header：
- 第一行：中文注释
- 第二行：字段名

枚举表的 `name` 字段值应对应 interface 的实现类名：

```csv
##,类型名称,描述
id,name,desc
1,Physical,物理伤害
2,Magical,魔法伤害
3,True,真实伤害
```

#### 与 interface 配合

```cfg
interface DamageEffect {
    struct Physical { base:int; ratio:float; }
    struct Magical { base:int; ratio:float; }
    struct True { base:int; }
}

table skill[id] {
    id:int;
    damageType:int ->damageType.id;  // 引用枚举
    effect:DamageEffect;             // 对应实现
}
```

#### enum vs entry

| 特性 | enum | entry |
|------|------|-------|
| 值唯一性 | 必须 | 必须 |
| 值可为空 | 不可 | 可 |
| 用途 | 枚举字典 | 入口标识 |
| 典型场景 | 类型定义 | 系统入口 |

### 2. 条件模式（Condition Pattern）

用于需要灵活组合条件的场景。

```cfg
interface Condition {
    // 基础条件
    struct LevelCheck {
        minLevel:int;
    }
    struct ItemCheck {
        itemid:int ->item.id;
        count:int;
    }

    // 逻辑组合
    struct And {
        left:Condition (pack);
        right:Condition (pack);
    }
    struct Or {
        left:Condition (pack);
        right:Condition (pack);
    }
    struct Not {
        cond:Condition (pack);
    }
}
```

### 3. 效果模式（Effect Pattern）

用于技能效果、物品效果等多态场景。

```cfg
interface Effect {
    struct Damage {
        value:int;
        type:int ->damageType.id;
    }
    struct Heal {
        value:int;
    }
    struct Buff {
        buffid:int ->buff.id;
        duration:float;
    }
}

// 可组合的效果列表
effects:list<Effect>;
```

### 4. 奖励模式（Reward Pattern）

通用的奖励结构设计。

```cfg
struct Reward {
    exp:int;
    gold:int;
    items:list<ItemRef>;
}

struct ItemRef {
    itemid:int ->item.id;
    count:int;
}
```

### 5. 触发器模式（Trigger Pattern）

事件驱动的配置设计。

```cfg
struct Trigger {
    event:str;           // 事件类型
    condition:Condition; // 触发条件
    effects:list<Effect>; // 触发效果
}

// 被动技能示例
table passiveSkill[id] {
    id:int;
    name:str;
    triggers:list<Trigger>;
}
```

### 6. 阶段模式（Phase Pattern）

分阶段执行的配置设计。

```cfg
interface PhaseAction {
    struct Spawn {
        monsterid:int ->monster.id;
        count:int;
    }
    struct Dialog {
        dialogid:int ->dialog.id;
    }
    struct Wait {
        seconds:float;
    }
}

struct Phase {
    name:str;
    actions:list<PhaseAction>;
    nextPhase:int ->phase.id (nullable);
}
```

## 最佳实践

### 主键设计

- 使用简单的 int 类型作为主键
- 主键名称通常为 `id`
- 避免使用复合主键

```cfg
// 推荐
table item[id] { ... }

// 避免
table item[id, type] { ... }
```

### 外键设计

- 明确外键关系：`->` 用于主键引用，`=>` 用于非主键引用
- 合理使用 `nullable`

```cfg
// 指向主键
skillid:int ->skill.id;

// 指向非主键
typeid:int =>item.type;

// 可空外键
parentid:int ->category.id (nullable);
```

### 结构体复用

提取公共字段为结构体：

```cfg
// 公共范围结构
struct IntRange {
    min:int;
    max:int;
}

// 公用位置结构
struct Position {
    x:int;
    y:int;
    z:int;
}

// 复用
levelRange:IntRange;
spawnPos:Position;
```

### 接口扩展性

设计接口时考虑未来扩展：

```cfg
interface Reward {
    struct Gold { amount:int; }
    struct Item { itemid:int ->item.id; count:int; }
    struct Exp { amount:int; }
    // 未来可以轻松添加新的奖励类型
    // struct Skill { skillid:int ->skill.id; }
}
```

### 模块化组织

按功能模块组织文件：

```
config/
├── config.cfg      # 顶层配置
├── item/
│   └── item.cfg    # 物品模块
├── skill/
│   └── skill.cfg   # 技能模块
├── task/
│   └── task.cfg    # 任务模块
└── monster/
    └── monster.cfg # 怪物模块
```

### 命名约定

- 表名：小写，如 `item`, `task`, `passive_skill`
- 字段名：驼峰式，如 `itemId`, `nextTask`
- 结构体：大驼峰，如 `ItemRef`, `IntRange`
- 接口：大驼峰，如 `Condition`, `Effect`

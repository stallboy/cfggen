# CFG 字段类型详细说明

## 基础类型

### bool
布尔类型，值为 true 或 false。

```cfg
enabled:bool;  // 是否启用
hidden:bool;   // 是否隐藏
```

### int
32位有符号整数，范围 -2,147,483,648 到 2,147,483,647。

```cfg
id:int;        // ID
count:int;     // 数量
level:int;     // 等级
```

### long
64位有符号整数，用于需要更大范围的场景。

```cfg
timestamp:long;  // 时间戳
gold:long;       // 金币（大额）
```

### float
浮点数，用于需要小数的场景。

```cfg
rate:float;    // 比率
chance:float;  // 概率
x:float;       // 坐标
```

### str
普通字符串，不参与国际化翻译。

```cfg
name:str;      // 名称
code:str;      // 代码
type:str;      // 类型标识
```

**适用场景：**
- 程序内部使用的标识符
- 不需要翻译的文本
- `enum` 和 `entry` 字段

### text
文本类型，启用国际化翻译支持。

```cfg
description:text;  // 描述（需翻译）
dialog:text;       // 对话内容
tip:text;          // 提示文本
```

**适用场景：**
- 需要多语言支持的文本
- 面向用户的显示内容

## 容器类型

### list<T>
列表类型，表示元素序列。

```cfg
ids:list<int>;          // 整数列表
names:list<str>;        // 字符串列表
rewards:list<Reward>;   // 结构体列表
```

**列表属性：**

| 属性 | 说明 | 示例 |
|------|------|------|
| `fix=N` | 固定 N 个元素 | `pos:list<int> (fix=3)` |
| `block=N` | 块大小 N，用于分块读取 | `items:list<Item> (block=2)` |

```cfg
// 固定 3 个元素的位置
position:list<int> (fix=3);  // x, y, z

// 块大小为 2 的物品列表
items:list<Item> (block=2);
```

### map<K,V>

键值对映射类型。

```cfg
// 整数到整数的映射
attrs:map<int,int>;

// 字符串到结构体的映射
rewards:map<str,Reward>;
```

**外键支持：**
- 只支持 value 的外键引用
- key 不支持外键

```cfg
// 每个 value 都作为外键
drops:map<int,int> =>item.id;
```

## 自定义类型

### struct 引用

```cfg
// 定义结构体
struct Position {
    x:int;
    y:int;
    z:int;
}

// 引用结构体
spawnPos:Position;
```

### interface 引用

```cfg
// 定义接口
interface Effect {
    struct Damage { ... }
    struct Heal { ... }
}

// 引用接口
effect:Effect;
```

### 跨模块引用

使用点号前缀引用其他模块的类型：

```cfg
// 在 skill.cfg 中引用 item 模块的类型
reward:item.Reward;

// 在 task.cfg 中引用 skill 模块的类型
skillCond:skill.Condition;
```

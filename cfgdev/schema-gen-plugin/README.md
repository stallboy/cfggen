# cfggen-architect

游戏架构师与数据驱动配置生成 Claude Code 插件。根据策划文档或自然语言描述，生成符合 cfggen 规范的模块化 schema 定义文件。

## 安装

将整个 `schema-gen-plugin` 目录复制到 Claude Code 的插件目录：

```bash
# 项目级安装
cp -r schema-gen-plugin .claude/plugins/

# 或全局安装
cp -r schema-gen-plugin ~/.claude/plugins/
```

## 使用方法

直接向 Claude 描述您的配置需求即可：

```
创建一个物品表，包含 id、名称、类型、价格、描述
```

```
设计一个任务系统，包含多种完成条件（击杀怪物、收集物品）和奖励
```

```
设计一个技能系统，支持伤害、治疗、Buff，以及事件触发器（如反伤）
```

## 功能特性

| 特性 | 说明 |
|------|------|
| 🎯 **自然语言输入** | 用中文描述需求，自动生成 CFG schema |
| 🏗️ **战略设计** | 识别核心玩法，划分模块，定义依赖关系 |
| ⚙️ **战术设计** | 实体建模、多态接口、值对象、枚举定义 |
| ✅ **Schema Enum 支持** | 优先使用 `enum Name { Value; }` 语法定义编译时常量 |
| 📚 **GAS 架构参考** | 战斗系统设计自动参考 skill-system-design.md |
| 🧪 **可测试性设计** | 引导产出解耦、可 Mock 的配置结构 |

## 技能结构

```
schema-gen-plugin/
├── .claude-plugin/
│   └── plugin.json
├── skills/
│   └── cfggen-architect/
│       ├── SKILL.md                      # 核心技能文件
│       ├── references/
│       │   └── skill-system-design.md    # 战斗系统设计参考
│       └── evals/
│           └── evals.json                # 测试用例
└── README.md
```

## 设计流程

### 阶段一：战略设计
1. 识别核心玩法 (Core)
2. 划分子域与周边系统
3. 定义模块依赖关系

### 阶段二：战术设计
1. 识别实体 → `table`
2. 识别多态点 → `interface`
3. 识别值对象 → `struct`
4. 识别枚举 → `enum` / Table Enum

### 阶段三：可测试性审查
### 阶段四：生成文件

## CFG 语法示例

### Schema Enum（优先使用）

```cfg
enum ModifierOp {
    Add;
    Multiply;
    Override;
}

// 使用
op:ModifierOp;
```

### Table Enum（策划扩展）

```cfg
table effecttype[id] (enum='name') {
    id:int;
    name:str;
    desc:text;
}
```

### 接口（多态）

```cfg
interface Condition {
    struct KillMonster { monsterid:int; count:int; }
    struct CollectItem { itemid:int; count:int; }
    struct And { left:Condition (pack); right:Condition (pack); }
}
```

## 战斗系统设计

当需求涉及战斗/技能系统时，技能会自动参考 `skill-system-design.md`，该文档定义了：

- **GameplayTag** - 层级化标签系统
- **Ability/Effect/Status** - 三层架构
- **FloatValue** - 动态求值接口
- **Trigger** - 事件触发器模式
- **事件管线** - Pre/Post 两阶段提交

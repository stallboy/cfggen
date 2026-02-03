---
layout: page
title: AI生成
nav_order: 4
---

# 🤖 AI 生成配置

AI 生成功能允许您使用自然语言描述自动生成符合结构定义的配置数据。通过集成 AI 能力,可以大幅提升配置效率,减少手动编写的工作量。

---

## 📋 功能概述

### 核心能力

- **🎯 自然语言生成**: 用中文描述需求,AI 自动生成 JSON 配置
- **📝 结构化验证**: 生成的数据自动符合 CFG 结构定义
- **🔄 迭代优化**: 支持多次对话修改和完善配置
- **📚 示例学习**: 从现有配置中学习,保持一致性
- **🔗 外键智能**: 自动处理外键关联,生成有效的引用关系

### 应用场景

- **快速原型**: 快速生成测试数据
- **批量配置**: 批量生成相似的配置记录
- **填充数据**: 为新系统快速填充初始数据
- **数据迁移**: 从描述性文档转换为结构化配置

---

## 📁 配置目录结构

AI 生成功能需要在配置目录中添加特定的提示词文件:

```
config/
│   config.cfg              # 结构定义文件
│   config.jte              # 全局提示词模板(可选)
│   init.md                 # 初始欢迎语(可选)
│
├───ai_行为/                # AI行为模块
│   │   $mod.md             # 模块规则和描述
│   │   ai.md               # ai表的规则(可选)
│   │   ai_action.md        # ai_action表的规则(可选)
│   │
│   ├── ai.cfg              # 结构定义
│   ├── ai行为.xlsx         # Excel数据
│   └── _ai_ai/             # AI生成的JSON数据
│       ├── 1.json
│       └── 2.json
│
├───equip/                  # 装备模块
│   │   $mod.md             # 模块规则
│   │   equip.md            # equip表规则
│   │
│   ├── equip.cfg
│   ├── ability.csv
│   └── rank.csv
│
└───task/                   # 任务模块
    │   $mod.md             # 模块规则
    │   task.md             # task表规则
    │   task2.md            # task2表规则
    │
    ├── task.cfg
    ├── task_任务.csv
    ├── completeconditiontype.csv
    └── _task_task2/        # AI生成的数据
        ├── 1.json
        ├── 2.json
        └── 3.json
```

### 文件说明

| 文件 | 用途 | 必需 |
|------|------|------|
| `config.jte` | 全局提示词模板,控制所有表的生成行为 | 否 |
| `init.md` | AI 对话的初始欢迎语 | 否 |
| `$mod.md` | 模块级规则和描述,影响该模块下所有表 | 否 |
| `[table].md` | 表级规则和示例,只影响特定表 | 否 |

**注意**: 以上文件都是可选的,系统提供默认行为。

---

## 🎨 配置文件详解

### 1. 全局提示词模板 `config.jte`

控制 AI 生成行为的核心模板,适用于所有表。

#### 数据模型

```java
public record PromptModel(
    String table,              // 表名
    String structInfo,         // 结构定义(TypeScript格式)
    String rule,               // 规则(从 $mod.md + [table].md 读取)
    List<Example> examples     // 示例记录
) {
    public record Example(
        String id,             // 记录ID
        String description,    // 描述
        String json            // JSON数据
    )
}
```

#### 默认模板

```markdown
@import configgen.genbyai.PromptModel
@import configgen.genbyai.PromptModel.Example

@param PromptModel model

# Role: 专业游戏设计师

## Profile
- Description: 经验丰富、逻辑严密,大师级,擅长把需求描述转变为符合结构的json数据
- OutputFormat: json

## Rules
### ${model.table()}结构定义

\`\`\`typescript
${model.structInfo()}
\`\`\`

@if (!model.rule().isEmpty())
    ${model.rule()}
@endif

## Constraints
生成的json数据必须严格遵守[${model.table()}结构定义],确保数据的一致性和有效性。遵守以下规则:
- 对象要加入$type字段,来表明此对象的类型
- 如果对象里字段为默认值,则可以忽略此字段
- 字段类型为number,默认为0
- 字段类型为array,默认为[]
- 字段类型为str,默认为空字符串
- 对象可以加入$note字段,作为注释,不用全部都加,最好这些注释合起来组成了描述
- json中不要包含//开头的注释

## Workflow

针对用户描述输出json格式的配置(若描述中不含ID,则自动选择)

@if(!model.examples().isEmpty())
    ## Examples
    ---
    @for(Example ex : model.examples())
        输入:${ex.id()}, ${ex.description()}

        输出:
        \`\`\`json
        ${ex.json()}
        \`\`\`
        ---
    @endfor
@endif

## Initialization
作为角色 [Role], 严格遵守 [Rules], 告诉用户 [Workflow]
```

### 2. 模块规则 `$mod.md`

为整个模块提供通用规则和描述。

#### 示例: `config/ai_行为/$mod.md`

```markdown
---
description: AI行为配置系统,包含AI的行为树、决策逻辑和动作定义
---

## 模块概述

本模块定义游戏中AI单元的行为模式,包括:
- 行为树节点配置
- AI决策逻辑
- 行动条件和效果

## 设计原则

1. **简洁性**: AI行为应该简单直接,避免过于复杂的状态
2. **可预测性**: 相同条件下应该产生可预测的行为
3. **性能优化**: 避免高频计算的复杂判断

## 数值范围

- 优先级范围: 0-100,数值越大优先级越高
- 概率范围: 0-10000,单位为0.01%
- 冷却时间: 单位为毫秒

## 注意事项

- 所有条件判断都应该考虑性能影响
- 避免无限循环的行为引用
```

### 3. 表规则 `[table].md`

为特定表提供详细规则和示例。

#### 示例: `config/task/task.md`

```markdown
---
description: 任务配置,定义游戏中各种任务的目标、奖励和完成条件
exampleId: 1
exampleDescription: 新手引导任务,要求玩家击杀10只史莱姆
---

## 任务设计原则

1. **渐进性**: 任务难度应该循序渐进
2. **清晰性**: 任务描述应该清晰易懂
3. **奖励合理**: 奖励应该与任务难度匹配

## 任务类型说明

- **主线任务**: 推进游戏剧情,奖励丰厚
- **支线任务**: 丰富游戏内容,奖励适中
- **日常任务**: 重复可完成,奖励较少
- **成就任务**: 长期目标,奖励特殊

## 数值建议

- 新手任务经验: 100-500
- 中级任务经验: 500-2000
- 高级任务经验: 2000-10000
- 任务奖励物品数量: 1-5个

## 完成条件示例

- 击杀怪物: KillMonster,指定怪物ID和数量
- 收集物品: CollectItem,指定物品ID和数量
- 与NPC对话: TalkNpc,指定NPC ID
- 等级要求: LevelReach,指定目标等级
```

---

## 🚀 AI 生成工作流程

### 方式一: 使用配置编辑器 (推荐)

1. **启动编辑器**
   ```bash
   java -jar cfggen.jar -datadir config -gen server
   ```

2. **打开 AI 聊天界面**
   - 在配置编辑器中导航到 AI Chat 页面
   - 选择要生成配置的表

3. **输入需求描述**
   ```
   请帮我生成一个新手任务:
   - 任务名称: "初次战斗"
   - 要求击杀10只史莱姆
   - 奖励100金币和一把新手剑
   - 完成后接取下一个任务
   ```

4. **AI 生成配置**
   - 系统自动生成符合结构的 JSON 数据
   - 验证数据有效性
   - 保存到 `_table_tableName/` 目录

5. **迭代优化**
   - 查看生成的配置
   - 如果不满意,继续与 AI 对话修改
   - 直到满意为止

### 方式二: 使用 MCP 服务器

在支持 MCP 的编辑器(如 Claude Code)中使用:

```bash
# 启动 MCP 服务器
java -jar cfggen.jar -datadir config -gen server
```

然后在编辑器中直接与 AI 对话生成配置。

详见: [MCP 服务器文档](./03.mcpserver.md)

---

## 📊 生成效果示例

### 示例 1: 生成任务配置

**用户输入**:
```
创建一个主线任务"勇者的试炼":
- 玩家需要达到5级
- 击杀20只哥布林
- 奖励500经验、1000金币
- 包含3个随机奖励物品
- 完成后开启新地图
```

**AI 生成**:
```json
{
  "taskid": 1001,
  "name": "勇者的试炼",
  "type": "main",
  "description": "完成勇者的试炼,证明你的实力!",
  "completecondition": {
    "$type": "ConditionAnd",
    "cond1": {
      "$type": "LevelReach",
      "level": 5
    },
    "cond2": {
      "$type": "KillMonster",
      "monsterid": 2001,
      "count": 20,
      "$note": "击杀哥布林"
    }
  },
  "exp": {
    "$type": "ByLevel",
    "levelcoef": 100.0,
    "value": 500,
    "$note": "基于等级的500经验"
  },
  "rewards": [
    {
      "chance": 10000,
      "itemids": [1001],
      "range": {
        "rmin": 1,
        "rmax": 1
      }
    },
    {
      "chance": 5000,
      "itemids": [2001, 2002],
      "range": {
        "rmin": 1,
        "rmax": 2
      }
    }
  ],
  "nexttask": 1002
}
```

### 示例 2: 生成装备配置

**用户输入**:
```
生成一件稀有装备"龙鳞铠甲":
- 防御力100-150
- 增加500点生命值
- 需要20级才能装备
- 火属性抗性+30%
```

**AI 生成**:
```json
{
  "id": 5001,
  "name": "龙鳞铠甲",
  "type": "armor",
  "rank": "rare",
  "levelReq": 20,
  "attributes": [
    {
      "attrId": 1,
      "value": 125,
      "$note": "防御力(100-150的中间值)"
    },
    {
      "attrId": 2,
      "value": 500,
      "$note": "生命值"
    },
    {
      "attrId": 10,
      "value": 30,
      "$note": "火属性抗性(%)"
    }
  ],
  "description": "用真龙鳞片打造的铠甲,提供卓越的防护。"
}
```

### 示例 3: 生成 AI 行为配置

**用户输入**:
```
创建一个哥布林战士的AI行为:
- 优先攻击距离最近的敌人
- 血量低于30%时逃跑
- 冷却时间2秒
```

**AI 生成**:
```json
{
  "id": 3001,
  "name": "哥布林战士_攻击",
  "priority": 80,
  "cooldown": 2000,
  "condition": {
    "$type": "And",
    "cond1": {
      "$type": "HasTarget",
      "$note": "有目标"
    },
    "cond2": {
      "$type": "HpAbove",
      "percent": 30,
      "$note": "血量高于30%"
    }
  },
  "action": {
    "$type": "AttackNearest",
    "$note": "攻击最近敌人"
  }
}
```

---

## 🎯 最佳实践

### 1. 编写清晰的规则

**好的规则**:
```markdown
## 任务ID规则
- 主线任务: 1000-1999
- 支线任务: 2000-2999
- 日常任务: 3000-3999

## 奖励数量建议
- 普通任务: 1-2个物品
- 精英任务: 3-5个物品
- Boss任务: 5-10个物品
```

**不好的规则**:
```markdown
任务奖励要合理
```

### 2. 提供具体示例

在 `[table].md` 中提供 exampleId 和 exampleDescription,帮助 AI 理解预期格式:

```markdown
---
exampleId: 1001
exampleDescription: 新手引导任务,简单易懂
---
```

### 3. 模块化设计

将相关的表放在同一模块目录下,使用 `$mod.md` 定义通用规则:

```
config/
├── task/              # 任务模块
│   ├── $mod.md        # 任务通用规则
│   ├── task.md        # task表规则
│   └── taskreward.md  # taskreward表规则
```

### 4. 迭代优化

- 第一次生成可能不够完美
- 通过对话不断调整和优化
- 将满意的配置作为示例提供给 AI

### 5. 数据验证

AI 生成后,使用 cfggen 的验证功能检查:

```bash
java -jar cfggen.jar -datadir config -gen validate
```

---

## 🔧 高级功能

### 指定引用表

在 `[table].md` 的 frontmatter 中指定引用表:

```markdown
---
refTables: item.itemtype,equip.rank
---
```

这样 AI 在生成时会参考这些表的数据,确保外键引用有效。

### 动态 ID 分配

如果不指定 ID,系统会自动分配可用的 ID:

**用户输入**:
```
创建一个新任务...
```

**系统行为**:
- 查找该表当前最大的 ID
- 自动分配 ID = maxId + 1
- 返回生成的配置和分配的 ID

### 批量生成

通过循环对话,可以快速生成多条配置:

```
用户: 生成5个日常任务,难度递增
AI: [生成5个任务配置]
用户: 第3个任务太难了,降低难度
AI: [修改第3个任务]
用户: 保存所有任务
AI: [保存到对应目录]
```

---

## 📚 相关文档

- [MCP 服务器集成](./03.mcpserver.md) - 在编辑器中使用 AI 生成
- [AI 翻译](./04.translate.md) - 使用 AI 翻译多语言文本
- [配置编辑器](../cfgeditor/) - 可视化配置编辑工具
- [结构定义](../cfggen/03.schema.md) - CFG 结构定义语法

---

## ❓ 常见问题

### Q: AI 生成的配置可以直接使用吗?

**A**: 可以,但建议:
1. 先使用 `-gen validate` 验证数据有效性
2. 检查外键引用是否正确
3. 确认数值范围合理

### Q: 如何改进 AI 生成质量?

**A**:
1. 编写详细的 `$mod.md` 和 `[table].md`
2. 提供更多示例
3. 通过对话不断调整和优化
4. 将满意的配置保存为示例

### Q: 支持哪些 AI 模型?

**A**: 通过 MCP 服务器,可以使用任何支持的 AI 模型,如 Claude、GPT-4 等。

### Q: 生成的历史记录在哪里?

**A**: 生成的配置保存在 `_table_tableName/` 目录下,每个配置一个 JSON 文件。

---

## 🎉 开始使用

现在您已经了解了 AI 生成的强大功能,可以开始使用了!

**建议步骤**:
1. 准备配置目录结构
2. 编写 `$mod.md` 和 `[table].md` 规则
3. 启动配置编辑器或 MCP 服务器
4. 开始与 AI 对话生成配置
5. 验证并保存生成的配置

祝您使用愉快! 🚀

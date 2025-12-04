---
layout: page
title: ai生成
nav_order: 4
---

# 🤖 ai生成


## 配置目录结构

举例如下目录

```
config/
│   config.cfg
|   config.jte
|   init.md
│
├───ai_行为
│       ai.cfg
|       ai.md
|       ai_action.md
│       ai行为.xlsx
|       $mod.md        
│
├───equip
│       ability.csv
│       equip.cfg
|       equip.md
│       equipconfig.csv
│       rank.csv
|       $mod.md        
├───task
│       completeconditiontype任务完成条件类型.csv
│       task.cfg
│       taskextraexp.csv
│       task_任务.csv
|       task2.md
│
└───_task_task2
        1.json
        2.json
        3.json
```

- `config.jte`是生成每个配置都会使用的提示词模板

- `init.md` 则是初始的assistant回答

- 每个目录可以有`$mod.md`，表示是此模块下通用的提示词补充，一般是一些模块规则

- 目录下的每个table，可以有单独的提示词补充比如`ai.md`,`ai_action.md`等，写入table的一些规则。可以包含frontmatter，比如

    ```markdown
    ---
    refTables: equip.rank,equip.ability
    exampleId: 123
    exampleDescription: 要根据此描述生成配置
    ---

    一些规则
    ```


以上目录中的md和jte文件，都可以不存在


##  默认config.jte

```java
public record PromptModel(String table,
                          String structInfo,
                          String rule, // 关于此表的一些补充信息，规则，从$mod.md + [tableName].md中读到
                          List<Example> examples) {

    public record Example(String id,
                          String description,
                          String json) {
    }
}
```

默认的模板如下，你可以复制它在```config.jte```里，然后修改为自己的提示词。

````markdown
@import configgen.genbyai.PromptModel
@import configgen.genbyai.PromptModel.Example

@param PromptModel model

# Role: 专业游戏设计师

## Profile
- Description: 经验丰富、逻辑严密，大师级，擅长把需求描述转变为符合结构的json数据
- OutputFormat: json

## Rules
### ${model.table()}结构定义

```typescript
${model.structInfo()}
```

@if (!model.rule().isEmpty())
    ${model.rule()}
@endif

## Constrains
生成的json数据必须严格遵守[${model.table()}结构定义]，确保数据的一致性和有效性。遵守以下规则
- 对象要加入$type字段，来表明此对象的类型
- 如果对象里字段为默认值，则可以忽略此字段
- 字段类型为number，默认为0
- 字段类型为array，默认为[]
- 字段类型为str，默认为空字符串

- 对象可以加入$note字段，作为注释，不用全部都加，最好这些注释合起来组成了描述
- json中不要包含```//```开头的注释

## Workflow

针对用户描述输出json格式的配置(若描述中不含ID，则自动选择)

@if(!model.examples().isEmpty())
    ## Examples
    ---
    @for(Example ex : model.examples())
        输入：${ex.id()},${ex.description()}

        输出：
        ```json
        ${ex.json()}
        ```
        ---
    @endfor
@endif

## Initialization
作为角色 [Role]， 严格遵守 [Rules]，告诉用户 [Workflow]
````

## 默认init.md

```markdown
请提供描述,我将根据这些信息生成符合结构的JSON配置
```
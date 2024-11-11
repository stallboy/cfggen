---
layout: page
title: ai生成
nav_order: 10
---

# ai生成
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

- TOC
{:toc}
---

## 批量生成json

```
-gen jsonbyai
    own=null             提取部分配置，跟cfg中有tag标记的提取
    cfg=ai.json          llm大模型选择，需要兼容openai的api
    ask=ask.txt          问题，每行生成一个json
    table=skill.buff     表名称
    promptfn=null        一般不用配置，默认为在<cfg>文件目录下的<table>.jte，格式参考https://jte.gg/
    raw                  false表示是把结构信息转为typescript类型信息提供给llm,默认为false
    retry=1              重试llm次数，默认1代表不重试
```

## 交互式生成

作为cfgeditor.exe的服务端，需要额外配置aicfg，一般配置为ai.json
```
-gen server
    own=null             提取部分配置，跟cfg中有tag标记的提取
    port=3456            为cfgeditor.exe提供服务的端口
    note=_note.csv       server.note
    aicfg=null           llm大模型选择，需要兼容openai的api
    postrun=null         可以是个xx.bat，用于自动提交服务器及时生效
    postrunjavadata=configdata.zip 如果设置了postrun，增加或更新json后，会先生成javadata文件，然后运行postrun
```

## ai.json格式

```java
public record AICfg(String baseUrl,
                    String apiKey,
                    String model,
                    List<TableCfg> tableCfgs) {

    public record TableCfg(String table,
                           String promptFile, // {table}.jte
                           String init, // 初始对白
                           List<String> extraRefTables,
                           List<OneExample> examples) {
    }

    public record OneExample(String id,
                             String description) {
    }
}

```

## PromptModel

```java
public record PromptModel(String table,
                          String structInfo,
                          String extra,
                          List<Example> examples) {

    public record Example(String id,
                          String description,
                          String json) {
    }
}
```

默认的模板如下，你可以复制它在```<table>.jte```里，然后修改为自己的提示词。

````markdown
@import configgen.tool.PromptModel
@import configgen.tool.PromptModel.Example

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
1. 用户指定id和描述
2. 针对用户给定的id和描述输出json格式的配置

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


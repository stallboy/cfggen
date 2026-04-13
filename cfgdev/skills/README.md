# cfggen-architect

游戏架构师与数据驱动配置生成 Claude Code 插件。根据策划文档或自然语言描述，生成符合 cfggen 规范的模块化 schema 定义文件。

## 安装

将整个 `schema-gen-plugin\cfggen-architect` 目录复制到 项目的`.claude\skills` 目录下：


## 使用方法

直接向 Claude 描述您的配置需求即可：

```
创建一个物品表，包含 id、名称、类型、价格、描述
```

```
设计一个任务系统，包含多种完成条件（击杀怪物、收集物品）和奖励
```

```
根据目录xx下的策划文档，生成配表
```

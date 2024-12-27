---
layout: page
title: 编辑器
nav_order: 3
---

# 编辑器
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

- TOC
{:toc}
---

## why？
一般游戏开发组会为任务、行为树、技能等复杂的结构，做专门的编辑器工具。
这个工具本意是要做基于节点的通用编辑工具，只要定义好结构，以上的专用编辑器都不用写了。

### 浏览数据

以下节点都可以右键点击跳转。
这样方便查看各个表，各个记录的链接关系，通过这个链接的节点方便的导航查看整个数据，
再加上一个全局数据搜索功能，这比打开excel来浏览应该是要方便很多。

### 编辑数据

编辑数据会保存成json文件

1. .cfg中设置表用json格式储存
```
table effect[id] (json) {
	id:int;
	text:str;
	logic:EffectLogic (pack);
}
```
如上用json来声明。

2, 在编辑器中选择id时，附带说明
```
table skillclass[id] (enum='name', title='text') {
	id:int;
	name:str;
	text:text;
}
struct FromSkillClass {
    skillClass:int ->skillclass;
}
```
如何当FromSkillClass 结构里int字段外键到skillclass表，则在编辑器选择下拉菜单是包含id（这里正好是个枚举，所以还包含枚举字符串），
这里再加上title='text'，则下拉菜单选项上会包含text字段的中文描述。


## 查看表结构
## 查看表关系
## 查看记录
## 查看记录关系
## 搜索记录

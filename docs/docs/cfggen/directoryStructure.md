---
layout: page
title: 文件目录结构
parent: 配表系统
nav_order: 2
---

# 文件目录结构
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

- TOC
{:toc}
---

## 目录结构

- 为了模块化，尽量不要在顶层目录放csv或excel文件，而是都要放到子文件夹下，比如equip目录下
- excel文件可以包含多个sheet，生成时excel文件名被忽略，而直接用sheet名称，csv文件则直接用csv的文件名。
- 每个子文件夹会有对应的.cfg文件，比如equip目录下生成equip.cfg，做为excel，csv数据文件的格式描述，
  在这里程序会配置上struct，interface，table列的类型，主键，唯一键，外键，枚举，取值约束等

目录结构，包含excel表做为数据，和后缀为.cfg文件做为数据的描述

* excel表主要由策划来填写，如下levelUp.xlsx

| id  | 升级经验       | 等级奖励       |
|-----|------------|------------|
| id  | upgradeExp | levelAward |
| 1   | 60         | 1000082    |
| 2   | 70         | 0          |
| 3   | 80         | 0          |
| 4   | 90         | 0          |


* .cfg文件主要由程序员来维护 ， 以上levelUp对应的.cfg文件里的描述如下
```
table levelup[id] (client) {
	id:int;
	upgradeExp:int; // 升级经验
	levelAward:int; // 等级奖励
}
```


## 新建或改结构


新建或修改csv或excel文件，csv文件前2行或前3行做为header，

    第一行是中文描述，
    第二行是程序用名
    第三行随意，我们可以约定填类型，但真正的类型以cfg里为准


然后交给程序就ok了

    程序使用cfggen 来完善cfg，如果cfg不满足需求，则手动修改cfg，
    比如修改类型，主键，增加唯一键，外键，枚举，取值约束等



## 改数据


更改csv或excel里的数据，随便改


然后根据需求双击配表顶层目录下的以下任一文件

* 校验数据.bat
* 校验数据并生成客户端配置.bat
* 校验数据并生成服务器配置.bat
* 校验数据并生成客户端和服务器配置.bat



如果出错，根据出错提示修改数据，如果正确，则可以提交svn了。

我们支持表之间的链接关系（可以让程序来在.cfg中配置），比如完成任务里的KillMonster的第一个参数是monsterid，这个id必须在monster表中存在。



## 忽略机制

- 忽略文件

  csv文件或excel里的sheet名称如果不是a-z，A-Z开头的就忽略，策划可以多建sheet或csv来做说明

- 忽略列或行

    - 如果第二行，也就是程序用名中的格子为空，则表明这列程序不用，策划随便写。
    - 如果数据行有一行全为空，或一行的第一个格子内容以#口头，则会被程序忽略。

| 序号       | 策划用列 | 名字    | 掉落0件物品的概率 | 掉落1件物品的概率 |
|----------|------|-------|-----------|-----------|
| lootid   |      | name  | chance1   | chance2   |
| 1        |      | 测试掉落  | 100       | 200       |
| #这行会被忽略 |      |       |           |           |
| 2        | XXX用 | 小宝箱   | 0         | 100       |
|          |      |       |           |           |
| 4        |      | 大宝箱   | 0         | 100       |
| 5        |      | 测试掉落2 | 20        | 10        |

上例中，第2列、第4行、第6行 都会被忽略


## 文件名、目录名规范

### 目录命名规则
- 首字母必须是英文字符
- 命名解析逻辑：
   ```
   截取第一个"."之前的内容 → 再截取"_汉字"或汉字之前的部分 → 作为module名
   ```

### CFG文件
- 根目录下必须存在 `config.cfg`
- 每个module目录下需有 `[module].cfg`

### 通用忽略规则
- 忽略以下文件：
  - 以`~`开头的文件
  - 隐藏文件

### CSV文件
- 文件后缀：`.csv`
- 文件名称不是a-z，A-Z开头的就忽略
- 命名解析逻辑：
  ```
  截取".csv"之前的内容 → 再截取"_汉字"或汉字之前的部分 → 作为table名
  ```
- 合法命名格式：
  - `[table]_[idx]`
  - `[table]`

### Excel文件
- 文件后缀：`.xls` 或 `.xlsx`
- Sheet名称不是a-z，A-Z开头的就忽略
- Sheet命名规则：
  ```
  截取"."之前的内容 → 再截取"_汉字"或汉字之前的部分 → 作为table名
  ```
- 合法命名格式：
  - `[table]_[idx]`
  - `[table]`

### JSON文件
- 目录命名规则：
  ```
  _[table.replace(".", "_")]/
  ```
  若table为`skill.buff` → 对应目录为`_skill_buff`
- 文件命名则是以主键pack为字符串来命名。比如1.json

### example/config
```
│   config.cfg
│
├───ai_行为
│       ai.cfg
│       ai行为.xlsx
│
├───equip
│       ability.csv
│       equip.cfg
│       equipconfig.csv
│       jewelry.csv
│       jewelryrandom.csv
│       jewelrysuit.csv
│       jewelrytype.csv
│       rank.csv
│
├───other
│       drop.csv
│       loot.csv
│       lootitem.csv
│       lootitem_1.csv
│       lootitem_2.csv
│       monster.csv
│       other.cfg
│       signin.csv
│
├───task
│       completeconditiontype任务完成条件类型.csv
│       task.cfg
│       taskextraexp.csv
│       task_任务.csv
│
├───_other_keytest
│       0.json
│       1,2.json
│
└───_task_task2
        1.json
        2.json
        3.json
        4.json
        5.json
        6.json
        7.json
        8.json
```

## 兼容老的表格

这个功能是为了兼容老的表格，但是个通用功能可以用于只读datadir目录下的部分目录功能。

涉及到4个参数

- -asroot  
    ```
    兼容之前的目录结构，有ClientTables、PublicTables、ServerTables目录，目录下是.txt后缀的tsv文件。
    可以配置为'ClientTables:noserver,PublicTables,ServerTables:noclient',
    配合gen的own:-noclient来提取客户端数据, own:-noserver来提取服务器数据
    （注意noclient前的-，配合no实现了双重否定，忽略设置了noclient的ServerTables）
    ```

- -exceldirs 
    ```
    excel目录，以,分隔
    ```

- -jsondirs
    ```
    json目录以,分隔，
    -asroot、-exceldirs、-jsondirs一旦有一个配置，说明要明确只用-datadir下的部分目录，而不是全部。
    ```

- -headrow
    ```
    老的配置，.txt后缀的tsv文件里，head是4行
    第一行是程序用名
    第二行是类型（INT，SHORT，BYTE，INT64，FLOAT，BOOL，STRING）
    第三行是其他信息
    第四行是中文描述
    ```
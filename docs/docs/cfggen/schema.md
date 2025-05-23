---
layout: page
title: 结构定义
parent: 配表系统
nav_order: 4
---

# 结构定义
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

- TOC
{:toc}
---


## 例子：任务表
以下是个任务表的结构，通过这个例子来说明

```
struct Range {
	rmin:int; // 最小
	rmax:int; // 最大
}

struct RewardItem {
	chance:int; // 掉落概率
	itemids:list<int> ->item.item (fix=2); // 掉落物品
	range:Range; // 数量下限
}

struct Time (sep=':'){
    hour:int;
    minute:int;
    second:int;
}

interface completecondition (enumRef='completeconditiontype') {
	struct KillMonster {
		monsterid:int ->npc.monster;
		count:int;
	}

	struct TalkNpc {
		npcid:int ->npc.npcid;
	}

	struct ConditionAnd {
		cond1:task.completecondition (pack);
		cond2:task.completecondition (pack);
	}

	struct CollectItem {
		itemid:int ->item.item;
		count:int;
	}
}

interface taskexp (enumRef='taskexptype', defaultImpl='ConstValue', pack) {
    struct ByLevel { // 玩家等级相关 
		levelcoef:float;
		value:int;
	}

	struct ByServerUpDay { // 服务器启动天数相关
		updaycoef1:float;
		updaycoef2:float;
		value:int;
	}
	
	struct ConstValue { // 固定值
	    value:int;
	}
}

table completeconditiontype[id] (enum='name') {
	id:int; // 任务完成条件类型
	name:str; // 程序用名字
}

table taskexptype[id] (enum='name') {
	id:int; // 经验公式类型
	name:str; // 程序用名字
}

table task[taskid] (entry='entry') {
	taskid:int ->task.taskextraexp (nullable); // 任务id
	entry:str;
	text:text; // 需要国际化的文本
	nexttask:int ->task (nullable);
	completecondition:task.completecondition; // 任务完成条件
	exp:taskexp;  // 经验奖励
	rewardItems:list<RewardItem> (block=1); // 物品奖励
	time:Time; 
	[nexttask];
}

```

## 结构化支持

### struct：定义结构

- struct后接结构体名称，然后在{}里每行定义field或外键
- field：字段名名字后面加:，然后是**字段类型**
- 字段类型：
  - 基本类型primitive：bool，int，float，long，str，text
  - 聚合：struct，interface
  - 容器：list，map
- 字段类型后可以接->，表明是**外键**，后接table名称，如果不是指向table的主键，则要接[]，[]内含table的唯一键
- 字段类型后可以接=>，后接table名称[]，[]内含table的field名称 这里扩展了数据库的外键含义，
  用=>表明不一定指向table的唯一键，而是任意字段，生成的程序代码会返回listRefXxx，是个列表。
- {}内行如果以->开头，表明是外键，->后接一个identifier，然后:，然后[],[]内是字段名列表，然后是->或=>,之后跟字段类型后接的一致。
- list类型配置外键时，表明list里每个item的外键
- map类型配置外键时，表明时map里每个value的外键，（不支持key的外键配置）


> RewardItem的range结构是Range，这是对嵌套结构的支持。
> 
> RewardItem.itemids 里配置->item.item，表明这个list\<int\>里每个int都指向item.item表。
> 
> KillMonster.monsterid 设置->npc.monster， 表明这个id是个外键，指向npc.monster表。
> 

### interface：定义接口

- interface内定义struct做为此接口的实现类
- interface后面()内可以加**enumRef**属性，指向一个表名，这个表里会有其实现类的信息，
  假如这个表策划用excel来配，则有enumRef后策划可以不用看.cfg文件。这个表必须包含其实现类的名称，也可以给id，可以包含任意额外信息。
- ()内可以加**defaultImpl**属性，指向一个此接口实现类，如果有这个属性，则excel中可以不配置这个属性，此时程序会读成这个默认实现。
- 如果defaultImpl指向的结构，只包含一个数字或bool，并且此interface是pack，则excel中可以直接写一个数字，此时程序会读成这个默认实现，里面字段为此数字。
   

> completecondition是任务的完成条件，其中ConditionAnd里的有字段其类型又是completecondition，形成了环，这样结构就是任意复杂的了。
> 
> taskexp是任务经验值公式。task表中经验奖励的格子可以大多数情况下只写数字就可以了，代表ConstValue。如果需要公式可以配置为ByLevel(10, 100)，服务器读到这个结构可以用公式
> role.level() * levelcoef + value得到具体的经验值。

### table：定义excel文件对应的结构
- 表名后[]内是此表的**主键**
- {}内每行如果以[开始，以]结束， 则表示是在定义**唯一键**
- 否则跟struct相同每行定义field或外键。
- 表名[主键]后的()内可以包含**enum**，指向field名称，表明此表是个枚举表，excel中这一列由程序员来填写，每行都要填
- 表名[主键]后的()内可以包含**entry**，指向field名称，这个会为程序生成静态成员变量，方便代码访问。 excel中这一列由程序员来填写，只有少数行需要填写。

> task表的[taskid]：主键是第一列taskid
> 
> task表{}里的最后一行[nexttask]：定义了唯一键，会生成对应的访问代码GetByNextTask，给出nextTaskId，查找到task
> 
> task表的(entry='entry'')，表明有其中一些行，程序代码需要访问，在对应行的第二列entry由程序来填上名字。
> 
> taskexptype表中的(enum='name')，表明这是个枚举表，excel数据要包含3行，第二列name里的字符串要分别是taskexp中的struct名字：ByLevel，ByServerUpDay，ConstValue



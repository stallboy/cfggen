---
layout: page
title: 最佳实践
nav_order: 20
---

# 最佳实践
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

- TOC
{:toc}
---

## 减少拆表

原来如果要模块化概念，可能要把它放到单独的一个配表中，其他表要引用这个就通过一个id，这样配置一个功能时需要牵涉到多个表格文件，
现在则可以定义这个概念为结构，在这个功能表格中直接配置这个结构。

通过[表列数太多怎么办？](#column_reduce)一节的处理方法，减少拆表，把需要配置的内容紧密放在一起。


## 表行数太多怎么办？

比如item表，太多行，那可以分开item.csv，item_1.csv，item_2.csv......如果是excel可以是分成多个sheet，item，item_1，item_2......

逻辑上认为就还是一个item表

策划规划拆表规则时，既可以按照比如物品类型来拆表，所有装备放到item_1，宝石放到item_2里；也可以按照id分段来拆。

## 表列数太多怎么办？

### 对简单的结构，配置pack或sep

表明这个结构只占一格，用pack的话用逗号分隔，用sep则可以用自定义的符号分割。
如下，Position默认是占3列，声明sep后，占1列

```
struct Position (sep=';') {
    x:int;
    y:int;
    z:int;
}
```

### 对简单的列表，配置sep或pack或block

比如list\<int\>, list\<float\>, list\<str\>；也支持map\<int, int>可以配置pack，block


如下：饰品套装的部件subList最多只有4个，则用fix=4是很合适的，只占4列。

```
table jewelrysuit[SuitID] (entry='Ename') {
    SuitID:int; // 饰品套装ID
    Ename:str;
    Name:text; // 策划用名字
    SuitList:list<int> (fix=4); // 子部件列表
}
```

如果这个部件大多数情况下都小于4，但少数情况会有12个。

- 如果配fix=12 如下，则固定占12列，可能太多了。
```
table jewelrysuit[SuitID] (entry='Ename') {
    ...
    SuitList:list<int> (fix=12);
}
```

- 可以配置sep或pack，让它都只占一列。
```
table jewelrysuit[SuitID] (entry='Ename') {
    ...
    SuitList:list<int> (pack);
}
```

```
table jewelrysuit[SuitID] (entry='Ename') {
    ...
    SuitList:list<int> (sep=';');
}
```

- 也可以配置block=4，占4列；一个记录大多数占一行，少数情况占2，3行。
```
table jewelrysuit[SuitID] (entry='Ename') {
    ...
    SuitList:list<int> (block=4);
}
```
### 对结构列表，配置block

比如20个复合结构，每个复合结构需要4列，那就需要20*4=80列，这应该是导致列数失控的罪魁祸首。大致有3种解法，先推荐最直接的，用block=1

```
struct DropItem {
    chance:int; // 掉落概率
    itemids:list<int> (block=1); // 掉落物品
    countmin:int; // 数量下限
    countmax:int; // 数量上限
}

table drop[dropid] {
    dropid:int; // 序号
    name:text; // 名字
    items:list<DropItem> (block=1); // 掉落概率
}
```
如上DropItem占4列，drop表里的items配置了block=1，表明它也占用4列，但这个list通过占用多行可以有任意多个，如下：

| 序号     | 名字         | 掉落概率         | 掉落物品列表，这个用列表是为了测试block嵌套block | 数量下限     | 数量上线     |
|--------|------------|--------------|-------------------------------|----------|----------|
| dropid | name       | items,chance | itemids                       | countmin | countmax |
| 1      | 测试掉落       | 100          | 1001                          | 10       | 20       |
|        |            |              | 1002                          |          |          |
|        |            |              | 1003                          |          |          |
|        |            | 10           | 2001                          | 10       | 10       |
|        |            | 10           | 2002                          | 0        | 1        |
|        |            | 50           | 3001                          | 1        | 1        |
| 2      | 剧情任务测试2    | 100          | 10001                         | 1        | 1        |
| 3      | 通告栏掉落染色模板1 | 80           | 20001                         | 10       | 20       |

这个表也演示了，block里可以嵌套block

### 对结构列表，用两个表，使用=>

```
table dropItem[dropItemId] {
    dropItemId:int;
    dropid:int ->drop;
    chance:int; // 掉落概率
    itemids:list<int> (block=1); // 掉落物品
    countmin:int; // 数量下限
    countmax:int; // 数量上限
}

table drop[dropid] {
    dropid:int =>dropItem[dropid];
    name:text; 
}
```

* dropItem是另一个表，里面包含了dropid来索引->到drop。
* drop表中dropid 通过=>(不是->)索引到dropItem的dropid列，程序代码会生成list\<DropItem>。
* 这里的会产生额外的dropItemId，一般情况下是不需要的，所以推荐3里的block做法。

### 对结构列表，用两个表，使用list\<int>

```
table dropItem[dropItemId] {
    dropItemId:int;
    chance:int; // 掉落概率
    itemids:list<int> (block=1); // 掉落物品
    countmin:int; // 数量下限
    countmax:int; // 数量上限
}

table drop[dropid] {
    dropid:int;
    name:text; // 名字
    items:list<int> ->dropItem (pack); // 掉落概率
}
```
* 跟4一样引入了dropItemId，但这里drop对dropItem列表的索引直观的通过list\<int>来取得。
* 好处：一行dropItem可以被多个drop公用

### 对行少列多，用列模式

参考[模块参数表例子]

## 单元格

### 可空的单元格

比如dialog表，可以配置一个npcid，如果配置了npcid，则朝向他，如果没配的话就不转向

则如果不配置npc，就把npcid对应的单元格留空，不要填0，-1

> excel或csv单元格中不填的话默认为false,0,""，所以不要用0作为一行的id。
>
> 如果有nullableRef请不要填0，请用留空。否则程序会检测报错.



### 复杂结构的单元格

比如代币奖励，有两个字段，一个是代币类型，一个是数量，可以在一个单元格里配置，比如

```
struct LevelRank {
	Level:int; // 等级
	Rank:int ->equip.rank; // 品质
}
```

```
table xx ...{
    ...
    reward:Reward (pack);
}
```

| reward |
|--------|
| 1,100  |

甚至可以在一个单元格里配置代币奖励列表

```
table xx ...{
    ...
    rewardList:list<Reward> (pack);
}
```

| rewardList     |
|:---------------|
| (1,100),(2,50) |

还支持多态的类型，比如task表，则可以定义接口如下：

``` 
interface completecondition (enumRef='completeconditiontype') {
	struct KillMonster {
		monsterid:int ->other.monster;
		count:int;
	}

	struct TalkNpc {
		npcid:int;
	}

	struct ConditionAnd {
		cond1:task.completecondition (pack);
		cond2:task.completecondition (pack);
	}

	struct CollectItem {
		itemid:int;
		count:int;
	}
}
```

任务表为：

```
table task...{
    ...
    condition: completecondition;
}
```

| Condition                               |
|:----------------------------------------|
| KillMonster(1001, 1)                    |
| CondAnd(TalkNpc(5),KillMonster(101, 3)) |

## 使用举例

### 物品表

比如物品表item，物品包含装备，宝石，货币等分类，每个分类下可能有独特的属性，怎么配置舒服呢？

item表，有type字段指向itemtype表，itemtype表里配置上装备，宝石，货币等分类；item表用于配置所有分类共有的属性，比如名称，允许堆叠上限等，

装备equip，跟宝石gem可能有各自独特的属性，那再加上itemequip表，itemgem表。
这两个表内的id跟item表相同，配置它索引到item表，然后其他列加上equip，gem特有的属性就行。

只配置一个item表，item表定义前面是共有字段，后面有个extra字段是多态类型，可以是装备，宝石，货币，各包含自己独有的属性。

然后再利用我们已有分拆文件功能，可以装备都放入item_1，宝石都放入item_2


* 第一种做法不好得地方在于：一个装备id，要配置在两处，一个item表，一个itemequip表。有重复。
* 第一种做法好处是其他表可以声明（ref）自己字段是个itemequip，如果用第二种方案，则只能声明字段是item。第二种程序也会要多做一些判断，
  我期望的是itemequip，但我拿到的类型是item，那item.extra是不是equip我需要判断啊，不是怎么处理。不好写。

### 模块参数表

一个模块一般会有通用参数需要配置，那怎么配置舒服呢？

- 可以配置一行，用entry来引用这整行。

- 但这样可能列数太多，看着不舒服，我们还有列模式机制，在这个表上配置columnMode，那可以把表反转90度，很多列，变成很多行，配置起来舒服多了

```
table equipconfig[entry] (entry='entry', columnMode) {
	entry:str; // 入口，程序填
	stone_count_for_set:int; // 形成套装的音石数量
	draw_protect_name:str; // 保底策略名称
	broadcastid:int; // 公告Id
	broadcast_least_quality:int; // 公告的最低品质
	week_reward_mailid:int; // 抽卡周奖励的邮件id
}
```

excel表为：

| 入口，程序填     | entry                   | Instance |
|------------|-------------------------|----------|
| 形成套装的音石数量  | stone_count_for_set     | 2        |
| 保底策略名称     | draw_protect_name       | 测试       |
| 公告Id       | broadcastid             | 9500     |
| 公告的最低品质    | broadcast_least_quality | 1003     |
| 抽卡周奖励的邮件id | week_reward_mailid      | 100      |


### 掉落表

见[对结构列表如何减少列数](#column_reduce_struct_list)一节的讨论

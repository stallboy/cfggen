# cfggen 给程序员看的介绍

<show-structure for="chapter,procedure" depth="2"/>

> 让策划可以在excel中做灵活的配置
> 
> 为程序生成访问配置表的代码
{style='note'}


## 主要功能

* 通过配置外键，取值范围，使策划可以随时检测数据一致性

* 通过生成代码，来访问类型化数据，生成外键引用，生成entry、enum，支持java、c#、lua

* 支持多态结构、嵌套结构，可在一个单元格里写任意复杂的结构数据，让csv有了xml的灵活性


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
- interface后面()内要有**enumRef**属性，指向一个表名，这个表里会有其实现类的信息，
  这里假设策划是不会看.cfg文件的，只用看excel就行，所以会有这个enumRef表。这个表必须包含其实现类的名称，也可以给id，可以包含任意额外信息
- ()内可以包含**defaultImpl**属性，指向一个此接口实现类，如果有这个属性，则excel中可以不配置这个属性，此时程序会读成这个默认实现。
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


## 结构映射到excel表格

以上我们支持嵌套结构，多态结构，递归结构，非常灵活；而excel是基于表格的结构。
如何把这种非常灵活的树状甚至递归的结构，映射到excel的一层的平坦的表格结构中呢？

这里我们有auto，pack, sep, fix, block五种映射方式。

### auto

适用type: primitive, struct, interface。

占格子数：primitive都是1，struct，interface则自动计算，不需要配置，默认就是auto

> Range中rmin占一列，rmax占一列，Range结构共占两列

### pack

适用type: struct, interface, list, map。

推荐使用，使用,()来写任何结构

> ConditionAnd.cond1 cond2都设置了(pack)，则这两个field各占一列，ConditionAnd 总共占2列。
> 
> 对于这个形成了环的递归结构，必须在某处用pack打断，要不然所占列数就无法计算。这里就是cond1，cond2处打断的。
> 
> 可以看到compeletecondition下4个struct最多占2列，则compeletecondition占2+1=3列，因为名称KillMonster或TalkNpc...要占第一列。
> 
> 至于pack如何以,()这三者写任意结构，参考 [复杂结构的单元格](Intro-to-designer.md#pack_to_one_cell)

### sep

适用type: struct, list

参数sep: 可以是, : = $等等都可以，用一个字符

* struct里field只有都是primitive，才能设置sep
* 不支持type为struct的field上设置sep（请在struct上设置，为了简单一致性）
* 如果field是list,struct结构，list这配置的sep和要struct配置的sep有区分才行（不支持在此field上配置struct的sep，为了方便理解，不要这种灵活性）。

> Time 设置了(sep=':')，这样这个结构只占一列，并且具体数据可以是"12:10:00" 
>
> 如果不是特别需要用某个分隔符，建议用更强大的pack

### fix

适用type: list, map

参数count：个数，占格子数 = 容器内元素占的格子 * count

* 横向扩展格子，当事先明确count限制，并且count不太大时，推荐使用

> RewardItem.itemids 设置fix=2，list里的单个item类型int占1列，所以此field占1*2=2列。
> RewardItem总共占5列。

### block

适用type: list, map

参数fix： 跟fix的count参数含义一致。

* fix参数负责横向扩展格子。
* 本身纵向随意扩展，会占用任意多行。

> task.rewardItems 配置block=1，list里的RewardItem占5列，所以此field占5*1=5列。


## 杂项

### columnMode

在table的meta里可配置columnMode，用于方便配置[模块参数表](Intro-to-designer.md#module_param)

### extraSplit

在table的meta里可配置extraSplit

为生成lua文件时是否为数据生成多个文件，默认为0。假如数据项有250行，extraSplit配置为100，则分为3个文件，会额外多出1,2两个文件，原文件和1各100行，2含50行。

- 引入这个是因为lua生成assets.lua时报错，assets.lua是资源系统自动生成的一个文件，里面会包含很多行，因为lua单个文件不能多余65526个constant，生成lua文件会报错，所以这里分割一下
- 还有个好处是热更时减少下载文件大小，比如item表有10000个，起始大部分情况下热更时就改几行，如果不split那就热更整个文件，如果split成了5个文件，那很可能就只用热更这5个中的一个，减少了热更大小。

### tag

在table的meta里可配置任意tag，比如我们一般会包含client（用更精简的c也行）。用于提取特定的字段，减少客户端使用内存。

* 在field上标注tag就行，不用标注foreign key。foreign key是否提取，只由是否可行决定，能包含就包含。

* 如果在struct或interface上配置了tag，分3种情况

  1. 所有field都没tag，-tag, 则包含所有field 
  2. 有部分field设了tag，则取这设置了tag的field 
  3. 没有设置tag的，但有部分设置了-tag，则提取没设-tag的field

* 一般情况下，impl不需要设置tag，* 如果impl上设置tag，则是为了能filter出空结构，相当于只用此impl类名字做标志，普通的struct不支持filter出空结构。


## cfg文件格式

cfg文件的antlrv4定义大致如下，熟悉bnf格式的，可以参考

<chapter title="cfg文件语法" collapsible="true"  default-state="collapsed">

```

grammar Cfg ;

schema : schema_ele* EOF ;

schema_ele: struct_decl | interface_decl | table_decl ;

struct_decl : STRUCT ns_ident metadata LC COMMENT? field_decl* foreign_decl*  RC ;

STRUCT: 'struct';

interface_decl : INTERFACE ns_ident metadata LC COMMENT? struct_decl+ RC ;

INTERFACE: 'interface';

table_decl : TABLE ns_ident key metadata LC COMMENT? key_decl* field_decl* foreign_decl*  RC ;

TABLE: 'table';

field_decl : identifier COLON type_ ( ref )? metadata SEMI COMMENT? ;

foreign_decl: REF identifier COLON key ref metadata SEMI COMMENT? ;

type_ : TLIST '<' type_ele '>' |  TMAP '<' type_ele ','  type_ele '>' | type_ele;

type_ele : TBASE | ns_ident;

TLIST : 'list';
TMAP: 'map';
TBASE : 'bool' | 'int' | 'long' | 'float' | 'str' | 'res' | 'text' ;

ref:  (REF | LISTREF) ns_ident key? ;

REF: '->';
LISTREF: '=>';

key_decl : key SEMI ;

key: '[' identifier (',' identifier)* ']' ;

COMMENT: '//' ~[\r\n]* ;

metadata : ( LP ident_with_opt_single_value ( COMMA ident_with_opt_single_value )* RP )? ;

ident_with_opt_single_value : identifier (EQ single_value)? |  minus_ident;

minus_ident: MINUS identifier;

single_value : INTEGER_CONSTANT | HEX_INTEGER_CONSTANT | FLOAT_CONSTANT | STRING_CONSTANT ;

ns_ident : identifier ( DOT identifier )* ;

identifier: IDENT | keywords;

IDENT : [a-zA-Z_] [a-zA-Z0-9_]* ;

keywords: STRUCT | INTERFACE | TABLE | TLIST | TMAP | TBASE;

```
</chapter>

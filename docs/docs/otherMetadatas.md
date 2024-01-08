---
layout: page
title: meta杂项设置
nav_order: 9
---


# meta杂项设置
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

- TOC
{:toc}
---


## 杂项

### columnMode

在table的meta里可配置columnMode，用于方便配置[模块参数表]

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

<details markdown="block">
<summary>cfg文件的antlrv4定义大致如下，熟悉bnf格式的，可以参考</summary>


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


</details>

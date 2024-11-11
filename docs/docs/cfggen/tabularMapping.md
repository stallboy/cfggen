---
layout: page
title: 映射到表格
parent: 配表系统
nav_order: 8
---

# 映射到表格
{: .no_toc }

## Table of contents
{: .no_toc .text-delta }

- TOC
{:toc}
---

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
> 至于pack如何以,()这三者写任意结构，参考 [复杂结构的单元格]

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

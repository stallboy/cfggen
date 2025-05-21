# 主键，副键，联合形态

最常见的主键，一般只用一个filed来定义。

键支持联合形态，用多个field构成一个唯一索引。
可以把主键设置成联合形态。
请参考other.lootitem。

主键并不是唯一的索引方式，支持增加副键。并且副键也支持联合形态。
请参考other.lootitem。

所有的Key变量需要类型检查，只支持bool int long string四种类型，不支持struct作为键。


主键、唯一键、联合键的说法容易产生概念混淆。

Key本身就有唯一的含义，前面加Unique听起来像没有其他键的意思。

Primary是主要的意思，Unique跟它不能对应。

联合键的说法像是多个键组合成一个键，但设计上并非如此。
比如signin的viplevel并不能作为一个独立的键。
而是可以由多个field联合。

tableSchema里，感觉也不用拆分成primaryKey和uniqueKeys，都归为keys，第一个key就是主key。

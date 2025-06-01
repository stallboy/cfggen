# cfgMgr.go 的设计
每个table文件里，怎么提供访问接口。
先提供唯一名字的mgr，由mgr来提供访问。
否则容易重名。

# 主键，副键，联合形态

最常见的主键，一般只用一个filed来定义。

键支持联合形态，用多个field构成一个唯一索引。
可以把主键设置成联合形态。
请参考other.lootitem。

主键并不是唯一的索引方式，支持增加副键。并且副键也支持联合形态。
请参考other.lootitem。

如果主键是联合形态，不能被ref，只能=>到其中的一个field，会返回所有符合这个key的容器。

主键可以直接=>到联合键。

GO版本不支持直接ref到一个struct类型的主键

# 外键
“外键”（Foreign Key）是数据库中的一个重要概念，用来表示一个表中的字段引用了另一个表的主键，从而在两个表之间建立起数据的关联关系。
cfggen的外键有多种类型，包括RefList, RefPrimary, RefUniq
当ForeignKeySchema的refKey()是RefList时，表示它是一个列表引用，意味着当前表中的一个记录可以关联到目标表中的多个记录。
在cfg文件里，通过=>来表示这种关联关系。
比如，lootid:int =>lootitem[lootid] 中的 => 表示一种外键关联逻辑。
lootid 是当前表的一个字段，它通过外键关联到 lootitem 表中的 lootid 字段，将返回所有符合这个条件的 lootitem 记录。

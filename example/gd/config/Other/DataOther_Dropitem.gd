class_name DataOther_Dropitem
## other.DropItem
# 公开属性
var chance: int:
	get:
		return chance  # 掉落概率
var itemids: Array[int]:
	get:
		return itemids  # 掉落物品
var countmin: int:
	get:
		return countmin  # 数量下限
var countmax: int:
	get:
		return countmax  # 数量上限
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataOther_Dropitem:
	var instance = DataOther_Dropitem.new()
	instance.chance = stream.get_32()
	instance.itemids = []
	for c in range(stream.get_32()):
		instance.itemids.append(stream.get_32())
	instance.countmin = stream.get_32()
	instance.countmax = stream.get_32()
	return instance

# 解析外键引用

class_name DataOther_Dropitem
## other.DropItem
# 公开属性
var chance: int  # 掉落概率
var itemids: Array[int]  # 掉落物品
var countmin: int  # 数量下限
var countmax: int  # 数量上限
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataOther_Dropitem:
	var instance = DataOther_Dropitem.new()
	instance.chance = stream.read_int32()
	for c in range(stream.read_int32()):
		instance.itemids.append(stream.read_int32())
	instance.countmin = stream.read_int32()
	instance.countmax = stream.read_int32()
	return instance

# 解析外键引用
# 字符串表示
func _to_string() -> String:
	return "DataOther_Dropitem{" + str(chance) + "," + str(itemids) + "," + str(countmin) + "," + str(countmax) + "}"

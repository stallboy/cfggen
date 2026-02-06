class_name DataOther_Lootitem
## other.lootitem
# 公开属性
var lootid: int:
	get:
		return lootid  # 掉落id
var itemid: int:
	get:
		return itemid  # 掉落物品
var chance: int:
	get:
		return chance  # 掉落概率
var countmin: int:
	get:
		return countmin  # 数量下限
var countmax: int:
	get:
		return countmax  # 数量上限
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataOther_Lootitem:
	var instance = DataOther_Lootitem.new()
	instance.lootid = stream.get_32()
	instance.itemid = stream.get_32()
	instance.chance = stream.get_32()
	instance.countmin = stream.get_32()
	instance.countmax = stream.get_32()
	return instance

# 主键查询
static func find(id: int) -> DataOther_Lootitem:
	return _data.get(id)

# 获取所有数据
static func all() -> Array[DataOther_Lootitem]:
	return _data.values()

# 从流初始化
static func _init_from_stream(stream: ConfigStream, err: ConfigErrors):
	var count = stream.get_32()
	for i in range(count):
		var item = create(stream)
		_data[item.lootid] = item
# 内部存储
static var _data: Dictionary[int, DataOther_Lootitem] = {}
# 解析外键引用

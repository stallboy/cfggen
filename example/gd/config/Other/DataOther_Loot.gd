class_name DataOther_Loot
## other.loot
# 公开属性
var lootid: int  # 序号
var ename: String
var name: String  # 名字
var chanceList: Array[int]  # 掉落0件物品的概率
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataOther_Loot:
	var instance = DataOther_Loot.new()
	instance.lootid = stream.read_int32()
	instance.ename = stream.read_string_in_pool()
	instance.name = stream.read_text_in_pool()
	for c in range(stream.read_int32()):
		instance.chanceList.append(stream.read_int32())
	return instance

# 主键查询
static func find(id: int) -> DataOther_Loot:
	return _data.get(id)

# 获取所有数据
static func all() -> Array[DataOther_Loot]:
	return _data.values()

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	var count = stream.read_int32()
	for i in range(count):
		var item = create(stream)
		_data[item.lootid] = item
# 内部存储
static var _data: Dictionary[int, DataOther_Loot] = {}
# 解析外键引用

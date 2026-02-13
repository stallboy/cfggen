class_name DataOther_Loot
## other.loot
# 公开属性
var lootid: int  # 序号
var ename: String
var name: ConfigText  # 名字
var chanceList: Array[int]  # 掉落0件物品的概率

# 内部存储
static var _data: Dictionary[int, DataOther_Loot] = {}
# 主键查询
static func find(id: int) -> DataOther_Loot:
	return _data.get(id)
# 获取所有数据
static func all() -> Array[DataOther_Loot]:
	return _data.values()

# 字符串表示
func _to_string() -> String:
	return "DataOther_Loot{" + str(lootid) + "," + ename + "," + str(name) + "," + str(chanceList) + "}"

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	var count = stream.read_int32()
	for i in range(count):
		var item = _create(stream)
		_data[item.lootid] = item

# 创建实例
static func _create(stream: ConfigStream) -> DataOther_Loot:
	var instance = DataOther_Loot.new()
	instance.lootid = stream.read_int32()
	instance.ename = stream.read_string_in_pool()
	instance.name = ConfigText._create(stream)
	for c in range(stream.read_int32()):
		instance.chanceList.append(stream.read_int32())
	return instance



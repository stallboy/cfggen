class_name DataEquip_Jewelryrandom
## equip.jewelryrandom
# 公开属性
var lvlRank: DataLevelrank:
	get:
		return lvlRank  # 等级
var attackRange: DataRange:
	get:
		return attackRange  # 最小攻击力
var otherRange: Array[DataRange]:
	get:
		return otherRange  # 最小防御力
var testPack: Array[DataEquip_Testpackbean]:
	get:
		return testPack  # 测试pack
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataEquip_Jewelryrandom:
	var instance = DataEquip_Jewelryrandom.new()
	instance.lvlRank = DataLevelrank.create(stream)
	instance.attackRange = DataRange.create(stream)
	instance.otherRange = []
	for c in range(stream.get_32()):
		instance.otherRange.append(DataRange.create(stream))
	instance.testPack = []
	for c in range(stream.get_32()):
		instance.testPack.append(DataEquip_Testpackbean.create(stream))
	return instance

# 主键查询
static func find(id: DataLevelrank) -> DataEquip_Jewelryrandom:
	return _data.get(id)

# 获取所有数据
static func all() -> Array[DataEquip_Jewelryrandom]:
	return _data.values()

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	var count = stream.get_32()
	for i in range(count):
		var item = create(stream)
		_data[item.lvlRank] = item
# 内部存储
static var _data: Dictionary[DataLevelrank, DataEquip_Jewelryrandom] = {}
# 解析外键引用
func _resolve(errors: ConfigErrors):
	if lvlRank != null:
		lvlRank._resolve(errors)
static func _resolve_refs(errors: ConfigErrors):
	for item in all():
		item._resolve(errors)

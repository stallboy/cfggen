class_name DataOther_Monster
## other.monster
# 公开属性
var id: int:
	get:
		return id
var posList: Array[DataPosition]:
	get:
		return posList
var lootId: int:
	get:
		return lootId  # loot
var lootItemId: int:
	get:
		return lootItemId  # item
# 外键引用属性
var RefAllLoot: DataOther_Loot:
	get:
		return RefAllLoot
# 创建实例
static func create(stream: ConfigStream) -> DataOther_Monster:
	var instance = DataOther_Monster.new()
	instance.id = stream.get_32()
	instance.posList = []
	for c in range(stream.get_32()):
		instance.posList.append(DataPosition.create(stream))
	instance.lootId = stream.get_32()
	instance.lootItemId = stream.get_32()
	return instance

# 主键查询
static func find(id: int) -> DataOther_Monster:
	return _data.get(id)

# 获取所有数据
static func all() -> Array[DataOther_Monster]:
	return _data.values()

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	var count = stream.get_32()
	for i in range(count):
		var item = create(stream)
		_data[item.id] = item
# 内部存储
static var _data: Dictionary[int, DataOther_Monster] = {}
# 解析外键引用
func _resolve(errors: ConfigErrors):
	RefAllLoot = DataOther_Loot.find(lootId)
	if RefAllLoot == null:
		errors.ref_null("other.monster", "AllLoot")
static func _resolve_refs(errors: ConfigErrors):
	for item in all():
		item._resolve(errors)

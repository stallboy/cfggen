class_name DataOther_Monster
## other.monster
# 公开属性
var id: int
var posList: Array[DataPosition]
var lootId: int  # loot
var lootItemId: int  # item
# 外键引用属性
var RefAllLoot: DataOther_Loot

# 内部存储
static var _data: Dictionary[int, DataOther_Monster] = {}
# 主键查询
static func find(id: int) -> DataOther_Monster:
	return _data.get(id)
# 获取所有数据
static func all() -> Array[DataOther_Monster]:
	return _data.values()

# 字符串表示
func _to_string() -> String:
	return "DataOther_Monster{" + str(id) + "," + str(posList) + "," + str(lootId) + "," + str(lootItemId) + "}"

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	var count = stream.read_int32()
	for i in range(count):
		var item = _create(stream)
		_data[item.id] = item

# 创建实例
static func _create(stream: ConfigStream) -> DataOther_Monster:
	var instance = DataOther_Monster.new()
	instance.id = stream.read_int32()
	for c in range(stream.read_int32()):
		instance.posList.append(DataPosition._create(stream))
	instance.lootId = stream.read_int32()
	instance.lootItemId = stream.read_int32()
	return instance


# 解析外键引用
func _resolve(errors: ConfigErrors):
	RefAllLoot = DataOther_Loot.find(lootId)
	if RefAllLoot == null:
		errors.ref_null("other.monster", "AllLoot")

static func _resolve_refs(errors: ConfigErrors):
	for item in all():
		item._resolve(errors)

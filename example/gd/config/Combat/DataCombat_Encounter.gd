class_name DataCombat_Encounter
## combat.encounter
# 公开属性
var id: int
var command: DataCombat_Command

# 内部存储
static var _data: Dictionary[int, DataCombat_Encounter] = {}
# 主键查询
static func find(key_0: int) -> DataCombat_Encounter:
	return _data.get(key_0)
# 获取所有数据
static func all() -> Array[DataCombat_Encounter]:
	return _data.values()

# 字符串表示
func _to_string() -> String:
	return "DataCombat_Encounter{" + str(id) + "," + str(command) + "}"

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	_data = {}
	var count = stream.read_count(1)
	for i in range(count):
		var item = _create(stream)
		_data[item.id] = item

# 创建实例
static func _create(stream: ConfigStream) -> DataCombat_Encounter:
	var instance = DataCombat_Encounter.new()
	instance.id = stream.read_int32()
	instance.command = DataCombat_Command._create(stream)
	return instance


# 解析外键引用
func _resolve(errors: ConfigErrors):
	pass
	if command != null:
		command._resolve(errors)

static func _resolve_refs(errors: ConfigErrors):
	for item in all():
		item._resolve(errors)

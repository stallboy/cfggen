class_name DataCombat_Phasebook
## combat.phasebook
# 公开属性
var school: String
var tier: int
var airborne: bool
var code: String
var edition: int
var entry: DataCombat_BookEntry

# 内部存储
static var _data: Dictionary[String, DataCombat_Phasebook] = {}
static var _code_edition_map: Dictionary[String, DataCombat_Phasebook] = {}
# 主键查询
static func find(key_0: String, key_1: int, key_2: bool) -> DataCombat_Phasebook:
	return _data.get(ConfigProcessor._object_key([key_0, key_1, key_2]))
# 唯一键查询
static func find_by_code_edition(key_0: String, key_1: int) -> DataCombat_Phasebook:
	return _code_edition_map.get(ConfigProcessor._object_key([key_0, key_1]))
# 获取所有数据
static func all() -> Array[DataCombat_Phasebook]:
	return _data.values()

# 字符串表示
func _to_string() -> String:
	return "DataCombat_Phasebook{" + school + "," + str(tier) + "," + str(airborne) + "," + code + "," + str(edition) + "," + str(entry) + "}"

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	_data = {}
	_code_edition_map = {}
	var count = stream.read_count(1)
	for i in range(count):
		var item = _create(stream)
		_data[ConfigProcessor._object_key([item.school, item.tier, item.airborne])] = item
		_code_edition_map[ConfigProcessor._object_key([item.code, item.edition])] = item

# 创建实例
static func _create(stream: ConfigStream) -> DataCombat_Phasebook:
	var instance = DataCombat_Phasebook.new()
	instance.school = stream.read_string_in_pool()
	instance.tier = stream.read_int32()
	instance.airborne = stream.read_bool()
	instance.code = stream.read_string_in_pool()
	instance.edition = stream.read_int32()
	instance.entry = DataCombat_BookEntry._create(stream)
	return instance



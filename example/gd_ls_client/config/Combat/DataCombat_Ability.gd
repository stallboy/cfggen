class_name DataCombat_Ability
## combat.ability
# 公开属性
var id: int
var code: String
var origin: DataCombat_Origin
var machines: Array[DataCombat_Machine]
var startMachine: String
var startPhase: String
# 外键引用属性
var RefInitial: DataCombat_Phase:
	get:
		if not has_meta(&"_cfggen_RefInitial"):
			return null
		var target: WeakRef = get_meta(&"_cfggen_RefInitial") as WeakRef
		return target.get_ref() as DataCombat_Phase if target != null else null
	set(value):
		if value != null:
			set_meta(&"_cfggen_RefInitial", weakref(value))
		elif has_meta(&"_cfggen_RefInitial"):
			remove_meta(&"_cfggen_RefInitial")
var RefSpawn: DataCombat_Origin:
	get:
		if not has_meta(&"_cfggen_RefSpawn"):
			return null
		var target: WeakRef = get_meta(&"_cfggen_RefSpawn") as WeakRef
		return target.get_ref() as DataCombat_Origin if target != null else null
	set(value):
		if value != null:
			set_meta(&"_cfggen_RefSpawn", weakref(value))
		elif has_meta(&"_cfggen_RefSpawn"):
			remove_meta(&"_cfggen_RefSpawn")

# 内部存储
static var _data: Dictionary[int, DataCombat_Ability] = {}
static var _code_map: Dictionary[String, DataCombat_Ability] = {}
# 主键查询
static func find(key_0: int) -> DataCombat_Ability:
	return _data.get(key_0)
# 唯一键查询
static func find_by_code(key_0: String) -> DataCombat_Ability:
	return _code_map.get(key_0)
# 获取所有数据
static func all() -> Array[DataCombat_Ability]:
	return _data.values()

# 字符串表示
func _to_string() -> String:
	return "DataCombat_Ability{" + str(id) + "," + code + "," + str(origin) + "," + str(machines) + "," + startMachine + "," + startPhase + "}"

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	_data = {}
	_code_map = {}
	var count = stream.read_count(1)
	for i in range(count):
		var item = _create(stream)
		_data[item.id] = item
		_code_map[item.code] = item

# 创建实例
static func _create(stream: ConfigStream) -> DataCombat_Ability:
	var instance = DataCombat_Ability.new()
	instance.id = stream.read_int32()
	instance.code = stream.read_string_in_pool()
	instance.origin = DataCombat_Origin._create(stream)
	for c in range(stream.read_count(0)):
		instance.machines.append(DataCombat_Machine._create(stream))
	instance.startMachine = stream.read_string_in_pool()
	instance.startPhase = stream.read_string_in_pool()
	return instance


# 解析外键引用
func _resolve(errors: ConfigErrors):
	pass
	for item in machines:
		if item != null:
			item._resolve(errors)

static func _resolve_refs(errors: ConfigErrors):
	for item in all():
		item._resolve(errors)

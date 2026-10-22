class_name DataCombat_Cast
## combat.cast
# 公开属性
var id: int
var abilityCode: String
var origin: DataCombat_Origin
var uses: Array[DataCombat_AbilityUse]
var optional: Array[DataCombat_OptionalAim]
var school: String
var tier: int
var airborne: bool
var bookCode: String
var edition: int
var rewardKey: DataCombat_PhaseKey
# 外键引用属性
var RefBook: DataCombat_BookEntry:
	get:
		if not has_meta(&"_cfggen_RefBook"):
			return null
		var target: WeakRef = get_meta(&"_cfggen_RefBook") as WeakRef
		return target.get_ref() as DataCombat_BookEntry if target != null else null
	set(value):
		if value != null:
			set_meta(&"_cfggen_RefBook", weakref(value))
		elif has_meta(&"_cfggen_RefBook"):
			remove_meta(&"_cfggen_RefBook")
var RefBookByCode: DataCombat_BookEntry:
	get:
		if not has_meta(&"_cfggen_RefBookByCode"):
			return null
		var target: WeakRef = get_meta(&"_cfggen_RefBookByCode") as WeakRef
		return target.get_ref() as DataCombat_BookEntry if target != null else null
	set(value):
		if value != null:
			set_meta(&"_cfggen_RefBookByCode", weakref(value))
		elif has_meta(&"_cfggen_RefBookByCode"):
			remove_meta(&"_cfggen_RefBookByCode")
var RefBookRecord: DataCombat_Phasebook:
	get:
		if not has_meta(&"_cfggen_RefBookRecord"):
			return null
		var target: WeakRef = get_meta(&"_cfggen_RefBookRecord") as WeakRef
		return target.get_ref() as DataCombat_Phasebook if target != null else null
	set(value):
		if value != null:
			set_meta(&"_cfggen_RefBookRecord", weakref(value))
		elif has_meta(&"_cfggen_RefBookRecord"):
			remove_meta(&"_cfggen_RefBookRecord")
var RefReward: DataCombat_Reward:
	get:
		if not has_meta(&"_cfggen_RefReward"):
			return null
		var target: WeakRef = get_meta(&"_cfggen_RefReward") as WeakRef
		return target.get_ref() as DataCombat_Reward if target != null else null
	set(value):
		if value != null:
			set_meta(&"_cfggen_RefReward", weakref(value))
		elif has_meta(&"_cfggen_RefReward"):
			remove_meta(&"_cfggen_RefReward")

# 内部存储
static var _data: Dictionary[int, DataCombat_Cast] = {}
# 主键查询
static func find(key_0: int) -> DataCombat_Cast:
	return _data.get(key_0)
# 获取所有数据
static func all() -> Array[DataCombat_Cast]:
	return _data.values()

# 字符串表示
func _to_string() -> String:
	return "DataCombat_Cast{" + str(id) + "," + abilityCode + "," + str(origin) + "," + str(uses) + "," + str(optional) + "," + school + "," + str(tier) + "," + str(airborne) + "," + bookCode + "," + str(edition) + "," + str(rewardKey) + "}"

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	_data = {}
	var count = stream.read_count(1)
	for i in range(count):
		var item = _create(stream)
		_data[item.id] = item

# 创建实例
static func _create(stream: ConfigStream) -> DataCombat_Cast:
	var instance = DataCombat_Cast.new()
	instance.id = stream.read_int32()
	instance.abilityCode = stream.read_string_in_pool()
	instance.origin = DataCombat_Origin._create(stream)
	for c in range(stream.read_count(0)):
		instance.uses.append(DataCombat_AbilityUse._create(stream))
	for c in range(stream.read_count(0)):
		instance.optional.append(DataCombat_OptionalAim._create(stream))
	instance.school = stream.read_string_in_pool()
	instance.tier = stream.read_int32()
	instance.airborne = stream.read_bool()
	instance.bookCode = stream.read_string_in_pool()
	instance.edition = stream.read_int32()
	instance.rewardKey = DataCombat_PhaseKey._create(stream)
	return instance


# 解析外键引用
func _resolve(errors: ConfigErrors):
	pass
	for item in uses:
		if item != null:
			item._resolve(errors)
	for item in optional:
		if item != null:
			item._resolve(errors)

static func _resolve_refs(errors: ConfigErrors):
	for item in all():
		item._resolve(errors)

class_name DataCombat_OptionalAim
## combat.OptionalAim
# 公开属性
var abilityId: int
var machineKey: String
var phaseKey: String
# 外键引用属性
var NullableRefTarget: DataCombat_Phase:
	get:
		if not has_meta(&"_cfggen_NullableRefTarget"):
			return null
		var target: WeakRef = get_meta(&"_cfggen_NullableRefTarget") as WeakRef
		return target.get_ref() as DataCombat_Phase if target != null else null
	set(value):
		if value != null:
			set_meta(&"_cfggen_NullableRefTarget", weakref(value))
		elif has_meta(&"_cfggen_NullableRefTarget"):
			remove_meta(&"_cfggen_NullableRefTarget")

# 字符串表示
func _to_string() -> String:
	return "DataCombat_OptionalAim{" + str(abilityId) + "," + machineKey + "," + phaseKey + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataCombat_OptionalAim:
	var instance = DataCombat_OptionalAim.new()
	instance.abilityId = stream.read_int32()
	instance.machineKey = stream.read_string_in_pool()
	instance.phaseKey = stream.read_string_in_pool()
	return instance


# 解析外键引用
func _resolve(errors: ConfigErrors):
	pass


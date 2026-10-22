class_name DataCombat_PhaseUse
## combat.PhaseUse
# 公开属性
var phaseKey: String
# 外键引用属性
var RefTarget: DataCombat_Phase:
	get:
		if not has_meta(&"_cfggen_RefTarget"):
			return null
		var target: WeakRef = get_meta(&"_cfggen_RefTarget") as WeakRef
		return target.get_ref() as DataCombat_Phase if target != null else null
	set(value):
		if value != null:
			set_meta(&"_cfggen_RefTarget", weakref(value))
		elif has_meta(&"_cfggen_RefTarget"):
			remove_meta(&"_cfggen_RefTarget")
var RefByCode: DataCombat_Phase:
	get:
		if not has_meta(&"_cfggen_RefByCode"):
			return null
		var target: WeakRef = get_meta(&"_cfggen_RefByCode") as WeakRef
		return target.get_ref() as DataCombat_Phase if target != null else null
	set(value):
		if value != null:
			set_meta(&"_cfggen_RefByCode", weakref(value))
		elif has_meta(&"_cfggen_RefByCode"):
			remove_meta(&"_cfggen_RefByCode")
var RefOrigin: DataCombat_Origin:
	get:
		if not has_meta(&"_cfggen_RefOrigin"):
			return null
		var target: WeakRef = get_meta(&"_cfggen_RefOrigin") as WeakRef
		return target.get_ref() as DataCombat_Origin if target != null else null
	set(value):
		if value != null:
			set_meta(&"_cfggen_RefOrigin", weakref(value))
		elif has_meta(&"_cfggen_RefOrigin"):
			remove_meta(&"_cfggen_RefOrigin")

# 字符串表示
func _to_string() -> String:
	return "DataCombat_PhaseUse{" + phaseKey + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataCombat_PhaseUse:
	var instance = DataCombat_PhaseUse.new()
	instance.phaseKey = stream.read_string_in_pool()
	return instance


# 解析外键引用
func _resolve(errors: ConfigErrors):
	pass


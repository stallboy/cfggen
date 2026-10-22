class_name DataCombat_Phase
## combat.Phase
# 公开属性
var key: String
var next: String
# 外键引用属性
var NullableRefNext: DataCombat_Phase:
	get:
		if not has_meta(&"_cfggen_NullableRefNext"):
			return null
		var target: WeakRef = get_meta(&"_cfggen_NullableRefNext") as WeakRef
		return target.get_ref() as DataCombat_Phase if target != null else null
	set(value):
		if value != null:
			set_meta(&"_cfggen_NullableRefNext", weakref(value))
		elif has_meta(&"_cfggen_NullableRefNext"):
			remove_meta(&"_cfggen_NullableRefNext")
var RefExit: DataCombat_Effect:
	get:
		if not has_meta(&"_cfggen_RefExit"):
			return null
		var target: WeakRef = get_meta(&"_cfggen_RefExit") as WeakRef
		return target.get_ref() as DataCombat_Effect if target != null else null
	set(value):
		if value != null:
			set_meta(&"_cfggen_RefExit", weakref(value))
		elif has_meta(&"_cfggen_RefExit"):
			remove_meta(&"_cfggen_RefExit")

# 字符串表示
func _to_string() -> String:
	return "DataCombat_Phase{" + key + "," + next + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataCombat_Phase:
	var instance = DataCombat_Phase.new()
	instance.key = stream.read_string_in_pool()
	instance.next = stream.read_string_in_pool()
	return instance


# 解析外键引用
func _resolve(errors: ConfigErrors):
	pass


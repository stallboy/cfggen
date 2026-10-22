class_name DataCombatCommand_Jump extends DataCombat_Command
## Jump
# 公开属性
var key: String
# 外键引用属性
var NullableRefKey: DataCombat_NestedPhase:
	get:
		if not has_meta(&"_cfggen_NullableRefKey"):
			return null
		var target: WeakRef = get_meta(&"_cfggen_NullableRefKey") as WeakRef
		return target.get_ref() as DataCombat_NestedPhase if target != null else null
	set(value):
		if value != null:
			set_meta(&"_cfggen_NullableRefKey", weakref(value))
		elif has_meta(&"_cfggen_NullableRefKey"):
			remove_meta(&"_cfggen_NullableRefKey")

# 字符串表示
func _to_string() -> String:
	return "DataCombatCommand_Jump{" + key + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataCombatCommand_Jump:
	var instance = DataCombatCommand_Jump.new()
	instance.key = stream.read_string_in_pool()
	return instance


# 解析外键引用
func _resolve(errors: ConfigErrors):
	pass


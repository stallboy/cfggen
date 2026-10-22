class_name DataCombat_AbilityUse
## combat.AbilityUse
# 公开属性
var binding: DataCombat_Binding
var machines: Array[DataCombat_MachineUse]
# 外键引用属性
var RefAbility: DataCombat_Ability:
	get:
		if not has_meta(&"_cfggen_RefAbility"):
			return null
		var target: WeakRef = get_meta(&"_cfggen_RefAbility") as WeakRef
		return target.get_ref() as DataCombat_Ability if target != null else null
	set(value):
		if value != null:
			set_meta(&"_cfggen_RefAbility", weakref(value))
		elif has_meta(&"_cfggen_RefAbility"):
			remove_meta(&"_cfggen_RefAbility")

# 字符串表示
func _to_string() -> String:
	return "DataCombat_AbilityUse{" + str(binding) + "," + str(machines) + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataCombat_AbilityUse:
	var instance = DataCombat_AbilityUse.new()
	instance.binding = DataCombat_Binding._create(stream)
	for c in range(stream.read_count(0)):
		instance.machines.append(DataCombat_MachineUse._create(stream))
	return instance


# 解析外键引用
func _resolve(errors: ConfigErrors):
	pass
	for item in machines:
		if item != null:
			item._resolve(errors)


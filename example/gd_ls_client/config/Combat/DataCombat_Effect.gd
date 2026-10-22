class_name DataCombat_Effect

## combat.Effect
# 创建实例（静态工厂方法）
static func _create(stream: ConfigStream) -> DataCombat_Effect:
	var type_name = stream.read_string_in_pool()
	match type_name:
		"Damage":
			return DataCombatEffect_Damage._create(stream)
		"Heal":
			return DataCombatEffect_Heal._create(stream)
		"None":
			return DataCombatEffect_None._create(stream)
	stream.fail("Unknown type: " + type_name)
	return null

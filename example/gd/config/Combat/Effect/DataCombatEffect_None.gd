class_name DataCombatEffect_None extends DataCombat_Effect
## None
# 公开属性

# 字符串表示
func _to_string() -> String:
	return "DataCombatEffect_None{}"

# 创建实例
static func _create(stream: ConfigStream) -> DataCombatEffect_None:
	var instance = DataCombatEffect_None.new()
	return instance



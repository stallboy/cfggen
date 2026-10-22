class_name DataCombatEffect_Heal extends DataCombat_Effect
## Heal
# 公开属性
var amount: int

# 字符串表示
func _to_string() -> String:
	return "DataCombatEffect_Heal{" + str(amount) + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataCombatEffect_Heal:
	var instance = DataCombatEffect_Heal.new()
	instance.amount = stream.read_int32()
	return instance



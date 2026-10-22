class_name DataCombatEffect_Damage extends DataCombat_Effect
## Damage
# 公开属性
var amount: int

# 字符串表示
func _to_string() -> String:
	return "DataCombatEffect_Damage{" + str(amount) + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataCombatEffect_Damage:
	var instance = DataCombatEffect_Damage.new()
	instance.amount = stream.read_int32()
	return instance



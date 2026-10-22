class_name DataCombat_Combo
## combat.Combo
# 公开属性
var weapon: String
var tier: int
var airborne: bool
var effect: DataCombat_Effect

# 字符串表示
func _to_string() -> String:
	return "DataCombat_Combo{" + weapon + "," + str(tier) + "," + str(airborne) + "," + str(effect) + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataCombat_Combo:
	var instance = DataCombat_Combo.new()
	instance.weapon = stream.read_string_in_pool()
	instance.tier = stream.read_int32()
	instance.airborne = stream.read_bool()
	instance.effect = DataCombat_Effect._create(stream)
	return instance



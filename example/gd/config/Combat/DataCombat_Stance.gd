class_name DataCombat_Stance
## combat.Stance
# 公开属性
var airborne: bool
var effect: DataCombat_Effect

# 字符串表示
func _to_string() -> String:
	return "DataCombat_Stance{" + str(airborne) + "," + str(effect) + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataCombat_Stance:
	var instance = DataCombat_Stance.new()
	instance.airborne = stream.read_bool()
	instance.effect = DataCombat_Effect._create(stream)
	return instance



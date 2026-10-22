class_name DataCombat_PhaseVariant
## combat.PhaseVariant
# 公开属性
var key: DataCombat_PhaseKey
var effect: DataCombat_Effect

# 字符串表示
func _to_string() -> String:
	return "DataCombat_PhaseVariant{" + str(key) + "," + str(effect) + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataCombat_PhaseVariant:
	var instance = DataCombat_PhaseVariant.new()
	instance.key = DataCombat_PhaseKey._create(stream)
	instance.effect = DataCombat_Effect._create(stream)
	return instance



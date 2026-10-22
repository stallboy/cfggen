class_name DataCombat_Exit
## combat.Exit
# 公开属性
var effect: DataCombat_Effect

# 字符串表示
func _to_string() -> String:
	return "DataCombat_Exit{" + str(effect) + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataCombat_Exit:
	var instance = DataCombat_Exit.new()
	instance.effect = DataCombat_Effect._create(stream)
	return instance



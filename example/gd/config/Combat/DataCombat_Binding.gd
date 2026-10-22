class_name DataCombat_Binding
## combat.Binding
# 公开属性
var abilityId: int

# 字符串表示
func _to_string() -> String:
	return "DataCombat_Binding{" + str(abilityId) + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataCombat_Binding:
	var instance = DataCombat_Binding.new()
	instance.abilityId = stream.read_int32()
	return instance



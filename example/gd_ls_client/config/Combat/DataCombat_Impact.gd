class_name DataCombat_Impact
## combat.Impact
# 公开属性
var ordinal: int
var damage: int

# 字符串表示
func _to_string() -> String:
	return "DataCombat_Impact{" + str(ordinal) + "," + str(damage) + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataCombat_Impact:
	var instance = DataCombat_Impact.new()
	instance.ordinal = stream.read_int32()
	instance.damage = stream.read_int32()
	return instance



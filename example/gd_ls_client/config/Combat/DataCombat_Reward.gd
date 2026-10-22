class_name DataCombat_Reward
## combat.Reward
# 公开属性
var count: int

# 字符串表示
func _to_string() -> String:
	return "DataCombat_Reward{" + str(count) + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataCombat_Reward:
	var instance = DataCombat_Reward.new()
	instance.count = stream.read_int32()
	return instance



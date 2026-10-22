class_name DataCombat_Frame
## combat.Frame
# 公开属性
var tick: int
var effect: DataCombat_Effect

# 字符串表示
func _to_string() -> String:
	return "DataCombat_Frame{" + str(tick) + "," + str(effect) + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataCombat_Frame:
	var instance = DataCombat_Frame.new()
	instance.tick = stream.read_int64()
	instance.effect = DataCombat_Effect._create(stream)
	return instance



class_name DataAiTriggertick_ByLevel extends DataAi_TriggerTick
## ByLevel
# 公开属性
var init: int
var coefficient: float

# 字符串表示
func _to_string() -> String:
	return "DataAiTriggertick_ByLevel{" + str(init) + "," + str(coefficient) + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataAiTriggertick_ByLevel:
	var instance = DataAiTriggertick_ByLevel.new()
	instance.init = stream.read_int32()
	instance.coefficient = stream.read_float()
	return instance



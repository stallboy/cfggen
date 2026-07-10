class_name DataAiTriggertick_ByServerUpDay extends DataAi_TriggerTick
## ByServerUpDay
# 公开属性
var init: int
var coefficient1: float
var coefficient2: float

# 字符串表示
func _to_string() -> String:
	return "DataAiTriggertick_ByServerUpDay{" + str(init) + "," + str(coefficient1) + "," + str(coefficient2) + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataAiTriggertick_ByServerUpDay:
	var instance = DataAiTriggertick_ByServerUpDay.new()
	instance.init = stream.read_int32()
	instance.coefficient1 = stream.read_float()
	instance.coefficient2 = stream.read_float()
	return instance



class_name DataAiTriggertick_Byserverupday extends DataAi_Triggertick
## ByServerUpDay
# 公开属性
var init: int
var coefficient1: float
var coefficient2: float

# 字符串表示
func _to_string() -> String:
	return "DataAiTriggertick_Byserverupday{" + str(init) + "," + str(coefficient1) + "," + str(coefficient2) + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataAiTriggertick_Byserverupday:
	var instance = DataAiTriggertick_Byserverupday.new()
	instance.init = stream.read_int32()
	instance.coefficient1 = stream.read_float()
	instance.coefficient2 = stream.read_float()
	return instance



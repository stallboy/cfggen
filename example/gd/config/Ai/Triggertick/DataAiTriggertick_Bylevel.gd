class_name DataAiTriggertick_Bylevel extends DataAi_Triggertick
## ByLevel
# 公开属性
var init: int
var coefficient: float

# 字符串表示
func _to_string() -> String:
	return "DataAiTriggertick_Bylevel{" + str(init) + "," + str(coefficient) + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataAiTriggertick_Bylevel:
	var instance = DataAiTriggertick_Bylevel.new()
	instance.init = stream.read_int32()
	instance.coefficient = stream.read_float()
	return instance



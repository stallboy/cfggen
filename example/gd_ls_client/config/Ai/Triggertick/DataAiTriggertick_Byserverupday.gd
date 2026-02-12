class_name DataAiTriggertick_Byserverupday extends DataAi_Triggertick
## ByServerUpDay
# 公开属性
var init: int:
	get:
		return init
var coefficient1: float:
	get:
		return coefficient1
var coefficient2: float:
	get:
		return coefficient2
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataAiTriggertick_Byserverupday:
	var instance = DataAiTriggertick_Byserverupday.new()
	instance.init = stream.get_32()
	instance.coefficient1 = stream.get_float()
	instance.coefficient2 = stream.get_float()
	return instance

# 解析外键引用

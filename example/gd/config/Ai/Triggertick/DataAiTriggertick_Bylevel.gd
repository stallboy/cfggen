class_name DataAiTriggertick_Bylevel extends DataAi_Triggertick
## ByLevel
# 公开属性
var init: int:
	get:
		return init
var coefficient: float:
	get:
		return coefficient
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataAiTriggertick_Bylevel:
	var instance = DataAiTriggertick_Bylevel.new()
	instance.init = stream.get_32()
	instance.coefficient = stream.get_float()
	return instance

# 解析外键引用

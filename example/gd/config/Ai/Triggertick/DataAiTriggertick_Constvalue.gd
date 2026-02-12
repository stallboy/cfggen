class_name DataAiTriggertick_Constvalue extends DataAi_Triggertick
## ConstValue
# 公开属性
var value: int
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataAiTriggertick_Constvalue:
	var instance = DataAiTriggertick_Constvalue.new()
	instance.value = stream.read_int32()
	return instance

# 解析外键引用

class_name DataAiTriggertick_Constvalue extends DataAi_Triggertick
## ConstValue
# 公开属性
var value: int

# 字符串表示
func _to_string() -> String:
	return "DataAiTriggertick_Constvalue{" + str(value) + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataAiTriggertick_Constvalue:
	var instance = DataAiTriggertick_Constvalue.new()
	instance.value = stream.read_int32()
	return instance



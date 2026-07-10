class_name DataAiTriggertick_ConstValue extends DataAi_TriggerTick
## ConstValue
# 公开属性
var value: int

# 字符串表示
func _to_string() -> String:
	return "DataAiTriggertick_ConstValue{" + str(value) + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataAiTriggertick_ConstValue:
	var instance = DataAiTriggertick_ConstValue.new()
	instance.value = stream.read_int32()
	return instance



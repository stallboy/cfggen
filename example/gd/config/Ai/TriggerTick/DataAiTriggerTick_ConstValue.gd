class_name DataAiTriggerTick_ConstValue extends DataAi_TriggerTick
## ConstValue
# 公开属性
var value: int

# 字符串表示
func _to_string() -> String:
	return "DataAiTriggerTick_ConstValue{" + str(value) + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataAiTriggerTick_ConstValue:
	var instance = DataAiTriggerTick_ConstValue.new()
	instance.value = stream.read_int32()
	return instance



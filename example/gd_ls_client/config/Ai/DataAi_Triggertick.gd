class_name DataAi_TriggerTick

## ai.TriggerTick
# 创建实例（静态工厂方法）
static func _create(stream: ConfigStream) -> DataAi_TriggerTick:
	var type_name = stream.read_string_in_pool()
	match type_name:
		"ConstValue":
			return DataAiTriggertick_ConstValue._create(stream)
		"ByLevel":
			return DataAiTriggertick_ByLevel._create(stream)
		"ByServerUpDay":
			return DataAiTriggertick_ByServerUpDay._create(stream)
		_:
			push_error("Unknown type: " + type_name)
			return null

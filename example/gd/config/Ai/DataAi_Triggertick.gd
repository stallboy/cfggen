class_name DataAi_Triggertick

## ai.TriggerTick
# 创建实例（静态工厂方法）
static func _create(stream: ConfigStream) -> DataAi_Triggertick:
	var type_name = stream.read_string_in_pool()
	match type_name:
		"ConstValue":
			return DataAiTriggertick_Constvalue._create(stream)
		"ByLevel":
			return DataAiTriggertick_Bylevel._create(stream)
		"ByServerUpDay":
			return DataAiTriggertick_Byserverupday._create(stream)
		_:
			push_error("Unknown type: " + type_name)
			return null

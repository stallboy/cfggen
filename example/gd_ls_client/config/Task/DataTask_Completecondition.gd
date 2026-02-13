class_name DataTask_Completecondition

## task.completecondition
# 获取接口类型
func get_type():
	push_error("Must be implemented by subclass")
	return null
func _resolve(errors: ConfigErrors):
	pass
# 创建实例（静态工厂方法）
static func _create(stream: ConfigStream) -> DataTask_Completecondition:
	var type_name = stream.read_string_in_pool()
	match type_name:
		"KillMonster":
			return DataTaskCompletecondition_Killmonster._create(stream)
		"TalkNpc":
			return DataTaskCompletecondition_Talknpc._create(stream)
		"TestNoColumn":
			return DataTaskCompletecondition_Testnocolumn._create(stream)
		"Chat":
			return DataTaskCompletecondition_Chat._create(stream)
		"ConditionAnd":
			return DataTaskCompletecondition_Conditionand._create(stream)
		"CollectItem":
			return DataTaskCompletecondition_Collectitem._create(stream)
		"aa":
			return DataTaskCompletecondition_Aa._create(stream)
		_:
			push_error("Unknown type: " + type_name)
			return null

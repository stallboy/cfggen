class_name DataTask_Completecondition

## task.completecondition
# 获取接口类型
func get_type():
	push_error("Must be implemented by subclass")
	return null
func _resolve(errors: ConfigErrors):
	pass
# 创建实例（静态工厂方法）
static func create(stream: ConfigStream) -> DataTask_Completecondition:
	var type_name = stream.get_string()
	match type_name:
		"KillMonster":
			return DataTaskCompletecondition_Killmonster.create(stream)
		"TalkNpc":
			return DataTaskCompletecondition_Talknpc.create(stream)
		"TestNoColumn":
			return DataTaskCompletecondition_Testnocolumn.create(stream)
		"Chat":
			return DataTaskCompletecondition_Chat.create(stream)
		"ConditionAnd":
			return DataTaskCompletecondition_Conditionand.create(stream)
		"CollectItem":
			return DataTaskCompletecondition_Collectitem.create(stream)
		"aa":
			return DataTaskCompletecondition_Aa.create(stream)
		_:
			push_error("Unknown type: " + type_name)
			return null

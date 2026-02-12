class_name DataTaskCompletecondition_Conditionand extends DataTask_Completecondition
## ConditionAnd
# 公开属性
var cond1: DataTask_Completecondition
var cond2: DataTask_Completecondition
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataTaskCompletecondition_Conditionand:
	var instance = DataTaskCompletecondition_Conditionand.new()
	instance.cond1 = DataTask_Completecondition.create(stream)
	instance.cond2 = DataTask_Completecondition.create(stream)
	return instance

# 解析外键引用
func _resolve(errors: ConfigErrors):
	if cond1 != null:
		cond1._resolve(errors)
	if cond2 != null:
		cond2._resolve(errors)

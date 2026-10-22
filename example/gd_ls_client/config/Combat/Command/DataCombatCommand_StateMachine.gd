class_name DataCombatCommand_StateMachine extends DataCombat_Command
## StateMachine
# 公开属性
var phases: Array[DataCombat_NestedPhase]

# 字符串表示
func _to_string() -> String:
	return "DataCombatCommand_StateMachine{" + str(phases) + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataCombatCommand_StateMachine:
	var instance = DataCombatCommand_StateMachine.new()
	for c in range(stream.read_count(0)):
		instance.phases.append(DataCombat_NestedPhase._create(stream))
	return instance


# 解析外键引用
func _resolve(errors: ConfigErrors):
	pass
	for item in phases:
		if item != null:
			item._resolve(errors)


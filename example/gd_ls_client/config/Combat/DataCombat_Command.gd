class_name DataCombat_Command

## combat.Command
## 递归行为树：最近的内层状态机缺少 Done 时，Jump 不回退到外层。
func _resolve(errors: ConfigErrors):
	pass
# 创建实例（静态工厂方法）
static func _create(stream: ConfigStream) -> DataCombat_Command:
	var type_name = stream.read_string_in_pool()
	match type_name:
		"StateMachine":
			return DataCombatCommand_StateMachine._create(stream)
		"Jump":
			return DataCombatCommand_Jump._create(stream)
		"Noop":
			return DataCombatCommand_Noop._create(stream)
	stream.fail("Unknown type: " + type_name)
	return null

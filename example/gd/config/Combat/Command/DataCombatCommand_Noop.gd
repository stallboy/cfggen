class_name DataCombatCommand_Noop extends DataCombat_Command
## Noop
# 公开属性

# 字符串表示
func _to_string() -> String:
	return "DataCombatCommand_Noop{}"

# 创建实例
static func _create(stream: ConfigStream) -> DataCombatCommand_Noop:
	var instance = DataCombatCommand_Noop.new()
	return instance



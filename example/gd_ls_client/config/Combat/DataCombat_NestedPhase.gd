class_name DataCombat_NestedPhase
## combat.NestedPhase
# 公开属性
var key: String
var action: DataCombat_Command

# 字符串表示
func _to_string() -> String:
	return "DataCombat_NestedPhase{" + key + "," + str(action) + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataCombat_NestedPhase:
	var instance = DataCombat_NestedPhase.new()
	instance.key = stream.read_string_in_pool()
	instance.action = DataCombat_Command._create(stream)
	return instance


# 解析外键引用
func _resolve(errors: ConfigErrors):
	pass
	if action != null:
		action._resolve(errors)


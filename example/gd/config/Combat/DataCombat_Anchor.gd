class_name DataCombat_Anchor
## combat.Anchor
# 公开属性

# 字符串表示
func _to_string() -> String:
	return "DataCombat_Anchor{}"

# 创建实例
static func _create(stream: ConfigStream) -> DataCombat_Anchor:
	var instance = DataCombat_Anchor.new()
	return instance



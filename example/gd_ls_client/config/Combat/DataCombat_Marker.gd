class_name DataCombat_Marker
## combat.Marker
# 公开属性
var key: String

# 字符串表示
func _to_string() -> String:
	return "DataCombat_Marker{" + key + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataCombat_Marker:
	var instance = DataCombat_Marker.new()
	instance.key = stream.read_string_in_pool()
	return instance



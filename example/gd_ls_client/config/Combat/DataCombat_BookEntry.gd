class_name DataCombat_BookEntry
## combat.BookEntry
# 公开属性
var name: String

# 字符串表示
func _to_string() -> String:
	return "DataCombat_BookEntry{" + name + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataCombat_BookEntry:
	var instance = DataCombat_BookEntry.new()
	instance.name = stream.read_string_in_pool()
	return instance



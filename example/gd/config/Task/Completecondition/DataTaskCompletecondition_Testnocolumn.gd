class_name DataTaskCompletecondition_Testnocolumn extends DataTask_Completecondition
## TestNoColumn
# 公开属性

# 字符串表示
func _to_string() -> String:
	return "DataTaskCompletecondition_Testnocolumn{}"

# 创建实例
static func _create(stream: ConfigStream) -> DataTaskCompletecondition_Testnocolumn:
	var instance = DataTaskCompletecondition_Testnocolumn.new()
	return instance

class_name DataTaskCompletecondition_Chat extends DataTask_Completecondition
## Chat
# 公开属性
var msg: String
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataTaskCompletecondition_Chat:
	var instance = DataTaskCompletecondition_Chat.new()
	instance.msg = stream.read_string_in_pool()
	return instance

# 解析外键引用
# 字符串表示
func _to_string() -> String:
	return "DataTaskCompletecondition_Chat{" + msg + "}"

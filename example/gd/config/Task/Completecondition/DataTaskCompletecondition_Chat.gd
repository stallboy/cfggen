class_name DataTaskCompletecondition_Chat extends DataTask_Completecondition
## Chat
# 公开属性
var msg: String:
	get:
		return msg
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataTaskCompletecondition_Chat:
	var instance = DataTaskCompletecondition_Chat.new()
	instance.msg = stream.get_string()
	return instance

# 解析外键引用

class_name DataTaskCompletecondition_Talknpc extends DataTask_Completecondition
## TalkNpc
# 公开属性
var npcid: int
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataTaskCompletecondition_Talknpc:
	var instance = DataTaskCompletecondition_Talknpc.new()
	instance.npcid = stream.read_int32()
	return instance

# 解析外键引用

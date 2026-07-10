class_name DataTaskCompletecondition_TalkNpc extends DataTask_Completecondition
## TalkNpc
# 公开属性
var npcid: int

# 字符串表示
func _to_string() -> String:
	return "DataTaskCompletecondition_TalkNpc{" + str(npcid) + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataTaskCompletecondition_TalkNpc:
	var instance = DataTaskCompletecondition_TalkNpc.new()
	instance.npcid = stream.read_int32()
	return instance



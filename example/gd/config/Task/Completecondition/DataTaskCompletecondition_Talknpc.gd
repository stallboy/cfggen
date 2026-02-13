class_name DataTaskCompletecondition_Talknpc extends DataTask_Completecondition
## TalkNpc
# 公开属性
var npcid: int

# 字符串表示
func _to_string() -> String:
	return "DataTaskCompletecondition_Talknpc{" + str(npcid) + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataTaskCompletecondition_Talknpc:
	var instance = DataTaskCompletecondition_Talknpc.new()
	instance.npcid = stream.read_int32()
	return instance



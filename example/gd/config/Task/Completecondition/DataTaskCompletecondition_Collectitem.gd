class_name DataTaskCompletecondition_Collectitem extends DataTask_Completecondition
## CollectItem
# 公开属性
var itemid: int
var count: int
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataTaskCompletecondition_Collectitem:
	var instance = DataTaskCompletecondition_Collectitem.new()
	instance.itemid = stream.read_int32()
	instance.count = stream.read_int32()
	return instance

# 解析外键引用
# 字符串表示
func _to_string() -> String:
	return "DataTaskCompletecondition_Collectitem{" + str(itemid) + "," + str(count) + "}"

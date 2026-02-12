class_name DataTaskCompletecondition_Collectitem extends DataTask_Completecondition
## CollectItem
# 公开属性
var itemid: int:
	get:
		return itemid
var count: int:
	get:
		return count
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataTaskCompletecondition_Collectitem:
	var instance = DataTaskCompletecondition_Collectitem.new()
	instance.itemid = stream.get_32()
	instance.count = stream.get_32()
	return instance

# 解析外键引用

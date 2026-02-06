class_name DataAi_Ai_action
## ai.ai_action
# 公开属性
var iD: int:
	get:
		return iD
var desc: String:
	get:
		return desc  # 描述
var formulaID: int:
	get:
		return formulaID  # 公式
var argIList: Array[int]:
	get:
		return argIList  # 参数(int)1
var argSList: Array[int]:
	get:
		return argSList  # 参数(string)1
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataAi_Ai_action:
	var instance = DataAi_Ai_action.new()
	instance.iD = stream.get_32()
	instance.desc = stream.get_string()
	instance.formulaID = stream.get_32()
	instance.argIList = []
	for c in range(stream.get_32()):
		instance.argIList.append(stream.get_32())
	instance.argSList = []
	for c in range(stream.get_32()):
		instance.argSList.append(stream.get_32())
	return instance

# 主键查询
static func find(id: int) -> DataAi_Ai_action:
	return _data.get(id)

# 获取所有数据
static func all() -> Array[DataAi_Ai_action]:
	return _data.values()

# 从流初始化
static func _init_from_stream(stream: ConfigStream, err: ConfigErrors):
	var count = stream.get_32()
	for i in range(count):
		var item = create(stream)
		_data[item.iD] = item
# 内部存储
static var _data: Dictionary[int, DataAi_Ai_action] = {}
# 解析外键引用

class_name DataAi_Ai_action
## ai.ai_action
# 公开属性
var iD: int
var desc: String  # 描述
var formulaID: int  # 公式
var argIList: Array[int]  # 参数(int)1
var argSList: Array[int]  # 参数(string)1
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataAi_Ai_action:
	var instance = DataAi_Ai_action.new()
	instance.iD = stream.read_int32()
	instance.desc = stream.read_string_in_pool()
	instance.formulaID = stream.read_int32()
	for c in range(stream.read_int32()):
		instance.argIList.append(stream.read_int32())
	for c in range(stream.read_int32()):
		instance.argSList.append(stream.read_int32())
	return instance

# 主键查询
static func find(id: int) -> DataAi_Ai_action:
	return _data.get(id)

# 获取所有数据
static func all() -> Array[DataAi_Ai_action]:
	return _data.values()

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	var count = stream.read_int32()
	for i in range(count):
		var item = create(stream)
		_data[item.iD] = item
# 内部存储
static var _data: Dictionary[int, DataAi_Ai_action] = {}
# 解析外键引用

class_name DataAi_Ai_condition
## ai.ai_condition
# 公开属性
var iD: int
var desc: String  # 描述
var formulaID: int  # 公式
var argIList: Array[int]  # 参数(int)1
var argSList: Array[int]  # 参数(string)1

# 内部存储
static var _data: Dictionary[int, DataAi_Ai_condition] = {}
# 主键查询
static func find(id: int) -> DataAi_Ai_condition:
	return _data.get(id)
# 获取所有数据
static func all() -> Array[DataAi_Ai_condition]:
	return _data.values()

# 字符串表示
func _to_string() -> String:
	return "DataAi_Ai_condition{" + str(iD) + "," + desc + "," + str(formulaID) + "," + str(argIList) + "," + str(argSList) + "}"

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	var count = stream.read_int32()
	for i in range(count):
		var item = _create(stream)
		_data[item.iD] = item

# 创建实例
static func _create(stream: ConfigStream) -> DataAi_Ai_condition:
	var instance = DataAi_Ai_condition.new()
	instance.iD = stream.read_int32()
	instance.desc = stream.read_string_in_pool()
	instance.formulaID = stream.read_int32()
	for c in range(stream.read_int32()):
		instance.argIList.append(stream.read_int32())
	for c in range(stream.read_int32()):
		instance.argSList.append(stream.read_int32())
	return instance



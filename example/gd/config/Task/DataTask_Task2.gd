class_name DataTask_Task2
## task.task2
# 公开属性
var taskid: int  # 任务完成条件类型（id的范围为1-100）
var name: Array[String]
var nexttask: int
var completecondition: DataTask_Completecondition
var exp: int
var testBool: bool
var testString: String
var testStruct: DataPosition
var testList: Array[int]
var testListStruct: Array[DataPosition]
var testListInterface: Array[DataAi_Triggertick]
# 外键引用属性
var NullableRefTaskid: DataTask_Taskextraexp
var NullableRefNexttask: DataTask_Task

# 内部存储
static var _data: Dictionary[int, DataTask_Task2] = {}
# 主键查询
static func find(id: int) -> DataTask_Task2:
	return _data.get(id)
# 获取所有数据
static func all() -> Array[DataTask_Task2]:
	return _data.values()

# 字符串表示
func _to_string() -> String:
	return "DataTask_Task2{" + str(taskid) + "," + str(name) + "," + str(nexttask) + "," + str(completecondition) + "," + str(exp) + "," + str(testBool) + "," + testString + "," + str(testStruct) + "," + str(testList) + "," + str(testListStruct) + "," + str(testListInterface) + "}"

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	var count = stream.read_int32()
	for i in range(count):
		var item = _create(stream)
		_data[item.taskid] = item

# 创建实例
static func _create(stream: ConfigStream) -> DataTask_Task2:
	var instance = DataTask_Task2.new()
	instance.taskid = stream.read_int32()
	for c in range(stream.read_int32()):
		instance.name.append(stream.read_text_in_pool())
	instance.nexttask = stream.read_int32()
	instance.completecondition = DataTask_Completecondition._create(stream)
	instance.exp = stream.read_int32()
	instance.testBool = stream.read_bool()
	instance.testString = stream.read_string_in_pool()
	instance.testStruct = DataPosition._create(stream)
	for c in range(stream.read_int32()):
		instance.testList.append(stream.read_int32())
	for c in range(stream.read_int32()):
		instance.testListStruct.append(DataPosition._create(stream))
	for c in range(stream.read_int32()):
		instance.testListInterface.append(DataAi_Triggertick._create(stream))
	return instance


# 解析外键引用
func _resolve(errors: ConfigErrors):
	if completecondition != null:
		completecondition._resolve(errors)
	NullableRefTaskid = DataTask_Taskextraexp.find(taskid)
	NullableRefNexttask = DataTask_Task.find(nexttask)

static func _resolve_refs(errors: ConfigErrors):
	for item in all():
		item._resolve(errors)

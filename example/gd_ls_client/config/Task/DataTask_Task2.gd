class_name DataTask_Task2
## task.task2
# 公开属性
var taskid: int  # 任务完成条件类型（id的范围为1-100）
var name: Array[ConfigText]
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
# 创建实例
static func create(stream: ConfigStream) -> DataTask_Task2:
	var instance = DataTask_Task2.new()
	instance.taskid = stream.read_int32()
	for c in range(stream.read_int32()):
		instance.name.append(ConfigText.create(stream))
	instance.nexttask = stream.read_int32()
	instance.completecondition = DataTask_Completecondition.create(stream)
	instance.exp = stream.read_int32()
	instance.testBool = stream.read_bool()
	instance.testString = stream.read_string_in_pool()
	instance.testStruct = DataPosition.create(stream)
	for c in range(stream.read_int32()):
		instance.testList.append(stream.read_int32())
	for c in range(stream.read_int32()):
		instance.testListStruct.append(DataPosition.create(stream))
	for c in range(stream.read_int32()):
		instance.testListInterface.append(DataAi_Triggertick.create(stream))
	return instance

# 主键查询
static func find(id: int) -> DataTask_Task2:
	return _data.get(id)

# 获取所有数据
static func all() -> Array[DataTask_Task2]:
	return _data.values()

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	var count = stream.read_int32()
	for i in range(count):
		var item = create(stream)
		_data[item.taskid] = item
# 内部存储
static var _data: Dictionary[int, DataTask_Task2] = {}
# 解析外键引用
func _resolve(errors: ConfigErrors):
	if completecondition != null:
		completecondition._resolve(errors)
	NullableRefTaskid = DataTask_Taskextraexp.find(taskid)
	NullableRefNexttask = DataTask_Task.find(nexttask)
static func _resolve_refs(errors: ConfigErrors):
	for item in all():
		item._resolve(errors)
# 字符串表示
func _to_string() -> String:
	return "DataTask_Task2{" + str(taskid) + "," + str(name) + "," + str(nexttask) + "," + str(completecondition) + "," + str(exp) + "," + str(testBool) + "," + testString + "," + str(testStruct) + "," + str(testList) + "," + str(testListStruct) + "," + str(testListInterface) + "}"

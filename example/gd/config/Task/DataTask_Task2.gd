class_name DataTask_Task2
## task.task2
# 公开属性
var taskid: int:
	get:
		return taskid  # 任务完成条件类型（id的范围为1-100）
var name: Array[String]:
	get:
		return name
var nexttask: int:
	get:
		return nexttask
var completecondition: DataTask_Completecondition:
	get:
		return completecondition
var exp: int:
	get:
		return exp
var testBool: bool:
	get:
		return testBool
var testString: String:
	get:
		return testString
var testStruct: DataPosition:
	get:
		return testStruct
var testList: Array[int]:
	get:
		return testList
var testListStruct: Array[DataPosition]:
	get:
		return testListStruct
var testListInterface: Array[DataAi_Triggertick]:
	get:
		return testListInterface
# 外键引用属性
var TaskidRef: DataTask_Taskextraexp:
	get:
		return TaskidRef
var NexttaskRef: DataTask_Task:
	get:
		return NexttaskRef
# 创建实例
static func create(stream: ConfigStream) -> DataTask_Task2:
	var instance = DataTask_Task2.new()
	instance.taskid = stream.get_32()
	instance.name = []
	for c in range(stream.get_32()):
		instance.name.append(stream.get_string())
	instance.nexttask = stream.get_32()
	instance.completecondition = DataTask_Completecondition.create(stream)
	instance.exp = stream.get_32()
	instance.testBool = stream.get_bool()
	instance.testString = stream.get_string()
	instance.testStruct = DataPosition.create(stream)
	instance.testList = []
	for c in range(stream.get_32()):
		instance.testList.append(stream.get_32())
	instance.testListStruct = []
	for c in range(stream.get_32()):
		instance.testListStruct.append(DataPosition.create(stream))
	instance.testListInterface = []
	for c in range(stream.get_32()):
		instance.testListInterface.append(DataAi_Triggertick.create(stream))
	return instance

# 主键查询
static func find(id: int) -> DataTask_Task2:
	return _data.get(id)

# 获取所有数据
static func all() -> Array[DataTask_Task2]:
	return _data.values()

# 从流初始化
static func _init_from_stream(stream: ConfigStream, err: ConfigErrors):
	var count = stream.get_32()
	for i in range(count):
		var item = create(stream)
		_data[item.taskid] = item
# 内部存储
static var _data: Dictionary[int, DataTask_Task2] = {}
# 解析外键引用
func _resolve(errors: ConfigErrors):
	if completecondition != null:
		completecondition._resolve(errors)
	TaskidRef = DataTask_Taskextraexp.find(taskid)
	NexttaskRef = DataTask_Task.find(nexttask)
static func _resolve_refs(errors: ConfigErrors):
	for item in all():
		item._resolve(errors)

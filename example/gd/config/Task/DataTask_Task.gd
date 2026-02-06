class_name DataTask_Task
## task.task
# 公开属性
var taskid: int:
	get:
		return taskid  # 任务完成条件类型（id的范围为1-100）
var name: Array[String]:
	get:
		return name  # 程序用名字
var nexttask: int:
	get:
		return nexttask
var completecondition: DataTask_Completecondition:
	get:
		return completecondition
var exp: int:
	get:
		return exp
var testDefaultBean: DataTask_Testdefaultbean:
	get:
		return testDefaultBean  # 测试
# 外键引用属性
var TaskidRef: DataTask_Taskextraexp:
	get:
		return TaskidRef
var NexttaskRef: DataTask_Task:
	get:
		return NexttaskRef
# 创建实例
static func create(stream: ConfigStream) -> DataTask_Task:
	var instance = DataTask_Task.new()
	instance.taskid = stream.get_32()
	instance.name = []
	for c in range(stream.get_32()):
		instance.name.append(stream.get_string())
	instance.nexttask = stream.get_32()
	instance.completecondition = DataTask_Completecondition.create(stream)
	instance.exp = stream.get_32()
	instance.testDefaultBean = DataTask_Testdefaultbean.create(stream)
	return instance

# 主键查询
static func find(id: int) -> DataTask_Task:
	return _data.get(id)

# 获取所有数据
static func all() -> Array[DataTask_Task]:
	return _data.values()

# 从流初始化
static func _init_from_stream(stream: ConfigStream, err: ConfigErrors):
	var count = stream.get_32()
	for i in range(count):
		var item = create(stream)
		_data[item.taskid] = item
# 内部存储
static var _data: Dictionary[int, DataTask_Task] = {}
# 解析外键引用
func _resolve(errors: ConfigErrors):
	if completecondition != null:
		completecondition._resolve(errors)
	TaskidRef = DataTask_Taskextraexp.find(taskid)
	NexttaskRef = DataTask_Task.find(nexttask)
static func _resolve_refs(errors: ConfigErrors):
	for item in all():
		item._resolve(errors)

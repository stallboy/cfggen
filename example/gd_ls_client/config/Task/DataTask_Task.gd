class_name DataTask_Task
## task.task
# 公开属性
var taskid: int  # 任务完成条件类型（id的范围为1-100）
var name: Array[ConfigText]  # 程序用名字
var nexttask: int
var completecondition: DataTask_Completecondition
var exp: int
var testDefaultBean: DataTask_Testdefaultbean  # 测试
# 外键引用属性
var NullableRefTaskid: DataTask_Taskextraexp
var NullableRefNexttask: DataTask_Task
# 创建实例
static func create(stream: ConfigStream) -> DataTask_Task:
	var instance = DataTask_Task.new()
	instance.taskid = stream.read_int32()
	for c in range(stream.read_int32()):
		instance.name.append(ConfigText.create(stream))
	instance.nexttask = stream.read_int32()
	instance.completecondition = DataTask_Completecondition.create(stream)
	instance.exp = stream.read_int32()
	instance.testDefaultBean = DataTask_Testdefaultbean.create(stream)
	return instance

# 主键查询
static func find(id: int) -> DataTask_Task:
	return _data.get(id)

# 获取所有数据
static func all() -> Array[DataTask_Task]:
	return _data.values()

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	var count = stream.read_int32()
	for i in range(count):
		var item = create(stream)
		_data[item.taskid] = item
# 内部存储
static var _data: Dictionary[int, DataTask_Task] = {}
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
	return "DataTask_Task{" + str(taskid) + "," + str(name) + "," + str(nexttask) + "," + str(completecondition) + "," + str(exp) + "," + str(testDefaultBean) + "}"

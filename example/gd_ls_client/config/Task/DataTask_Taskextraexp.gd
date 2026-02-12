class_name DataTask_Taskextraexp
## task.taskextraexp
# 公开属性
var taskid: int:
	get:
		return taskid  # 任务完成条件类型（id的范围为1-100）
var extraexp: int:
	get:
		return extraexp  # 额外奖励经验
var test1: String:
	get:
		return test1
var test2: String:
	get:
		return test2
var fielda: String:
	get:
		return fielda
var fieldb: String:
	get:
		return fieldb
var fieldc: String:
	get:
		return fieldc
var fieldd: String:
	get:
		return fieldd
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataTask_Taskextraexp:
	var instance = DataTask_Taskextraexp.new()
	instance.taskid = stream.get_32()
	instance.extraexp = stream.get_32()
	instance.test1 = stream.read_string_in_pool()
	instance.test2 = stream.read_string_in_pool()
	instance.fielda = stream.read_string_in_pool()
	instance.fieldb = stream.read_string_in_pool()
	instance.fieldc = stream.read_string_in_pool()
	instance.fieldd = stream.read_string_in_pool()
	return instance

# 主键查询
static func find(id: int) -> DataTask_Taskextraexp:
	return _data.get(id)

# 获取所有数据
static func all() -> Array[DataTask_Taskextraexp]:
	return _data.values()

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	var count = stream.get_32()
	for i in range(count):
		var item = create(stream)
		_data[item.taskid] = item
# 内部存储
static var _data: Dictionary[int, DataTask_Taskextraexp] = {}
# 解析外键引用

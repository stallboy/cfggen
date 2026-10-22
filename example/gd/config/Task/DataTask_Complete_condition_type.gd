class_name DataTask_Complete_condition_type
## task.complete_condition_type
## 任务完成条件类型
# 公开属性
var id: int  # 任务完成条件类型（id的范围为1-100）
var name: String  # 程序用名字

# 静态枚举实例
static var KillMonster: DataTask_Complete_condition_type
static var TalkNpc: DataTask_Complete_condition_type
static var CollectItem: DataTask_Complete_condition_type
static var ConditionAnd: DataTask_Complete_condition_type
static var Chat: DataTask_Complete_condition_type
static var TestNoColumn: DataTask_Complete_condition_type
static var Aa: DataTask_Complete_condition_type
# 内部存储
static var _data: Dictionary[int, DataTask_Complete_condition_type] = {}
# 主键查询
static func find(key_0: int) -> DataTask_Complete_condition_type:
	return _data.get(key_0)
# 获取所有数据
static func all() -> Array[DataTask_Complete_condition_type]:
	return _data.values()

# 字符串表示
func _to_string() -> String:
	return "DataTask_Complete_condition_type{" + str(id) + "," + name + "}"

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	_data = {}
	KillMonster = null
	TalkNpc = null
	CollectItem = null
	ConditionAnd = null
	Chat = null
	TestNoColumn = null
	Aa = null
	var count = stream.read_count(1)
	for i in range(count):
		var item = _create(stream)
		_data[item.id] = item
		if item.name.strip_edges() != "":
			match item.name.strip_edges():
				"KillMonster":
					if KillMonster != null:
						_errors.enum_dup("task.complete_condition_type", str(item))
					KillMonster = item
				"TalkNpc":
					if TalkNpc != null:
						_errors.enum_dup("task.complete_condition_type", str(item))
					TalkNpc = item
				"CollectItem":
					if CollectItem != null:
						_errors.enum_dup("task.complete_condition_type", str(item))
					CollectItem = item
				"ConditionAnd":
					if ConditionAnd != null:
						_errors.enum_dup("task.complete_condition_type", str(item))
					ConditionAnd = item
				"Chat":
					if Chat != null:
						_errors.enum_dup("task.complete_condition_type", str(item))
					Chat = item
				"TestNoColumn":
					if TestNoColumn != null:
						_errors.enum_dup("task.complete_condition_type", str(item))
					TestNoColumn = item
				"aa":
					if Aa != null:
						_errors.enum_dup("task.complete_condition_type", str(item))
					Aa = item
				_:
					_errors.enum_data_add("task.complete_condition_type", str(item))
	if KillMonster == null:
		_errors.enum_null("task.complete_condition_type", "KillMonster")
	if TalkNpc == null:
		_errors.enum_null("task.complete_condition_type", "TalkNpc")
	if CollectItem == null:
		_errors.enum_null("task.complete_condition_type", "CollectItem")
	if ConditionAnd == null:
		_errors.enum_null("task.complete_condition_type", "ConditionAnd")
	if Chat == null:
		_errors.enum_null("task.complete_condition_type", "Chat")
	if TestNoColumn == null:
		_errors.enum_null("task.complete_condition_type", "TestNoColumn")
	if Aa == null:
		_errors.enum_null("task.complete_condition_type", "aa")

# 创建实例
static func _create(stream: ConfigStream) -> DataTask_Complete_condition_type:
	var instance = DataTask_Complete_condition_type.new()
	instance.id = stream.read_int32()
	instance.name = stream.read_string_in_pool()
	return instance



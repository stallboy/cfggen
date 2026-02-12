class_name DataTask_Completeconditiontype
## task.completeconditiontype
# 公开属性
var id: int  # 任务完成条件类型（id的范围为1-100）
var name: String  # 程序用名字
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataTask_Completeconditiontype:
	var instance = DataTask_Completeconditiontype.new()
	instance.id = stream.read_int32()
	instance.name = stream.read_string_in_pool()
	return instance

# 主键查询
static func find(id: int) -> DataTask_Completeconditiontype:
	return _data.get(id)

# 获取所有数据
static func all() -> Array[DataTask_Completeconditiontype]:
	return _data.values()

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	var count = stream.read_int32()
	for i in range(count):
		var item = create(stream)
		_data[item.id] = item
		if item.name.strip_edges() != "":
			match item.name.strip_edges():
				"KillMonster":
					if KillMonster != null:
						_errors.enum_dup("task.completeconditiontype", str(item))
					KillMonster = item
				"TalkNpc":
					if TalkNpc != null:
						_errors.enum_dup("task.completeconditiontype", str(item))
					TalkNpc = item
				"CollectItem":
					if CollectItem != null:
						_errors.enum_dup("task.completeconditiontype", str(item))
					CollectItem = item
				"ConditionAnd":
					if ConditionAnd != null:
						_errors.enum_dup("task.completeconditiontype", str(item))
					ConditionAnd = item
				"Chat":
					if Chat != null:
						_errors.enum_dup("task.completeconditiontype", str(item))
					Chat = item
				"TestNoColumn":
					if TestNoColumn != null:
						_errors.enum_dup("task.completeconditiontype", str(item))
					TestNoColumn = item
				"aa":
					if Aa != null:
						_errors.enum_dup("task.completeconditiontype", str(item))
					Aa = item
				_:
					_errors.enum_data_add("task.completeconditiontype", str(item))
	if KillMonster == null:
		_errors.enum_null("task.completeconditiontype", "KillMonster")
	if TalkNpc == null:
		_errors.enum_null("task.completeconditiontype", "TalkNpc")
	if CollectItem == null:
		_errors.enum_null("task.completeconditiontype", "CollectItem")
	if ConditionAnd == null:
		_errors.enum_null("task.completeconditiontype", "ConditionAnd")
	if Chat == null:
		_errors.enum_null("task.completeconditiontype", "Chat")
	if TestNoColumn == null:
		_errors.enum_null("task.completeconditiontype", "TestNoColumn")
	if Aa == null:
		_errors.enum_null("task.completeconditiontype", "aa")
# 内部存储
static var _data: Dictionary[int, DataTask_Completeconditiontype] = {}
# 静态枚举实例
static var KillMonster: DataTask_Completeconditiontype
static var TalkNpc: DataTask_Completeconditiontype
static var CollectItem: DataTask_Completeconditiontype
static var ConditionAnd: DataTask_Completeconditiontype
static var Chat: DataTask_Completeconditiontype
static var TestNoColumn: DataTask_Completeconditiontype
static var Aa: DataTask_Completeconditiontype
# 解析外键引用
# 字符串表示
func _to_string() -> String:
	return "DataTask_Completeconditiontype{" + str(id) + "," + name + "}"

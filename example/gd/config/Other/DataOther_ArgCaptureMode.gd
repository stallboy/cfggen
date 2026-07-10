class_name DataOther_ArgCaptureMode
## other.ArgCaptureMode
# 公开属性
var name: String
var id: int
var comment: String

# 静态枚举实例
static var Snapshot: DataOther_ArgCaptureMode
static var Dynamic: DataOther_ArgCaptureMode
# 内部存储
static var _data: Dictionary[String, DataOther_ArgCaptureMode] = {}
static var _id_map: Dictionary[int, DataOther_ArgCaptureMode] = {}
# 主键查询
static func find(id: String) -> DataOther_ArgCaptureMode:
	return _data.get(id)
# 唯一键查询
static func find_by_id(id) -> DataOther_ArgCaptureMode:
	return _id_map.get(id)
# 获取所有数据
static func all() -> Array[DataOther_ArgCaptureMode]:
	return _data.values()

# 字符串表示
func _to_string() -> String:
	return "DataOther_ArgCaptureMode{" + name + "," + str(id) + "," + comment + "}"

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	_id_map = {}
	var count = stream.read_int32()
	for i in range(count):
		var item = _create(stream)
		_data[item.name] = item
		_id_map[item.id] = item
		if item.name.strip_edges() != "":
			match item.name.strip_edges():
				"Snapshot":
					if Snapshot != null:
						_errors.enum_dup("other.ArgCaptureMode", str(item))
					Snapshot = item
				"Dynamic":
					if Dynamic != null:
						_errors.enum_dup("other.ArgCaptureMode", str(item))
					Dynamic = item
				_:
					_errors.enum_data_add("other.ArgCaptureMode", str(item))
	if Snapshot == null:
		_errors.enum_null("other.ArgCaptureMode", "Snapshot")
	if Dynamic == null:
		_errors.enum_null("other.ArgCaptureMode", "Dynamic")

# 创建实例
static func _create(stream: ConfigStream) -> DataOther_ArgCaptureMode:
	var instance = DataOther_ArgCaptureMode.new()
	instance.name = stream.read_string_in_pool()
	instance.id = stream.read_int32()
	instance.comment = stream.read_text_in_pool()
	return instance



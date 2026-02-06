class_name DataOther_Keytest
## other.keytest
# 公开属性
var id1: int:
	get:
		return id1
var id2: int:
	get:
		return id2
var id3: int:
	get:
		return id3
var ids: Array[int]:
	get:
		return ids
# 外键引用属性
var IdsRef: Array[DataOther_Signin]:
	get:
		return IdsRef
# 创建实例
static func create(stream: ConfigStream) -> DataOther_Keytest:
	var instance = DataOther_Keytest.new()
	instance.id1 = stream.get_32()
	instance.id2 = stream.get_64()
	instance.id3 = stream.get_32()
	instance.ids = []
	for c in range(stream.get_32()):
		instance.ids.append(stream.get_32())
	return instance

# 主键查询
static func find(id: int) -> DataOther_Keytest:
	return _data.get(id)

# 唯一键查询
static func find_by_id1_id3(id1, id3) -> DataOther_Keytest:
	return _id1_id3_map.get(id1, id3)
# 唯一键查询
static func find_by_id2(id2) -> DataOther_Keytest:
	return _id2_map.get(id2)
# 唯一键查询
static func find_by_id2_id3(id2, id3) -> DataOther_Keytest:
	return _id2_id3_map.get(id2, id3)
# 获取所有数据
static func all() -> Array[DataOther_Keytest]:
	return _data.values()

# 从流初始化
static func _init_from_stream(stream: ConfigStream, err: ConfigErrors):
	_id1_id3_map = {}
	_id2_map = {}
	_id2_id3_map = {}
	var count = stream.get_32()
	for i in range(count):
		var item = create(stream)
		_data[item.id1] = item
		_id1_id3_map[{"id1": id1, "id3": id3}] = item
		_id2_map[id2] = item
		_id2_id3_map[{"id2": id2, "id3": id3}] = item
# 内部存储
static var _data: Dictionary[int, DataOther_Keytest] = {}
static var _id1_id3_map: Dictionary = {}
static var _id2_map: Dictionary[int, DataOther_Keytest] = {}
static var _id2_id3_map: Dictionary = {}
# 解析外键引用
func _resolve(errors: ConfigErrors):
	IdsRef = []
	for item in ids:
		var r = DataOther_Signin.find(item)
		if r == null:
			errors.ref_null("other.keytest", "ids")
		IdsRef.append(r)
static func _resolve_refs(errors: ConfigErrors):
	for item in all():
		item._resolve(errors)

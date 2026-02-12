class_name DataOther_Drop
## other.drop
# 公开属性
var dropid: int  # 序号
var name: String  # 名字
var items: Array[DataOther_Dropitem]  # 掉落概率
var testmap: Dictionary[int, int]  # 测试map block
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataOther_Drop:
	var instance = DataOther_Drop.new()
	instance.dropid = stream.read_int32()
	instance.name = stream.read_text_in_pool()
	for c in range(stream.read_int32()):
		instance.items.append(DataOther_Dropitem.create(stream))
	for c in range(stream.read_int32()):
		var k = stream.read_int32()
		var v = stream.read_int32()
		instance.testmap[k] = v
	return instance

# 主键查询
static func find(id: int) -> DataOther_Drop:
	return _data.get(id)

# 获取所有数据
static func all() -> Array[DataOther_Drop]:
	return _data.values()

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	var count = stream.read_int32()
	for i in range(count):
		var item = create(stream)
		_data[item.dropid] = item
# 内部存储
static var _data: Dictionary[int, DataOther_Drop] = {}
# 解析外键引用

class_name DataOther_Signin
## other.signin
# 公开属性
var id: int  # 礼包ID
var item2countMap: Dictionary[int, int]  # 普通奖励
var vipitem2vipcountMap: Dictionary[int, int]  # vip奖励
var viplevel: int  # 领取vip奖励的最低等级
var iconFile: String  # 礼包图标
# 外键引用属性
var RefVipitem2vipcountMap: Dictionary[int, DataOther_Loot]
# 创建实例
static func create(stream: ConfigStream) -> DataOther_Signin:
	var instance = DataOther_Signin.new()
	instance.id = stream.read_int32()
	for c in range(stream.read_int32()):
		var k = stream.read_int32()
		var v = stream.read_int32()
		instance.item2countMap[k] = v
	for c in range(stream.read_int32()):
		var k = stream.read_int32()
		var v = stream.read_int32()
		instance.vipitem2vipcountMap[k] = v
	instance.viplevel = stream.read_int32()
	instance.iconFile = stream.read_string_in_pool()
	return instance

# 主键查询
static func find(id: int) -> DataOther_Signin:
	return _data.get(id)

# 获取所有数据
static func all() -> Array[DataOther_Signin]:
	return _data.values()

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	var count = stream.read_int32()
	for i in range(count):
		var item = create(stream)
		_data[item.id] = item
# 内部存储
static var _data: Dictionary[int, DataOther_Signin] = {}
# 解析外键引用
func _resolve(errors: ConfigErrors):
	for k in vipitem2vipcountMap.keys():
		var v = DataOther_Loot.find(vipitem2vipcountMap[k])
		if v == null:
			errors.ref_null("other.signin", "vipitem2vipcountMap")
		RefVipitem2vipcountMap[k] = v
static func _resolve_refs(errors: ConfigErrors):
	for item in all():
		item._resolve(errors)

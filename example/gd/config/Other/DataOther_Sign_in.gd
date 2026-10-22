class_name DataOther_Sign_in
## other.sign_in
# 公开属性
var id: int  # 礼包ID
var item2countMap: Dictionary[int, int]  # 普通奖励
var vipitem2vipcountMap: Dictionary[int, int]  # vip奖励
var viplevel: int  # 领取vip奖励的最低等级
var iconFile: String  # 礼包图标
# 外键引用属性
var RefVipitem2vipcountMap: Dictionary[int, DataOther_Loot]

# 内部存储
static var _data: Dictionary[int, DataOther_Sign_in] = {}
# 主键查询
static func find(key_0: int) -> DataOther_Sign_in:
	return _data.get(key_0)
# 获取所有数据
static func all() -> Array[DataOther_Sign_in]:
	return _data.values()

# 字符串表示
func _to_string() -> String:
	return "DataOther_Sign_in{" + str(id) + "," + str(item2countMap) + "," + str(vipitem2vipcountMap) + "," + str(viplevel) + "," + iconFile + "}"

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	_data = {}
	var count = stream.read_count(1)
	for i in range(count):
		var item = _create(stream)
		_data[item.id] = item

# 创建实例
static func _create(stream: ConfigStream) -> DataOther_Sign_in:
	var instance = DataOther_Sign_in.new()
	instance.id = stream.read_int32()
	for c in range(stream.read_count(0)):
		var k = stream.read_int32()
		var v = stream.read_int32()
		if instance.item2countMap.has(k):
			stream.fail("Duplicate map key: other.sign_in.item2countMap")
			return instance
		instance.item2countMap[k] = v
	for c in range(stream.read_count(0)):
		var k = stream.read_int32()
		var v = stream.read_int32()
		if instance.vipitem2vipcountMap.has(k):
			stream.fail("Duplicate map key: other.sign_in.vipitem2vipcountMap")
			return instance
		instance.vipitem2vipcountMap[k] = v
	instance.viplevel = stream.read_int32()
	instance.iconFile = stream.read_string_in_pool()
	return instance


# 解析外键引用
func _resolve(errors: ConfigErrors):
	pass
	for k in vipitem2vipcountMap.keys():
		var v = DataOther_Loot.find(vipitem2vipcountMap[k])
		if v == null:
			errors.ref_null("other.sign_in", "vipitem2vipcountMap")
		RefVipitem2vipcountMap[k] = v

static func _resolve_refs(errors: ConfigErrors):
	for item in all():
		item._resolve(errors)

class_name DataEquip_Rank
## equip.rank
# 公开属性
var rankID: int  # 稀有度
var rankName: String  # 程序用名字
var rankShowName: String  # 显示名称
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataEquip_Rank:
	var instance = DataEquip_Rank.new()
	instance.rankID = stream.read_int32()
	instance.rankName = stream.read_string_in_pool()
	instance.rankShowName = stream.read_string_in_pool()
	return instance

# 主键查询
static func find(id: int) -> DataEquip_Rank:
	return _data.get(id)

# 获取所有数据
static func all() -> Array[DataEquip_Rank]:
	return _data.values()

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	var count = stream.read_int32()
	for i in range(count):
		var item = create(stream)
		_data[item.rankID] = item
		if item.rankName.strip_edges() != "":
			match item.rankName.strip_edges():
				"white":
					if White != null:
						_errors.enum_dup("equip.rank", str(item))
					White = item
				"green":
					if Green != null:
						_errors.enum_dup("equip.rank", str(item))
					Green = item
				"blue":
					if Blue != null:
						_errors.enum_dup("equip.rank", str(item))
					Blue = item
				"purple":
					if Purple != null:
						_errors.enum_dup("equip.rank", str(item))
					Purple = item
				"yellow":
					if Yellow != null:
						_errors.enum_dup("equip.rank", str(item))
					Yellow = item
				_:
					_errors.enum_data_add("equip.rank", str(item))
	if White == null:
		_errors.enum_null("equip.rank", "white")
	if Green == null:
		_errors.enum_null("equip.rank", "green")
	if Blue == null:
		_errors.enum_null("equip.rank", "blue")
	if Purple == null:
		_errors.enum_null("equip.rank", "purple")
	if Yellow == null:
		_errors.enum_null("equip.rank", "yellow")
# 内部存储
static var _data: Dictionary[int, DataEquip_Rank] = {}
# 静态枚举实例
static var White: DataEquip_Rank
static var Green: DataEquip_Rank
static var Blue: DataEquip_Rank
static var Purple: DataEquip_Rank
static var Yellow: DataEquip_Rank
# 解析外键引用
# 字符串表示
func _to_string() -> String:
	return "DataEquip_Rank{" + str(rankID) + "," + rankName + "," + rankShowName + "}"

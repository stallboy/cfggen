class_name DataEquip_Equipconfig
## equip.equipconfig
# 公开属性
var entry: String  # 入口，程序填
var stone_count_for_set: int  # 形成套装的音石数量
var draw_protect_name: String  # 保底策略名称
var broadcastid: int  # 公告Id
var broadcast_least_quality: int  # 公告的最低品质
var week_reward_mailid: int  # 抽卡周奖励的邮件id
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataEquip_Equipconfig:
	var instance = DataEquip_Equipconfig.new()
	instance.entry = stream.read_string_in_pool()
	instance.stone_count_for_set = stream.read_int32()
	instance.draw_protect_name = stream.read_string_in_pool()
	instance.broadcastid = stream.read_int32()
	instance.broadcast_least_quality = stream.read_int32()
	instance.week_reward_mailid = stream.read_int32()
	return instance

# 主键查询
static func find(id: String) -> DataEquip_Equipconfig:
	return _data.get(id)

# 获取所有数据
static func all() -> Array[DataEquip_Equipconfig]:
	return _data.values()

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	var count = stream.read_int32()
	for i in range(count):
		var item = create(stream)
		_data[item.entry] = item
		if item.entry.strip_edges() != "":
			match item.entry.strip_edges():
				"Instance":
					if Instance != null:
						_errors.enum_dup("equip.equipconfig", str(item))
					Instance = item
				"Instance2":
					if Instance2 != null:
						_errors.enum_dup("equip.equipconfig", str(item))
					Instance2 = item
				_:
					_errors.enum_data_add("equip.equipconfig", str(item))
	if Instance == null:
		_errors.enum_null("equip.equipconfig", "Instance")
	if Instance2 == null:
		_errors.enum_null("equip.equipconfig", "Instance2")
# 内部存储
static var _data: Dictionary[String, DataEquip_Equipconfig] = {}
# 静态枚举实例
static var Instance: DataEquip_Equipconfig
static var Instance2: DataEquip_Equipconfig
# 解析外键引用
# 字符串表示
func _to_string() -> String:
	return "DataEquip_Equipconfig{" + entry + "," + str(stone_count_for_set) + "," + draw_protect_name + "," + str(broadcastid) + "," + str(broadcast_least_quality) + "," + str(week_reward_mailid) + "}"

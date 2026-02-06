class_name DataEquip_Equipconfig
## equip.equipconfig
# 公开属性
var entry: String:
	get:
		return entry  # 入口，程序填
var stone_count_for_set: int:
	get:
		return stone_count_for_set  # 形成套装的音石数量
var draw_protect_name: String:
	get:
		return draw_protect_name  # 保底策略名称
var broadcastid: int:
	get:
		return broadcastid  # 公告Id
var broadcast_least_quality: int:
	get:
		return broadcast_least_quality  # 公告的最低品质
var week_reward_mailid: int:
	get:
		return week_reward_mailid  # 抽卡周奖励的邮件id
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataEquip_Equipconfig:
	var instance = DataEquip_Equipconfig.new()
	instance.entry = stream.get_string()
	instance.stone_count_for_set = stream.get_32()
	instance.draw_protect_name = stream.get_string()
	instance.broadcastid = stream.get_32()
	instance.broadcast_least_quality = stream.get_32()
	instance.week_reward_mailid = stream.get_32()
	return instance

# 主键查询
static func find(id: String) -> DataEquip_Equipconfig:
	return _data.get(id)

# 获取所有数据
static func all() -> Array[DataEquip_Equipconfig]:
	return _data.values()

# 从流初始化
static func _init_from_stream(stream: ConfigStream, err: ConfigErrors):
	var count = stream.get_32()
	for i in range(count):
		var item = create(stream)
		_data[item.entry] = item
		if item.entry.strip_edges() != "":
			match item.entry.strip_edges():
				"Instance":
					if Instance != null:
						err.error("枚举重复: equip.equipconfig, " + str(item))
					Instance = item
				"Instance2":
					if Instance2 != null:
						err.error("枚举重复: equip.equipconfig, " + str(item))
					Instance2 = item
				_:
					err.error("枚举数据错误: equip.equipconfig, " + str(item))
	if Instance == null:
		err.error("枚举缺失: equip.equipconfig, Instance")
	if Instance2 == null:
		err.error("枚举缺失: equip.equipconfig, Instance2")
# 内部存储
static var _data: Dictionary[String, DataEquip_Equipconfig] = {}
# 静态枚举实例
static var Instance: DataEquip_Equipconfig
static var Instance2: DataEquip_Equipconfig
# 解析外键引用

class_name DataEquip_Ability
## equip.ability
# 公开属性
var id: int:
	get:
		return id  # 属性类型
var name: String:
	get:
		return name  # 程序用名字
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataEquip_Ability:
	var instance = DataEquip_Ability.new()
	instance.id = stream.get_32()
	instance.name = stream.get_string()
	return instance

# 主键查询
static func find(id: int) -> DataEquip_Ability:
	return _data.get(id)

# 获取所有数据
static func all() -> Array[DataEquip_Ability]:
	return _data.values()

# 从流初始化
static func _init_from_stream(stream: ConfigStream, err: ConfigErrors):
	var count = stream.get_32()
	for i in range(count):
		var item = create(stream)
		_data[item.id] = item
		if item.name.strip_edges() != "":
			match item.name.strip_edges():
				"attack":
					if Attack != null:
						err.error("枚举重复: equip.ability, " + str(item))
					Attack = item
				"defence":
					if Defence != null:
						err.error("枚举重复: equip.ability, " + str(item))
					Defence = item
				"hp":
					if Hp != null:
						err.error("枚举重复: equip.ability, " + str(item))
					Hp = item
				"critical":
					if Critical != null:
						err.error("枚举重复: equip.ability, " + str(item))
					Critical = item
				"critical_resist":
					if Critical_resist != null:
						err.error("枚举重复: equip.ability, " + str(item))
					Critical_resist = item
				"block":
					if Block != null:
						err.error("枚举重复: equip.ability, " + str(item))
					Block = item
				"break_armor":
					if Break_armor != null:
						err.error("枚举重复: equip.ability, " + str(item))
					Break_armor = item
				_:
					err.error("枚举数据错误: equip.ability, " + str(item))
	if Attack == null:
		err.error("枚举缺失: equip.ability, attack")
	if Defence == null:
		err.error("枚举缺失: equip.ability, defence")
	if Hp == null:
		err.error("枚举缺失: equip.ability, hp")
	if Critical == null:
		err.error("枚举缺失: equip.ability, critical")
	if Critical_resist == null:
		err.error("枚举缺失: equip.ability, critical_resist")
	if Block == null:
		err.error("枚举缺失: equip.ability, block")
	if Break_armor == null:
		err.error("枚举缺失: equip.ability, break_armor")
# 内部存储
static var _data: Dictionary[int, DataEquip_Ability] = {}
# 静态枚举实例
static var Attack: DataEquip_Ability
static var Defence: DataEquip_Ability
static var Hp: DataEquip_Ability
static var Critical: DataEquip_Ability
static var Critical_resist: DataEquip_Ability
static var Block: DataEquip_Ability
static var Break_armor: DataEquip_Ability
# 解析外键引用

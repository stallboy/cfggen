class_name DataEquip_Ability
## equip.ability
# 公开属性
var id: int  # 属性类型
var name: String  # 程序用名字

# 静态枚举实例
static var Attack: DataEquip_Ability
static var Defence: DataEquip_Ability
static var Hp: DataEquip_Ability
static var Critical: DataEquip_Ability
static var Critical_resist: DataEquip_Ability
static var Block: DataEquip_Ability
static var Break_armor: DataEquip_Ability
# 内部存储
static var _data: Dictionary[int, DataEquip_Ability] = {}
# 主键查询
static func find(id: int) -> DataEquip_Ability:
	return _data.get(id)
# 获取所有数据
static func all() -> Array[DataEquip_Ability]:
	return _data.values()

# 字符串表示
func _to_string() -> String:
	return "DataEquip_Ability{" + str(id) + "," + name + "}"

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	var count = stream.read_int32()
	for i in range(count):
		var item = _create(stream)
		_data[item.id] = item
		if item.name.strip_edges() != "":
			match item.name.strip_edges():
				"attack":
					if Attack != null:
						_errors.enum_dup("equip.ability", str(item))
					Attack = item
				"defence":
					if Defence != null:
						_errors.enum_dup("equip.ability", str(item))
					Defence = item
				"hp":
					if Hp != null:
						_errors.enum_dup("equip.ability", str(item))
					Hp = item
				"critical":
					if Critical != null:
						_errors.enum_dup("equip.ability", str(item))
					Critical = item
				"critical_resist":
					if Critical_resist != null:
						_errors.enum_dup("equip.ability", str(item))
					Critical_resist = item
				"block":
					if Block != null:
						_errors.enum_dup("equip.ability", str(item))
					Block = item
				"break_armor":
					if Break_armor != null:
						_errors.enum_dup("equip.ability", str(item))
					Break_armor = item
				_:
					_errors.enum_data_add("equip.ability", str(item))
	if Attack == null:
		_errors.enum_null("equip.ability", "attack")
	if Defence == null:
		_errors.enum_null("equip.ability", "defence")
	if Hp == null:
		_errors.enum_null("equip.ability", "hp")
	if Critical == null:
		_errors.enum_null("equip.ability", "critical")
	if Critical_resist == null:
		_errors.enum_null("equip.ability", "critical_resist")
	if Block == null:
		_errors.enum_null("equip.ability", "block")
	if Break_armor == null:
		_errors.enum_null("equip.ability", "break_armor")

# 创建实例
static func _create(stream: ConfigStream) -> DataEquip_Ability:
	var instance = DataEquip_Ability.new()
	instance.id = stream.read_int32()
	instance.name = stream.read_string_in_pool()
	return instance



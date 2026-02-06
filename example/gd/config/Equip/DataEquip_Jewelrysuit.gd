class_name DataEquip_Jewelrysuit
## equip.jewelrysuit
# 公开属性
var suitID: int:
	get:
		return suitID  # 饰品套装ID
var ename: String:
	get:
		return ename
var name: String:
	get:
		return name  # 策划用名字
var ability1: int:
	get:
		return ability1  # 套装属性类型1（装备套装中的两件时增加的属性）
var ability1Value: int:
	get:
		return ability1Value  # 套装属性1
var ability2: int:
	get:
		return ability2  # 套装属性类型2（装备套装中的三件时增加的属性）
var ability2Value: int:
	get:
		return ability2Value  # 套装属性2
var ability3: int:
	get:
		return ability3  # 套装属性类型3（装备套装中的四件时增加的属性）
var ability3Value: int:
	get:
		return ability3Value  # 套装属性3
var suitList: Array[int]:
	get:
		return suitList  # 部件1
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataEquip_Jewelrysuit:
	var instance = DataEquip_Jewelrysuit.new()
	instance.suitID = stream.get_32()
	instance.ename = stream.get_string()
	instance.name = stream.get_string()
	instance.ability1 = stream.get_32()
	instance.ability1Value = stream.get_32()
	instance.ability2 = stream.get_32()
	instance.ability2Value = stream.get_32()
	instance.ability3 = stream.get_32()
	instance.ability3Value = stream.get_32()
	instance.suitList = []
	for c in range(stream.get_32()):
		instance.suitList.append(stream.get_32())
	return instance

# 主键查询
static func find(id: int) -> DataEquip_Jewelrysuit:
	return _data.get(id)

# 获取所有数据
static func all() -> Array[DataEquip_Jewelrysuit]:
	return _data.values()

# 从流初始化
static func _init_from_stream(stream: ConfigStream, err: ConfigErrors):
	var count = stream.get_32()
	for i in range(count):
		var item = create(stream)
		_data[item.suitID] = item
		if item.ename.strip_edges() != "":
			match item.ename.strip_edges():
				"SpecialSuit":
					if SpecialSuit != null:
						err.error("枚举重复: equip.jewelrysuit, " + str(item))
					SpecialSuit = item
				_:
					err.error("枚举数据错误: equip.jewelrysuit, " + str(item))
	if SpecialSuit == null:
		err.error("枚举缺失: equip.jewelrysuit, SpecialSuit")
# 内部存储
static var _data: Dictionary[int, DataEquip_Jewelrysuit] = {}
# 静态枚举实例
static var SpecialSuit: DataEquip_Jewelrysuit
# 解析外键引用

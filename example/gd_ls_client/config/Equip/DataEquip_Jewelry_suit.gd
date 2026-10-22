class_name DataEquip_Jewelry_suit
## equip.jewelry_suit
# 公开属性
var suitID: int  # 饰品套装ID
var ename: String
var name: ConfigText  # 策划用名字
var ability1: int  # 套装属性类型1（装备套装中的两件时增加的属性）
var ability1Value: int  # 套装属性1
var ability2: int  # 套装属性类型2（装备套装中的三件时增加的属性）
var ability2Value: int  # 套装属性2
var ability3: int  # 套装属性类型3（装备套装中的四件时增加的属性）
var ability3Value: int  # 套装属性3
var suitList: Array[int]  # 部件1

# 静态枚举实例
static var SpecialSuit: DataEquip_Jewelry_suit
# 内部存储
static var _data: Dictionary[int, DataEquip_Jewelry_suit] = {}
# 主键查询
static func find(key_0: int) -> DataEquip_Jewelry_suit:
	return _data.get(key_0)
# 获取所有数据
static func all() -> Array[DataEquip_Jewelry_suit]:
	return _data.values()

# 字符串表示
func _to_string() -> String:
	return "DataEquip_Jewelry_suit{" + str(suitID) + "," + ename + "," + str(name) + "," + str(ability1) + "," + str(ability1Value) + "," + str(ability2) + "," + str(ability2Value) + "," + str(ability3) + "," + str(ability3Value) + "," + str(suitList) + "}"

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	_data = {}
	SpecialSuit = null
	var count = stream.read_count(1)
	for i in range(count):
		var item = _create(stream)
		_data[item.suitID] = item
		if item.ename.strip_edges() != "":
			match item.ename.strip_edges():
				"SpecialSuit":
					if SpecialSuit != null:
						_errors.enum_dup("equip.jewelry_suit", str(item))
					SpecialSuit = item
				_:
					_errors.enum_data_add("equip.jewelry_suit", str(item))
	if SpecialSuit == null:
		_errors.enum_null("equip.jewelry_suit", "SpecialSuit")

# 创建实例
static func _create(stream: ConfigStream) -> DataEquip_Jewelry_suit:
	var instance = DataEquip_Jewelry_suit.new()
	instance.suitID = stream.read_int32()
	instance.ename = stream.read_string_in_pool()
	instance.name = ConfigText._create(stream)
	instance.ability1 = stream.read_int32()
	instance.ability1Value = stream.read_int32()
	instance.ability2 = stream.read_int32()
	instance.ability2Value = stream.read_int32()
	instance.ability3 = stream.read_int32()
	instance.ability3Value = stream.read_int32()
	for c in range(stream.read_count(0)):
		instance.suitList.append(stream.read_int32())
	return instance



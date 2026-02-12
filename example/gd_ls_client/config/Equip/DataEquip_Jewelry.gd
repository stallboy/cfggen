class_name DataEquip_Jewelry
## equip.jewelry
# 公开属性
var iD: int:
	get:
		return iD  # 首饰ID
var name: String:
	get:
		return name  # 首饰名称
var iconFile: String:
	get:
		return iconFile  # 图标ID
var lvlRank: DataLevelrank:
	get:
		return lvlRank  # 首饰等级
var jType: String:
	get:
		return jType  # 首饰类型
var suitID: int:
	get:
		return suitID  # 套装ID（为0是没有不属于套装，首饰品级为4的首饰该参数为套装id，其余情况为0,引用JewelrySuit.csv）
var keyAbility: int:
	get:
		return keyAbility  # 关键属性类型
var keyAbilityValue: int:
	get:
		return keyAbilityValue  # 关键属性数值
var salePrice: int:
	get:
		return salePrice  # 售卖价格
var description: String:
	get:
		return description  # 描述,根据Lvl和Rank来随机3个属性，第一个属性由Lvl,Rank行随机，剩下2个由Lvl和小于Rank的行里随机。Rank最小的时候都从Lvl，Rank里随机。
# 外键引用属性
var RefLvlRank: DataEquip_Jewelryrandom:
	get:
		return RefLvlRank
var RefJType: DataEquip_Jewelrytype:
	get:
		return RefJType
var NullableRefSuitID: DataEquip_Jewelrysuit:
	get:
		return NullableRefSuitID
var RefKeyAbility: DataEquip_Ability:
	get:
		return RefKeyAbility
# 创建实例
static func create(stream: ConfigStream) -> DataEquip_Jewelry:
	var instance = DataEquip_Jewelry.new()
	instance.iD = stream.get_32()
	instance.name = stream.read_string_in_pool()
	instance.iconFile = stream.read_string_in_pool()
	instance.lvlRank = DataLevelrank.create(stream)
	instance.jType = stream.read_string_in_pool()
	instance.suitID = stream.get_32()
	instance.keyAbility = stream.get_32()
	instance.keyAbilityValue = stream.get_32()
	instance.salePrice = stream.get_32()
	instance.description = stream.read_string_in_pool()
	return instance

# 主键查询
static func find(id: int) -> DataEquip_Jewelry:
	return _data.get(id)

# 获取所有数据
static func all() -> Array[DataEquip_Jewelry]:
	return _data.values()

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	var count = stream.get_32()
	for i in range(count):
		var item = create(stream)
		_data[item.iD] = item
# 内部存储
static var _data: Dictionary[int, DataEquip_Jewelry] = {}
# 解析外键引用
func _resolve(errors: ConfigErrors):
	if lvlRank != null:
		lvlRank._resolve(errors)
	RefLvlRank = DataEquip_Jewelryrandom.find(lvlRank)
	if RefLvlRank == null:
		errors.ref_null("equip.jewelry", "LvlRank")
	RefJType = DataEquip_Jewelrytype.find(jType)
	if RefJType == null:
		errors.ref_null("equip.jewelry", "JType")
	NullableRefSuitID = DataEquip_Jewelrysuit.find(suitID)
	RefKeyAbility = DataEquip_Ability.find(keyAbility)
	if RefKeyAbility == null:
		errors.ref_null("equip.jewelry", "KeyAbility")
static func _resolve_refs(errors: ConfigErrors):
	for item in all():
		item._resolve(errors)

class_name DataEquip_Jewelrytype
## equip.jewelrytype
# 公开属性
var typeName: String:
	get:
		return typeName  # 程序用名字
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataEquip_Jewelrytype:
	var instance = DataEquip_Jewelrytype.new()
	instance.typeName = stream.get_string()
	return instance

# 主键查询
static func find(id: String) -> DataEquip_Jewelrytype:
	return _data.get(id)

# 获取所有数据
static func all() -> Array[DataEquip_Jewelrytype]:
	return _data.values()

# 从流初始化
static func _init_from_stream(stream: ConfigStream, err: ConfigErrors):
	var count = stream.get_32()
	for i in range(count):
		var item = create(stream)
		_data[item.typeName] = item
		if item.typeName.strip_edges() != "":
			match item.typeName.strip_edges():
				"Jade":
					if Jade != null:
						err.error("枚举重复: equip.jewelrytype, " + str(item))
					Jade = item
				"Bracelet":
					if Bracelet != null:
						err.error("枚举重复: equip.jewelrytype, " + str(item))
					Bracelet = item
				"Magic":
					if Magic != null:
						err.error("枚举重复: equip.jewelrytype, " + str(item))
					Magic = item
				"Bottle":
					if Bottle != null:
						err.error("枚举重复: equip.jewelrytype, " + str(item))
					Bottle = item
				_:
					err.error("枚举数据错误: equip.jewelrytype, " + str(item))
	if Jade == null:
		err.error("枚举缺失: equip.jewelrytype, Jade")
	if Bracelet == null:
		err.error("枚举缺失: equip.jewelrytype, Bracelet")
	if Magic == null:
		err.error("枚举缺失: equip.jewelrytype, Magic")
	if Bottle == null:
		err.error("枚举缺失: equip.jewelrytype, Bottle")
# 内部存储
static var _data: Dictionary[String, DataEquip_Jewelrytype] = {}
# 静态枚举实例
static var Jade: DataEquip_Jewelrytype
static var Bracelet: DataEquip_Jewelrytype
static var Magic: DataEquip_Jewelrytype
static var Bottle: DataEquip_Jewelrytype
# 解析外键引用

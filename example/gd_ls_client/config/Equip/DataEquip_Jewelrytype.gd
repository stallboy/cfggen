class_name DataEquip_Jewelrytype
## equip.jewelrytype
# 公开属性
var typeName: String  # 程序用名字
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataEquip_Jewelrytype:
	var instance = DataEquip_Jewelrytype.new()
	instance.typeName = stream.read_string_in_pool()
	return instance

# 主键查询
static func find(id: String) -> DataEquip_Jewelrytype:
	return _data.get(id)

# 获取所有数据
static func all() -> Array[DataEquip_Jewelrytype]:
	return _data.values()

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	var count = stream.read_int32()
	for i in range(count):
		var item = create(stream)
		_data[item.typeName] = item
		if item.typeName.strip_edges() != "":
			match item.typeName.strip_edges():
				"Jade":
					if Jade != null:
						_errors.enum_dup("equip.jewelrytype", str(item))
					Jade = item
				"Bracelet":
					if Bracelet != null:
						_errors.enum_dup("equip.jewelrytype", str(item))
					Bracelet = item
				"Magic":
					if Magic != null:
						_errors.enum_dup("equip.jewelrytype", str(item))
					Magic = item
				"Bottle":
					if Bottle != null:
						_errors.enum_dup("equip.jewelrytype", str(item))
					Bottle = item
				_:
					_errors.enum_data_add("equip.jewelrytype", str(item))
	if Jade == null:
		_errors.enum_null("equip.jewelrytype", "Jade")
	if Bracelet == null:
		_errors.enum_null("equip.jewelrytype", "Bracelet")
	if Magic == null:
		_errors.enum_null("equip.jewelrytype", "Magic")
	if Bottle == null:
		_errors.enum_null("equip.jewelrytype", "Bottle")
# 内部存储
static var _data: Dictionary[String, DataEquip_Jewelrytype] = {}
# 静态枚举实例
static var Jade: DataEquip_Jewelrytype
static var Bracelet: DataEquip_Jewelrytype
static var Magic: DataEquip_Jewelrytype
static var Bottle: DataEquip_Jewelrytype
# 解析外键引用
# 字符串表示
func _to_string() -> String:
	return "DataEquip_Jewelrytype{" + typeName + "}"

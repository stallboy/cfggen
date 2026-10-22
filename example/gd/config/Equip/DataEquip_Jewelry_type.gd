class_name DataEquip_Jewelry_type
## equip.jewelry_type
# 公开属性
var typeName: String  # 程序用名字

# 静态枚举实例
static var Jade: DataEquip_Jewelry_type
static var Bracelet: DataEquip_Jewelry_type
static var Magic: DataEquip_Jewelry_type
static var Bottle: DataEquip_Jewelry_type
# 内部存储
static var _data: Dictionary[String, DataEquip_Jewelry_type] = {}
# 主键查询
static func find(key_0: String) -> DataEquip_Jewelry_type:
	return _data.get(key_0)
# 获取所有数据
static func all() -> Array[DataEquip_Jewelry_type]:
	return _data.values()

# 字符串表示
func _to_string() -> String:
	return "DataEquip_Jewelry_type{" + typeName + "}"

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	_data = {}
	Jade = null
	Bracelet = null
	Magic = null
	Bottle = null
	var count = stream.read_count(1)
	for i in range(count):
		var item = _create(stream)
		_data[item.typeName] = item
		if item.typeName.strip_edges() != "":
			match item.typeName.strip_edges():
				"Jade":
					if Jade != null:
						_errors.enum_dup("equip.jewelry_type", str(item))
					Jade = item
				"Bracelet":
					if Bracelet != null:
						_errors.enum_dup("equip.jewelry_type", str(item))
					Bracelet = item
				"Magic":
					if Magic != null:
						_errors.enum_dup("equip.jewelry_type", str(item))
					Magic = item
				"Bottle":
					if Bottle != null:
						_errors.enum_dup("equip.jewelry_type", str(item))
					Bottle = item
				_:
					_errors.enum_data_add("equip.jewelry_type", str(item))
	if Jade == null:
		_errors.enum_null("equip.jewelry_type", "Jade")
	if Bracelet == null:
		_errors.enum_null("equip.jewelry_type", "Bracelet")
	if Magic == null:
		_errors.enum_null("equip.jewelry_type", "Magic")
	if Bottle == null:
		_errors.enum_null("equip.jewelry_type", "Bottle")

# 创建实例
static func _create(stream: ConfigStream) -> DataEquip_Jewelry_type:
	var instance = DataEquip_Jewelry_type.new()
	instance.typeName = stream.read_string_in_pool()
	return instance



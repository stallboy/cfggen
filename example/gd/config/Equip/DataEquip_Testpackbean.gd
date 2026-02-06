class_name DataEquip_Testpackbean
## equip.TestPackBean
# 公开属性
var name: String:
	get:
		return name
var iRange: DataRange:
	get:
		return iRange
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataEquip_Testpackbean:
	var instance = DataEquip_Testpackbean.new()
	instance.name = stream.get_string()
	instance.iRange = DataRange.create(stream)
	return instance

# 解析外键引用

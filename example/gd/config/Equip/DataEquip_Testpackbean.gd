class_name DataEquip_Testpackbean
## equip.TestPackBean
# 公开属性
var name: String
var iRange: DataRange
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataEquip_Testpackbean:
	var instance = DataEquip_Testpackbean.new()
	instance.name = stream.read_string_in_pool()
	instance.iRange = DataRange.create(stream)
	return instance

# 解析外键引用

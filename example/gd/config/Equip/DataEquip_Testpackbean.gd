class_name DataEquip_Testpackbean
## equip.TestPackBean
# 公开属性
var name: String
var iRange: DataRange

# 字符串表示
func _to_string() -> String:
	return "DataEquip_Testpackbean{" + name + "," + str(iRange) + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataEquip_Testpackbean:
	var instance = DataEquip_Testpackbean.new()
	instance.name = stream.read_string_in_pool()
	instance.iRange = DataRange._create(stream)
	return instance



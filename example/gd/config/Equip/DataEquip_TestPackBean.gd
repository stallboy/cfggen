class_name DataEquip_TestPackBean
## equip.TestPackBean
# 公开属性
var name: String
var iRange: DataRange

# 字符串表示
func _to_string() -> String:
	return "DataEquip_TestPackBean{" + name + "," + str(iRange) + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataEquip_TestPackBean:
	var instance = DataEquip_TestPackBean.new()
	instance.name = stream.read_string_in_pool()
	instance.iRange = DataRange._create(stream)
	return instance



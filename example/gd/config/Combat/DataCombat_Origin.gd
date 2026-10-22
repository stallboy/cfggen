class_name DataCombat_Origin
## combat.Origin
# 公开属性
var x: int
var y: int

# 字符串表示
func _to_string() -> String:
	return "DataCombat_Origin{" + str(x) + "," + str(y) + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataCombat_Origin:
	var instance = DataCombat_Origin.new()
	instance.x = stream.read_int32()
	instance.y = stream.read_int32()
	return instance



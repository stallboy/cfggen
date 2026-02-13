class_name DataPosition
## Position
# 公开属性
var x: int
var y: int
var z: int

# 字符串表示
func _to_string() -> String:
	return "DataPosition{" + str(x) + "," + str(y) + "," + str(z) + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataPosition:
	var instance = DataPosition.new()
	instance.x = stream.read_int32()
	instance.y = stream.read_int32()
	instance.z = stream.read_int32()
	return instance

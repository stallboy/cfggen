class_name DataPosition
## Position
# 公开属性
var x: int:
	get:
		return x
var y: int:
	get:
		return y
var z: int:
	get:
		return z
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataPosition:
	var instance = DataPosition.new()
	instance.x = stream.get_32()
	instance.y = stream.get_32()
	instance.z = stream.get_32()
	return instance

# 解析外键引用

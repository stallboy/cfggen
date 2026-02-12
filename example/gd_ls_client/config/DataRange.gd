class_name DataRange
## Range
# 公开属性
var min: int:
	get:
		return min  # 最小
var max: int:
	get:
		return max  # 最大
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataRange:
	var instance = DataRange.new()
	instance.min = stream.get_32()
	instance.max = stream.get_32()
	return instance

# 解析外键引用

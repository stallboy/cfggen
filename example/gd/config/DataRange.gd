class_name DataRange
## Range
# 公开属性
var min: int  # 最小
var max: int  # 最大
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataRange:
	var instance = DataRange.new()
	instance.min = stream.read_int32()
	instance.max = stream.read_int32()
	return instance

# 解析外键引用

class_name DataRange
## Range
# 公开属性
var min: int  # 最小
var max: int  # 最大

# 字符串表示
func _to_string() -> String:
	return "DataRange{" + str(min) + "," + str(max) + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataRange:
	var instance = DataRange.new()
	instance.min = stream.read_int32()
	instance.max = stream.read_int32()
	return instance



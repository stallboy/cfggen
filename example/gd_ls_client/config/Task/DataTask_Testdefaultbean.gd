class_name DataTask_Testdefaultbean
## task.TestDefaultBean
# 公开属性
var testInt: int
var testBool: bool
var testString: String
var testSubBean: DataPosition
var testList: Array[int]
var testList2: Array[int]
var testMap: Dictionary[int, String]
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataTask_Testdefaultbean:
	var instance = DataTask_Testdefaultbean.new()
	instance.testInt = stream.read_int32()
	instance.testBool = stream.read_bool()
	instance.testString = stream.read_string_in_pool()
	instance.testSubBean = DataPosition.create(stream)
	for c in range(stream.read_int32()):
		instance.testList.append(stream.read_int32())
	for c in range(stream.read_int32()):
		instance.testList2.append(stream.read_int32())
	for c in range(stream.read_int32()):
		var k = stream.read_int32()
		var v = stream.read_string_in_pool()
		instance.testMap[k] = v
	return instance

# 解析外键引用
# 字符串表示
func _to_string() -> String:
	return "DataTask_Testdefaultbean{" + str(testInt) + "," + str(testBool) + "," + testString + "," + str(testSubBean) + "," + str(testList) + "," + str(testList2) + "," + str(testMap) + "}"

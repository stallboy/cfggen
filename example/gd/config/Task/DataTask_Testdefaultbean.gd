class_name DataTask_Testdefaultbean
## task.TestDefaultBean
# 公开属性
var testInt: int:
	get:
		return testInt
var testBool: bool:
	get:
		return testBool
var testString: String:
	get:
		return testString
var testSubBean: DataPosition:
	get:
		return testSubBean
var testList: Array[int]:
	get:
		return testList
var testList2: Array[int]:
	get:
		return testList2
var testMap: Dictionary[int, String]:
	get:
		return testMap
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataTask_Testdefaultbean:
	var instance = DataTask_Testdefaultbean.new()
	instance.testInt = stream.get_32()
	instance.testBool = stream.get_bool()
	instance.testString = stream.get_string()
	instance.testSubBean = DataPosition.create(stream)
	instance.testList = []
	for c in range(stream.get_32()):
		instance.testList.append(stream.get_32())
	instance.testList2 = []
	for c in range(stream.get_32()):
		instance.testList2.append(stream.get_32())
	instance.testMap = {}
	for c in range(stream.get_32()):
		var k = stream.get_32()
		var v = stream.get_string()
		instance.testMap[k] = v
	return instance

# 解析外键引用

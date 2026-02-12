class_name DataTaskCompletecondition_Killmonster extends DataTask_Completecondition
## KillMonster
# 公开属性
var monsterid: int
var count: int
# 外键引用属性
var RefMonsterid: DataOther_Monster
# 创建实例
static func create(stream: ConfigStream) -> DataTaskCompletecondition_Killmonster:
	var instance = DataTaskCompletecondition_Killmonster.new()
	instance.monsterid = stream.read_int32()
	instance.count = stream.read_int32()
	return instance

# 解析外键引用
func _resolve(errors: ConfigErrors):
	RefMonsterid = DataOther_Monster.find(monsterid)
	if RefMonsterid == null:
		errors.ref_null("KillMonster", "monsterid")
# 字符串表示
func _to_string() -> String:
	return "DataTaskCompletecondition_Killmonster{" + str(monsterid) + "," + str(count) + "}"

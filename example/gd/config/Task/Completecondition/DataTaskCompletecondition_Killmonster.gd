class_name DataTaskCompletecondition_Killmonster extends DataTask_Completecondition
## KillMonster
# 公开属性
var monsterid: int:
	get:
		return monsterid
var count: int:
	get:
		return count
# 外键引用属性
var MonsteridRef: DataOther_Monster:
	get:
		return MonsteridRef
# 创建实例
static func create(stream: ConfigStream) -> DataTaskCompletecondition_Killmonster:
	var instance = DataTaskCompletecondition_Killmonster.new()
	instance.monsterid = stream.get_32()
	instance.count = stream.get_32()
	return instance

# 解析外键引用
func _resolve(errors: ConfigErrors):
	MonsteridRef = DataOther_Monster.find(monsterid)
	if MonsteridRef == null:
		errors.ref_null("KillMonster", "monsterid")

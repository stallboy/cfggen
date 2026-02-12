class_name DataAi_Ai
## ai.ai
# 公开属性
var iD: int:
	get:
		return iD
var desc: String:
	get:
		return desc  # 描述----这里测试下多行效果--再来一行
var condID: String:
	get:
		return condID  # 触发公式
var trigTick: DataAi_Triggertick:
	get:
		return trigTick  # 触发间隔(帧)
var trigOdds: int:
	get:
		return trigOdds  # 触发几率
var actionID: Array[int]:
	get:
		return actionID  # 触发行为
var deathRemove: bool:
	get:
		return deathRemove  # 死亡移除
# 外键引用属性
# 创建实例
static func create(stream: ConfigStream) -> DataAi_Ai:
	var instance = DataAi_Ai.new()
	instance.iD = stream.get_32()
	instance.desc = stream.read_string_in_pool()
	instance.condID = stream.read_string_in_pool()
	instance.trigTick = DataAi_Triggertick.create(stream)
	instance.trigOdds = stream.get_32()
	instance.actionID = []
	for c in range(stream.get_32()):
		instance.actionID.append(stream.get_32())
	instance.deathRemove = stream.get_bool()
	return instance

# 主键查询
static func find(id: int) -> DataAi_Ai:
	return _data.get(id)

# 获取所有数据
static func all() -> Array[DataAi_Ai]:
	return _data.values()

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	var count = stream.get_32()
	for i in range(count):
		var item = create(stream)
		_data[item.iD] = item
# 内部存储
static var _data: Dictionary[int, DataAi_Ai] = {}
# 解析外键引用

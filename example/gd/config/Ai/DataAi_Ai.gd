class_name DataAi_Ai
## ai.ai
# 公开属性
var iD: int
var desc: String  # 描述----这里测试下多行效果--再来一行
var condID: String  # 触发公式
var trigTick: DataAi_Triggertick  # 触发间隔(帧)
var trigOdds: int  # 触发几率
var actionID: Array[int]  # 触发行为
var deathRemove: bool  # 死亡移除

# 内部存储
static var _data: Dictionary[int, DataAi_Ai] = {}
# 主键查询
static func find(id: int) -> DataAi_Ai:
	return _data.get(id)
# 获取所有数据
static func all() -> Array[DataAi_Ai]:
	return _data.values()

# 字符串表示
func _to_string() -> String:
	return "DataAi_Ai{" + str(iD) + "," + desc + "," + condID + "," + str(trigTick) + "," + str(trigOdds) + "," + str(actionID) + "," + str(deathRemove) + "}"

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	var count = stream.read_int32()
	for i in range(count):
		var item = _create(stream)
		_data[item.iD] = item

# 创建实例
static func _create(stream: ConfigStream) -> DataAi_Ai:
	var instance = DataAi_Ai.new()
	instance.iD = stream.read_int32()
	instance.desc = stream.read_string_in_pool()
	instance.condID = stream.read_string_in_pool()
	instance.trigTick = DataAi_Triggertick._create(stream)
	instance.trigOdds = stream.read_int32()
	for c in range(stream.read_int32()):
		instance.actionID.append(stream.read_int32())
	instance.deathRemove = stream.read_bool()
	return instance

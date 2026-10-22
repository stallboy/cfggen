class_name DataCombat_Branchreward
## combat.branchreward
# 公开属性
var key: DataCombat_PhaseKey
var reward: DataCombat_Reward

# 内部存储
static var _data: Dictionary[String, DataCombat_Branchreward] = {}
# 主键查询
static func find(key_0: DataCombat_PhaseKey) -> DataCombat_Branchreward:
	return _data.get(ConfigProcessor._object_key([key_0]))
# 获取所有数据
static func all() -> Array[DataCombat_Branchreward]:
	return _data.values()

# 字符串表示
func _to_string() -> String:
	return "DataCombat_Branchreward{" + str(key) + "," + str(reward) + "}"

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	_data = {}
	var count = stream.read_count(1)
	for i in range(count):
		var item = _create(stream)
		_data[ConfigProcessor._object_key([item.key])] = item

# 创建实例
static func _create(stream: ConfigStream) -> DataCombat_Branchreward:
	var instance = DataCombat_Branchreward.new()
	instance.key = DataCombat_PhaseKey._create(stream)
	instance.reward = DataCombat_Reward._create(stream)
	return instance



class_name DataLevelrank
## LevelRank
# 公开属性
var level: int:
	get:
		return level  # 等级
var rank: int:
	get:
		return rank  # 品质
# 外键引用属性
var RankRef: DataEquip_Rank:
	get:
		return RankRef
# 创建实例
static func create(stream: ConfigStream) -> DataLevelrank:
	var instance = DataLevelrank.new()
	instance.level = stream.get_32()
	instance.rank = stream.get_32()
	return instance

# 解析外键引用
func _resolve(errors: ConfigErrors):
	RankRef = DataEquip_Rank.find(rank)
	if RankRef == null:
		errors.ref_null("LevelRank", "Rank")

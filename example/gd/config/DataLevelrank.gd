class_name DataLevelrank
## LevelRank
# 公开属性
var level: int  # 等级
var rank: int  # 品质
# 外键引用属性
var RefRank: DataEquip_Rank

# 字符串表示
func _to_string() -> String:
	return "DataLevelrank{" + str(level) + "," + str(rank) + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataLevelrank:
	var instance = DataLevelrank.new()
	instance.level = stream.read_int32()
	instance.rank = stream.read_int32()
	return instance


# 解析外键引用
func _resolve(errors: ConfigErrors):
	RefRank = DataEquip_Rank.find(rank)
	if RefRank == null:
		errors.ref_null("LevelRank", "Rank")


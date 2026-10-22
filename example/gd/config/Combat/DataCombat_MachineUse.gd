class_name DataCombat_MachineUse
## combat.MachineUse
# 公开属性
var machineKey: String
var targets: Array[DataCombat_PhaseUse]

# 字符串表示
func _to_string() -> String:
	return "DataCombat_MachineUse{" + machineKey + "," + str(targets) + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataCombat_MachineUse:
	var instance = DataCombat_MachineUse.new()
	instance.machineKey = stream.read_string_in_pool()
	for c in range(stream.read_count(0)):
		instance.targets.append(DataCombat_PhaseUse._create(stream))
	return instance


# 解析外键引用
func _resolve(errors: ConfigErrors):
	pass
	for item in targets:
		if item != null:
			item._resolve(errors)


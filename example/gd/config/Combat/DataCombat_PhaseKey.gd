class_name DataCombat_PhaseKey
## combat.PhaseKey
## 内部引用验收：技能状态机、命中窗口、事件时间轴与连招分支。
# 公开属性
var name: String
var variant: int

# 字符串表示
func _to_string() -> String:
	return "DataCombat_PhaseKey{" + name + "," + str(variant) + "}"

# 创建实例
func _init():
	set_meta(&"_cfggen_key_fields", ["name", "variant"])
	set_meta(&"_cfggen_key_type", "combat.PhaseKey")

static func _create(stream: ConfigStream) -> DataCombat_PhaseKey:
	var instance = DataCombat_PhaseKey.new()
	instance.name = stream.read_string_in_pool()
	instance.variant = stream.read_int32()
	return instance



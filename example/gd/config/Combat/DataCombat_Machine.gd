class_name DataCombat_Machine
## combat.Machine
# 公开属性
var code: String
var phases: Array[DataCombat_Phase]
var impacts: Array[DataCombat_Impact]
var frames: Array[DataCombat_Frame]
var stances: Array[DataCombat_Stance]
var variants: Array[DataCombat_PhaseVariant]
var combos: Array[DataCombat_Combo]
var markers: Array[DataCombat_Marker]  # 未被引用也必须保持唯一。
var anchor: DataCombat_Anchor
var anchors: Array[DataCombat_AnchorUse]
var onExit: DataCombat_Exit
var initial: String
var impact: int
var frame: int
var stance: bool
var variant: DataCombat_PhaseKey
var weapon: String
var tier: int
var airborne: bool
# 外键引用属性
var RefInitial: DataCombat_Phase:
	get:
		if not has_meta(&"_cfggen_RefInitial"):
			return null
		var target: WeakRef = get_meta(&"_cfggen_RefInitial") as WeakRef
		return target.get_ref() as DataCombat_Phase if target != null else null
	set(value):
		if value != null:
			set_meta(&"_cfggen_RefInitial", weakref(value))
		elif has_meta(&"_cfggen_RefInitial"):
			remove_meta(&"_cfggen_RefInitial")
var RefImpact: DataCombat_Impact:
	get:
		if not has_meta(&"_cfggen_RefImpact"):
			return null
		var target: WeakRef = get_meta(&"_cfggen_RefImpact") as WeakRef
		return target.get_ref() as DataCombat_Impact if target != null else null
	set(value):
		if value != null:
			set_meta(&"_cfggen_RefImpact", weakref(value))
		elif has_meta(&"_cfggen_RefImpact"):
			remove_meta(&"_cfggen_RefImpact")
var RefFrame: DataCombat_Frame:
	get:
		if not has_meta(&"_cfggen_RefFrame"):
			return null
		var target: WeakRef = get_meta(&"_cfggen_RefFrame") as WeakRef
		return target.get_ref() as DataCombat_Frame if target != null else null
	set(value):
		if value != null:
			set_meta(&"_cfggen_RefFrame", weakref(value))
		elif has_meta(&"_cfggen_RefFrame"):
			remove_meta(&"_cfggen_RefFrame")
var RefStance: DataCombat_Stance:
	get:
		if not has_meta(&"_cfggen_RefStance"):
			return null
		var target: WeakRef = get_meta(&"_cfggen_RefStance") as WeakRef
		return target.get_ref() as DataCombat_Stance if target != null else null
	set(value):
		if value != null:
			set_meta(&"_cfggen_RefStance", weakref(value))
		elif has_meta(&"_cfggen_RefStance"):
			remove_meta(&"_cfggen_RefStance")
var RefVariant: DataCombat_PhaseVariant:
	get:
		if not has_meta(&"_cfggen_RefVariant"):
			return null
		var target: WeakRef = get_meta(&"_cfggen_RefVariant") as WeakRef
		return target.get_ref() as DataCombat_PhaseVariant if target != null else null
	set(value):
		if value != null:
			set_meta(&"_cfggen_RefVariant", weakref(value))
		elif has_meta(&"_cfggen_RefVariant"):
			remove_meta(&"_cfggen_RefVariant")
var RefCombo: DataCombat_Combo:
	get:
		if not has_meta(&"_cfggen_RefCombo"):
			return null
		var target: WeakRef = get_meta(&"_cfggen_RefCombo") as WeakRef
		return target.get_ref() as DataCombat_Combo if target != null else null
	set(value):
		if value != null:
			set_meta(&"_cfggen_RefCombo", weakref(value))
		elif has_meta(&"_cfggen_RefCombo"):
			remove_meta(&"_cfggen_RefCombo")

# 字符串表示
func _to_string() -> String:
	return "DataCombat_Machine{" + code + "," + str(phases) + "," + str(impacts) + "," + str(frames) + "," + str(stances) + "," + str(variants) + "," + str(combos) + "," + str(markers) + "," + str(anchor) + "," + str(anchors) + "," + str(onExit) + "," + initial + "," + str(impact) + "," + str(frame) + "," + str(stance) + "," + str(variant) + "," + weapon + "," + str(tier) + "," + str(airborne) + "}"

# 创建实例
static func _create(stream: ConfigStream) -> DataCombat_Machine:
	var instance = DataCombat_Machine.new()
	instance.code = stream.read_string_in_pool()
	for c in range(stream.read_count(0)):
		instance.phases.append(DataCombat_Phase._create(stream))
	for c in range(stream.read_count(0)):
		instance.impacts.append(DataCombat_Impact._create(stream))
	for c in range(stream.read_count(0)):
		instance.frames.append(DataCombat_Frame._create(stream))
	for c in range(stream.read_count(0)):
		instance.stances.append(DataCombat_Stance._create(stream))
	for c in range(stream.read_count(0)):
		instance.variants.append(DataCombat_PhaseVariant._create(stream))
	for c in range(stream.read_count(0)):
		instance.combos.append(DataCombat_Combo._create(stream))
	for c in range(stream.read_count(0)):
		instance.markers.append(DataCombat_Marker._create(stream))
	instance.anchor = DataCombat_Anchor._create(stream)
	for c in range(stream.read_count(0)):
		instance.anchors.append(DataCombat_AnchorUse._create(stream))
	instance.onExit = DataCombat_Exit._create(stream)
	instance.initial = stream.read_string_in_pool()
	instance.impact = stream.read_int32()
	instance.frame = stream.read_int64()
	instance.stance = stream.read_bool()
	instance.variant = DataCombat_PhaseKey._create(stream)
	instance.weapon = stream.read_string_in_pool()
	instance.tier = stream.read_int32()
	instance.airborne = stream.read_bool()
	return instance


# 解析外键引用
func _resolve(errors: ConfigErrors):
	pass
	for item in phases:
		if item != null:
			item._resolve(errors)
	for item in anchors:
		if item != null:
			item._resolve(errors)


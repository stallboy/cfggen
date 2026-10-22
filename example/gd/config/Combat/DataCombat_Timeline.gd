class_name DataCombat_Timeline
## combat.timeline
## CSV pack 映射：五种 map 键、多态 value、无键的 interface 固定终点。
# 公开属性
var id: int
var events: Dictionary[DataCombat_PhaseKey, DataCombat_Effect]
var channels: Dictionary[String, DataCombat_Effect]
var slots: Dictionary[int, DataCombat_Effect]
var clock: Dictionary[int, DataCombat_Effect]
var modes: Dictionary[bool, DataCombat_Effect]
var eventKey: DataCombat_PhaseKey
var channel: String
var slot: int
var frame: int
var airborne: bool
var fallback: DataCombat_Effect
# 外键引用属性
var RefEventKey: DataCombat_Effect:
	get:
		if not has_meta(&"_cfggen_RefEventKey"):
			return null
		var target: WeakRef = get_meta(&"_cfggen_RefEventKey") as WeakRef
		return target.get_ref() as DataCombat_Effect if target != null else null
	set(value):
		if value != null:
			set_meta(&"_cfggen_RefEventKey", weakref(value))
		elif has_meta(&"_cfggen_RefEventKey"):
			remove_meta(&"_cfggen_RefEventKey")
var RefChannel: DataCombat_Effect:
	get:
		if not has_meta(&"_cfggen_RefChannel"):
			return null
		var target: WeakRef = get_meta(&"_cfggen_RefChannel") as WeakRef
		return target.get_ref() as DataCombat_Effect if target != null else null
	set(value):
		if value != null:
			set_meta(&"_cfggen_RefChannel", weakref(value))
		elif has_meta(&"_cfggen_RefChannel"):
			remove_meta(&"_cfggen_RefChannel")
var RefSlot: DataCombat_Effect:
	get:
		if not has_meta(&"_cfggen_RefSlot"):
			return null
		var target: WeakRef = get_meta(&"_cfggen_RefSlot") as WeakRef
		return target.get_ref() as DataCombat_Effect if target != null else null
	set(value):
		if value != null:
			set_meta(&"_cfggen_RefSlot", weakref(value))
		elif has_meta(&"_cfggen_RefSlot"):
			remove_meta(&"_cfggen_RefSlot")
var RefFrame: DataCombat_Effect:
	get:
		if not has_meta(&"_cfggen_RefFrame"):
			return null
		var target: WeakRef = get_meta(&"_cfggen_RefFrame") as WeakRef
		return target.get_ref() as DataCombat_Effect if target != null else null
	set(value):
		if value != null:
			set_meta(&"_cfggen_RefFrame", weakref(value))
		elif has_meta(&"_cfggen_RefFrame"):
			remove_meta(&"_cfggen_RefFrame")
var RefAirborne: DataCombat_Effect:
	get:
		if not has_meta(&"_cfggen_RefAirborne"):
			return null
		var target: WeakRef = get_meta(&"_cfggen_RefAirborne") as WeakRef
		return target.get_ref() as DataCombat_Effect if target != null else null
	set(value):
		if value != null:
			set_meta(&"_cfggen_RefAirborne", weakref(value))
		elif has_meta(&"_cfggen_RefAirborne"):
			remove_meta(&"_cfggen_RefAirborne")
var RefDefault: DataCombat_Effect:
	get:
		if not has_meta(&"_cfggen_RefDefault"):
			return null
		var target: WeakRef = get_meta(&"_cfggen_RefDefault") as WeakRef
		return target.get_ref() as DataCombat_Effect if target != null else null
	set(value):
		if value != null:
			set_meta(&"_cfggen_RefDefault", weakref(value))
		elif has_meta(&"_cfggen_RefDefault"):
			remove_meta(&"_cfggen_RefDefault")

# 内部存储
static var _data: Dictionary[int, DataCombat_Timeline] = {}
# 主键查询
static func find(key_0: int) -> DataCombat_Timeline:
	return _data.get(key_0)
# 获取所有数据
static func all() -> Array[DataCombat_Timeline]:
	return _data.values()

# 字符串表示
func _to_string() -> String:
	return "DataCombat_Timeline{" + str(id) + "," + str(events) + "," + str(channels) + "," + str(slots) + "," + str(clock) + "," + str(modes) + "," + str(eventKey) + "," + channel + "," + str(slot) + "," + str(frame) + "," + str(airborne) + "," + str(fallback) + "}"

# 从流初始化
static func _init_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	_data = {}
	var count = stream.read_count(1)
	for i in range(count):
		var item = _create(stream)
		_data[item.id] = item

# 创建实例
static func _create(stream: ConfigStream) -> DataCombat_Timeline:
	var instance = DataCombat_Timeline.new()
	instance.id = stream.read_int32()
	for c in range(stream.read_count(0)):
		var k = DataCombat_PhaseKey._create(stream)
		var v = DataCombat_Effect._create(stream)
		if instance.events.has(k):
			stream.fail("Duplicate map key: combat.timeline.events")
			return instance
		instance.events[k] = v
	for c in range(stream.read_count(0)):
		var k = stream.read_string_in_pool()
		var v = DataCombat_Effect._create(stream)
		if instance.channels.has(k):
			stream.fail("Duplicate map key: combat.timeline.channels")
			return instance
		instance.channels[k] = v
	for c in range(stream.read_count(0)):
		var k = stream.read_int32()
		var v = DataCombat_Effect._create(stream)
		if instance.slots.has(k):
			stream.fail("Duplicate map key: combat.timeline.slots")
			return instance
		instance.slots[k] = v
	for c in range(stream.read_count(0)):
		var k = stream.read_int64()
		var v = DataCombat_Effect._create(stream)
		if instance.clock.has(k):
			stream.fail("Duplicate map key: combat.timeline.clock")
			return instance
		instance.clock[k] = v
	for c in range(stream.read_count(0)):
		var k = stream.read_bool()
		var v = DataCombat_Effect._create(stream)
		if instance.modes.has(k):
			stream.fail("Duplicate map key: combat.timeline.modes")
			return instance
		instance.modes[k] = v
	instance.eventKey = DataCombat_PhaseKey._create(stream)
	instance.channel = stream.read_string_in_pool()
	instance.slot = stream.read_int32()
	instance.frame = stream.read_int64()
	instance.airborne = stream.read_bool()
	instance.fallback = DataCombat_Effect._create(stream)
	return instance


# 解析外键引用
func _resolve(errors: ConfigErrors):
	pass

static func _resolve_refs(errors: ConfigErrors):
	for item in all():
		item._resolve(errors)

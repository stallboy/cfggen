class_name DataCombat_AnchorUse
## combat.AnchorUse
# 公开属性
# 外键引用属性
var RefAnchor: DataCombat_Anchor:
	get:
		if not has_meta(&"_cfggen_RefAnchor"):
			return null
		var target: WeakRef = get_meta(&"_cfggen_RefAnchor") as WeakRef
		return target.get_ref() as DataCombat_Anchor if target != null else null
	set(value):
		if value != null:
			set_meta(&"_cfggen_RefAnchor", weakref(value))
		elif has_meta(&"_cfggen_RefAnchor"):
			remove_meta(&"_cfggen_RefAnchor")

# 字符串表示
func _to_string() -> String:
	return "DataCombat_AnchorUse{}"

# 创建实例
static func _create(stream: ConfigStream) -> DataCombat_AnchorUse:
	var instance = DataCombat_AnchorUse.new()
	return instance


# 解析外键引用
func _resolve(errors: ConfigErrors):
	pass


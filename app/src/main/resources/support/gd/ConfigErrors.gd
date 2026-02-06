class_name ConfigErrors

## 配置加载错误收集器

var _errors: Array[Dictionary] = []
var _warns: Array[Dictionary] = []

# 添加错误
func error(msg: String):
	_errors.append({"type": "Error", "message": msg})
	print("ERROR: ", msg)

# 添加警告
func warn(msg: String):
	_warns.append({"type": "Warn", "message": msg})
	print("WARN: ", msg)

# 配置表缺失
func config_null(config: String):
	warn("配置表缺失: " + config)

# 配置数据多余
func config_data_add(config: String):
	warn("配置数据多余: " + config)

# 枚举数据错误
func enum_data_add(config: String, record: String):
	warn("枚举数据错误: " + config + ", " + record)

# 枚举重复
func enum_dup(config: String, record: String):
	error("枚举重复: " + config + ", " + record)

# 枚举缺失
func enum_null(config: String, enum_name: String):
	error("枚举缺失: " + config + ", " + enum_name)

# 外键引用为空
func ref_null(config: String, field: String):
	error("外键引用为空: " + config + ", " + field)

# 检查是否有错误
func has_errors() -> bool:
	return _errors.size() > 0

# 检查是否有警告
func has_warns() -> bool:
	return _warns.size() > 0

# 获取所有消息（错误+警告）
func get_messages() -> Array:
	var all = []
	all.append_array(_errors)
	all.append_array(_warns)
	return all

# 获取错误数量
func get_error_count() -> int:
	return _errors.size()

# 获取警告数量
func get_warn_count() -> int:
	return _warns.size()

# 打印所有消息
func print_all():
	for msg in _errors:
		print("ERROR: ", msg.message)
	for msg in _warns:
		print("WARN: ", msg.message)

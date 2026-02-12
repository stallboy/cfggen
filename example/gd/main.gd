extends Control

# UI 引用
var table_option: OptionButton
var id_option: OptionButton
var result_text: TextEdit

# 配置是否已加载
var _config_loaded: bool = false

# 数据表映射
var _tables: Dictionary = {
	"Task": {
		"data_func": func(): return DataTask_Task.all(),
		"find_func": func(id): return DataTask_Task.find(id),
		"id_field": "taskid",
		"name_field": "name"
	},
	"Equip": {
		"data_func": func(): return DataEquip_Ability.all(),
		"find_func": func(id): return DataEquip_Ability.find(id),
		"id_field": "id",
		"name_field": "name"
	},
	"Ai": {
		"data_func": func(): return DataAi_Ai.all(),
		"find_func": func(id): return DataAi_Ai.find(id),
		"id_field": "iD",
		"name_field": "desc"
	},
	"Monster": {
		"data_func": func(): return DataOther_Monster.all(),
		"find_func": func(id): return DataOther_Monster.find(id),
		"id_field": "id",
		"name_field": ""
	}
}

var _current_table: String = ""
var _current_records: Array = []

func _ready():
	print("========================================")
	print("_ready() 开始执行")
	print("========================================")

	# 获取 UI 节点引用
	table_option = $VBoxContainer/TableOption
	id_option = $VBoxContainer/IdOption
	result_text = $VBoxContainer/ResultText

	print("UI 节点引用获取完成")

	if ConfigErrors == null:
		push_error("ConfigErrors 加载失败！")
		return
	print("配置类加载完成")

	# 1. 先加载配置
	_load_config()

	if not _config_loaded:
		result_text.text = "配置加载失败，请检查控制台输出"
		return

	# 2. 填充表选项
	_populate_tables()

	# 3. 连接信号
	table_option.item_selected.connect(_on_table_selected)
	id_option.item_selected.connect(_on_id_selected)

	print("========================================")
	print("_ready() 执行完成")
	print("========================================")

func _load_config():
	"""加载配置数据"""
	print("开始加载配置...")

	var file = FileAccess.open("res://config.bytes", FileAccess.READ)
	if file == null:
		push_error("无法打开配置文件: res://config.bytes")
		result_text.text = "错误：无法打开配置文件"
		return

	var bytes = file.get_buffer(file.get_length())
	file.close()
	print("配置文件大小: %d bytes" % bytes.size())

	# 创建错误收集器
	var errors = ConfigErrors.new()

	# 创建配置处理器
	var processor = ConfigProcessor.new()

	# 加载配置数据
	ConfigLoader.load_bytes(bytes, processor.load_from_stream, errors)

	print("配置加载完成！")

	# 检查错误
	if errors.get_error_count() > 0:
		print("发现 %d 个错误:" % errors.get_error_count())
		errors.print_all()
	else:
		print("无错误")

	if errors.get_warn_count() > 0:
		print("发现 %d 个警告:" % errors.get_warn_count())
		for msg in errors._warns:
			print("WARN: ", msg)

	_config_loaded = true

func _populate_tables():
	"""填充表下拉选项"""
	table_option.clear()
	table_option.add_item("请选择 Table")
	for table_name in _tables.keys():
		table_option.add_item(table_name)
	table_option.selected = 0

func _on_table_selected(index: int):
	"""表选择变化时更新 ID 列表"""
	if index <= 0:  # 选择了"请选择 Table"
		_current_table = ""
		_current_records = []
		id_option.clear()
		result_text.text = "请先选择一个表"
		return

	var table_names = _tables.keys()
	var table_index = index - 1  # 减 1 因为第一项是"请选择 Table"
	if table_index < 0 or table_index >= table_names.size():
		return

	_current_table = table_names[table_index]
	var table_info = _tables[_current_table]

	# 获取该表的所有记录
	_current_records = table_info.data_func.call()
	print("表 %s 有 %d 条记录" % [_current_table, _current_records.size()])

	# 更新 ID 选项
	id_option.clear()
	id_option.add_item("请选择 ID")
	for record in _current_records:
		var id_str = str(_get_record_id(record))
		var name_str = _get_record_name(record)
		if name_str != "":
			id_option.add_item("%s: %s" % [id_str, name_str])
		else:
			id_option.add_item("%s" % [id_str])
	id_option.selected = 0

	# 清空结果显示
	result_text.text = "请选择一条记录查看详情"

func _on_id_selected(index: int):
	"""ID 选择变化时显示详情"""
	if index <= 0 or _current_records.is_empty():
		result_text.text = "请选择一条记录查看详情"
		return

	var record = _current_records[index - 1]  # -1 因为第一项是"请选择"
	result_text.text = str(record)  # 直接使用 toString()

func _get_record_id(record) -> int:
	"""获取记录的 ID 字段"""
	if _current_table.is_empty():
		return 0
	var table_info = _tables[_current_table]
	var id_field = table_info.id_field
	# 使用反射获取字段值
	if record.has_method("get"):
		return record.get(id_field)
	return 0

func _get_record_name(record) -> String:
	"""获取记录的名称字段"""
	if _current_table.is_empty():
		return ""
	var table_info = _tables[_current_table]
	var name_field = table_info.name_field
	if name_field == "":
		return ""
	# 使用反射获取字段值
	if record.has_method("get"):
		var value = record.get(name_field)
		if value is Array:
			return value[0] if value.size() > 0 else ""
		elif value is String:
			return value
		return str(value)
	return ""

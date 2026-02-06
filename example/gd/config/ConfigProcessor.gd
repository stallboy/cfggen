class_name ConfigProcessor

## 配置处理器，负责加载所有配置表

var _errors: ConfigErrors

func _init():
	_errors = ConfigErrors.new()

func get_errors() -> ConfigErrors:
	return _errors

# 从流加载所有配置
func load_from_stream(stream: ConfigStream):
	var config_tables = {}
	config_tables["ai.ai"] = false
	config_tables["ai.ai_action"] = false
	config_tables["ai.ai_condition"] = false
	config_tables["equip.ability"] = false
	config_tables["equip.equipconfig"] = false
	config_tables["equip.jewelry"] = false
	config_tables["equip.jewelryrandom"] = false
	config_tables["equip.jewelrysuit"] = false
	config_tables["equip.jewelrytype"] = false
	config_tables["equip.rank"] = false
	config_tables["other.drop"] = false
	config_tables["other.keytest"] = false
	config_tables["other.loot"] = false
	config_tables["other.lootitem"] = false
	config_tables["other.monster"] = false
	config_tables["other.signin"] = false
	config_tables["task.completeconditiontype"] = false
	config_tables["task.task"] = false
	config_tables["task.task2"] = false
	config_tables["task.taskextraexp"] = false
	while true:
		var csv_name = stream.get_string()
		if csv_name.is_empty():
			break

		match csv_name:
			"ai.ai":
				config_tables["ai.ai"] = true
				DataAi_Ai._init_from_stream(stream)
			"ai.ai_action":
				config_tables["ai.ai_action"] = true
				DataAi_Ai_action._init_from_stream(stream)
			"ai.ai_condition":
				config_tables["ai.ai_condition"] = true
				DataAi_Ai_condition._init_from_stream(stream)
			"equip.ability":
				config_tables["equip.ability"] = true
				DataEquip_Ability._init_from_stream(stream)
			"equip.equipconfig":
				config_tables["equip.equipconfig"] = true
				DataEquip_Equipconfig._init_from_stream(stream)
			"equip.jewelry":
				config_tables["equip.jewelry"] = true
				DataEquip_Jewelry._init_from_stream(stream)
			"equip.jewelryrandom":
				config_tables["equip.jewelryrandom"] = true
				DataEquip_Jewelryrandom._init_from_stream(stream)
			"equip.jewelrysuit":
				config_tables["equip.jewelrysuit"] = true
				DataEquip_Jewelrysuit._init_from_stream(stream)
			"equip.jewelrytype":
				config_tables["equip.jewelrytype"] = true
				DataEquip_Jewelrytype._init_from_stream(stream)
			"equip.rank":
				config_tables["equip.rank"] = true
				DataEquip_Rank._init_from_stream(stream)
			"other.drop":
				config_tables["other.drop"] = true
				DataOther_Drop._init_from_stream(stream)
			"other.keytest":
				config_tables["other.keytest"] = true
				DataOther_Keytest._init_from_stream(stream)
			"other.loot":
				config_tables["other.loot"] = true
				DataOther_Loot._init_from_stream(stream)
			"other.lootitem":
				config_tables["other.lootitem"] = true
				DataOther_Lootitem._init_from_stream(stream)
			"other.monster":
				config_tables["other.monster"] = true
				DataOther_Monster._init_from_stream(stream)
			"other.signin":
				config_tables["other.signin"] = true
				DataOther_Signin._init_from_stream(stream)
			"task.completeconditiontype":
				config_tables["task.completeconditiontype"] = true
				DataTask_Completeconditiontype._init_from_stream(stream)
			"task.task":
				config_tables["task.task"] = true
				DataTask_Task._init_from_stream(stream)
			"task.task2":
				config_tables["task.task2"] = true
				DataTask_Task2._init_from_stream(stream)
			"task.taskextraexp":
				config_tables["task.taskextraexp"] = true
				DataTask_Taskextraexp._init_from_stream(stream)
			_:
				_errors.error("未知配置表: " + csv_name)
				# 跳过未知表的数据
				_skip_table_data(stream)

	# 检查缺失的配置表
	for table_name in config_tables.keys():
		if not config_tables[table_name]:
			_errors.error("配置表缺失: " + table_name)

	# 解析外键引用
	DataEquip_Jewelry._resolve_refs(_errors)
	DataEquip_Jewelryrandom._resolve_refs(_errors)
	DataOther_Keytest._resolve_refs(_errors)
	DataOther_Loot._resolve_refs(_errors)
	DataOther_Monster._resolve_refs(_errors)
	DataOther_Signin._resolve_refs(_errors)
	DataTask_Task._resolve_refs(_errors)
	DataTask_Task2._resolve_refs(_errors)
# 跳过表数据（用于未知表）
func _skip_table_data(stream: ConfigStream):
	var count = stream.get_32()
	for i in range(count):
		# 跳过一行数据（简化处理，假设只有一列）
		stream.get_string()

# 检查是否有错误
func has_errors() -> bool:
	return _errors.has_errors()

# 获取错误消息
func get_error_messages() -> Array:
	return _errors.get_messages()

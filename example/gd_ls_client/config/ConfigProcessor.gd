class_name ConfigProcessor

# 配置处理器，负责加载所有配置表
# 从流加载所有配置
func load_from_stream(stream: ConfigStream, _errors: ConfigErrors) -> Array[String]:
	var config_tables = {}
	var config_table_names: Array[String] = []
	config_tables["ai.ai"] = false
	config_table_names.append("ai.ai")
	config_tables["ai.ai_action"] = false
	config_table_names.append("ai.ai_action")
	config_tables["ai.ai_condition"] = false
	config_table_names.append("ai.ai_condition")
	config_tables["equip.ability"] = false
	config_table_names.append("equip.ability")
	config_tables["equip.equipconfig"] = false
	config_table_names.append("equip.equipconfig")
	config_tables["equip.jewelry"] = false
	config_table_names.append("equip.jewelry")
	config_tables["equip.jewelryrandom"] = false
	config_table_names.append("equip.jewelryrandom")
	config_tables["equip.jewelrysuit"] = false
	config_table_names.append("equip.jewelrysuit")
	config_tables["equip.jewelrytype"] = false
	config_table_names.append("equip.jewelrytype")
	config_tables["equip.rank"] = false
	config_table_names.append("equip.rank")
	config_tables["other.drop"] = false
	config_table_names.append("other.drop")
	config_tables["other.loot"] = false
	config_table_names.append("other.loot")
	config_tables["other.monster"] = false
	config_table_names.append("other.monster")
	config_tables["other.signin"] = false
	config_table_names.append("other.signin")
	config_tables["task.completeconditiontype"] = false
	config_table_names.append("task.completeconditiontype")
	config_tables["task.task"] = false
	config_table_names.append("task.task")
	config_tables["task.task2"] = false
	config_table_names.append("task.task2")
	config_tables["task.taskextraexp"] = false
	config_table_names.append("task.taskextraexp")
	while true:
		var csv_name = stream.get_string()
		if csv_name.is_empty():
			break

		match csv_name:
			"ai.ai":
				config_tables["ai.ai"] = true
				DataAi_Ai._init_from_stream(stream, _errors)
			"ai.ai_action":
				config_tables["ai.ai_action"] = true
				DataAi_Ai_action._init_from_stream(stream, _errors)
			"ai.ai_condition":
				config_tables["ai.ai_condition"] = true
				DataAi_Ai_condition._init_from_stream(stream, _errors)
			"equip.ability":
				config_tables["equip.ability"] = true
				DataEquip_Ability._init_from_stream(stream, _errors)
			"equip.equipconfig":
				config_tables["equip.equipconfig"] = true
				DataEquip_Equipconfig._init_from_stream(stream, _errors)
			"equip.jewelry":
				config_tables["equip.jewelry"] = true
				DataEquip_Jewelry._init_from_stream(stream, _errors)
			"equip.jewelryrandom":
				config_tables["equip.jewelryrandom"] = true
				DataEquip_Jewelryrandom._init_from_stream(stream, _errors)
			"equip.jewelrysuit":
				config_tables["equip.jewelrysuit"] = true
				DataEquip_Jewelrysuit._init_from_stream(stream, _errors)
			"equip.jewelrytype":
				config_tables["equip.jewelrytype"] = true
				DataEquip_Jewelrytype._init_from_stream(stream, _errors)
			"equip.rank":
				config_tables["equip.rank"] = true
				DataEquip_Rank._init_from_stream(stream, _errors)
			"other.drop":
				config_tables["other.drop"] = true
				DataOther_Drop._init_from_stream(stream, _errors)
			"other.loot":
				config_tables["other.loot"] = true
				DataOther_Loot._init_from_stream(stream, _errors)
			"other.monster":
				config_tables["other.monster"] = true
				DataOther_Monster._init_from_stream(stream, _errors)
			"other.signin":
				config_tables["other.signin"] = true
				DataOther_Signin._init_from_stream(stream, _errors)
			"task.completeconditiontype":
				config_tables["task.completeconditiontype"] = true
				DataTask_Completeconditiontype._init_from_stream(stream, _errors)
			"task.task":
				config_tables["task.task"] = true
				DataTask_Task._init_from_stream(stream, _errors)
			"task.task2":
				config_tables["task.task2"] = true
				DataTask_Task2._init_from_stream(stream, _errors)
			"task.taskextraexp":
				config_tables["task.taskextraexp"] = true
				DataTask_Taskextraexp._init_from_stream(stream, _errors)
			_:
				_errors.config_unknown(csv_name)
				# 遇到未知表，无法继续处理，直接抛出异常
				assert(false, "Unknown config table: %s" % csv_name)
				break;

	# 检查缺失的配置表
	for table_name in config_tables.keys():
		if not config_tables[table_name]:
			_errors.config_null(table_name)

	# 解析外键引用
	DataEquip_Jewelry._resolve_refs(_errors)
	DataEquip_Jewelryrandom._resolve_refs(_errors)
	DataOther_Monster._resolve_refs(_errors)
	DataOther_Signin._resolve_refs(_errors)
	DataTask_Task._resolve_refs(_errors)
	DataTask_Task2._resolve_refs(_errors)
	return config_table_names


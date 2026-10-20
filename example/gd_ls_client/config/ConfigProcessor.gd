class_name ConfigProcessor

func load_from_package(package: ConfigPackage, errors: ConfigErrors) -> ConfigStream:
	return ConfigLoader.load_package(package, load_from_stream, errors, ["zh_cn", "en", "tw"])

# 从流加载所有配置（新格式）
func load_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	var config_nulls: Array[String] = []
	config_nulls.append("ai.ai")
	config_nulls.append("ai.ai_action")
	config_nulls.append("ai.ai_condition")
	config_nulls.append("equip.ability")
	config_nulls.append("equip.equipconfig")
	config_nulls.append("equip.jewelry")
	config_nulls.append("equip.jewelrysuit")
	config_nulls.append("equip.jewelrytype")
	config_nulls.append("equip.rank")
	config_nulls.append("other.ArgCaptureMode")
	config_nulls.append("other.drop")
	config_nulls.append("other.loot")
	config_nulls.append("other.monster")
	config_nulls.append("other.signin")
	config_nulls.append("task.completeconditiontype")
	config_nulls.append("task.task")
	config_nulls.append("task.task2")
	config_nulls.append("task.taskextraexp")
	# 读取表数量
	var tables = stream.read_tables()
	if not stream.valid():
		return
	for table_name in config_nulls:
		if not tables.has(table_name):
			stream.fail("Missing table in standalone package: " + table_name)
			return
	for table_name in tables:
		var table_input: ConfigStream = tables[table_name]

		match table_name:
			"ai.ai":
				config_nulls.erase("ai.ai")
				DataAi_Ai._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"ai.ai_action":
				config_nulls.erase("ai.ai_action")
				DataAi_Ai_action._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"ai.ai_condition":
				config_nulls.erase("ai.ai_condition")
				DataAi_Ai_condition._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"equip.ability":
				config_nulls.erase("equip.ability")
				DataEquip_Ability._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"equip.equipconfig":
				config_nulls.erase("equip.equipconfig")
				DataEquip_Equipconfig._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"equip.jewelry":
				config_nulls.erase("equip.jewelry")
				DataEquip_Jewelry._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"equip.jewelrysuit":
				config_nulls.erase("equip.jewelrysuit")
				DataEquip_Jewelrysuit._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"equip.jewelrytype":
				config_nulls.erase("equip.jewelrytype")
				DataEquip_Jewelrytype._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"equip.rank":
				config_nulls.erase("equip.rank")
				DataEquip_Rank._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"other.ArgCaptureMode":
				config_nulls.erase("other.ArgCaptureMode")
				DataOther_ArgCaptureMode._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"other.drop":
				config_nulls.erase("other.drop")
				DataOther_Drop._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"other.loot":
				config_nulls.erase("other.loot")
				DataOther_Loot._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"other.monster":
				config_nulls.erase("other.monster")
				DataOther_Monster._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"other.signin":
				config_nulls.erase("other.signin")
				DataOther_Signin._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"task.completeconditiontype":
				config_nulls.erase("task.completeconditiontype")
				DataTask_Completeconditiontype._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"task.task":
				config_nulls.erase("task.task")
				DataTask_Task._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"task.task2":
				config_nulls.erase("task.task2")
				DataTask_Task2._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"task.taskextraexp":
				config_nulls.erase("task.taskextraexp")
				DataTask_Taskextraexp._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			_:
				# 未知表，跳过
				pass

	# 检查缺失的配置表
	for table_name in config_nulls:
		_errors.config_null(table_name)

	# 解析外键引用
	DataEquip_Jewelry._resolve_refs(_errors)
	DataOther_Monster._resolve_refs(_errors)
	DataOther_Signin._resolve_refs(_errors)
	DataTask_Task._resolve_refs(_errors)
	DataTask_Task2._resolve_refs(_errors)

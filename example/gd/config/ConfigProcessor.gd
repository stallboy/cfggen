class_name ConfigProcessor

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
	config_nulls.append("other.drop")
	config_nulls.append("other.loot")
	config_nulls.append("other.monster")
	config_nulls.append("other.signin")
	config_nulls.append("task.completeconditiontype")
	config_nulls.append("task.task")
	config_nulls.append("task.task2")
	config_nulls.append("task.taskextraexp")
	# 读取表数量
	var table_count = stream.read_int32()

	for i in range(table_count):
		# 读取表名
		var table_name = stream.read_string()
		# 读取表大小
		var table_size = stream.read_int32()

		match table_name:
			"ai.ai":
				config_nulls.erase("ai.ai")
				DataAi_Ai._init_from_stream(stream, _errors)
			"ai.ai_action":
				config_nulls.erase("ai.ai_action")
				DataAi_Ai_action._init_from_stream(stream, _errors)
			"ai.ai_condition":
				config_nulls.erase("ai.ai_condition")
				DataAi_Ai_condition._init_from_stream(stream, _errors)
			"equip.ability":
				config_nulls.erase("equip.ability")
				DataEquip_Ability._init_from_stream(stream, _errors)
			"equip.equipconfig":
				config_nulls.erase("equip.equipconfig")
				DataEquip_Equipconfig._init_from_stream(stream, _errors)
			"equip.jewelry":
				config_nulls.erase("equip.jewelry")
				DataEquip_Jewelry._init_from_stream(stream, _errors)
			"equip.jewelrysuit":
				config_nulls.erase("equip.jewelrysuit")
				DataEquip_Jewelrysuit._init_from_stream(stream, _errors)
			"equip.jewelrytype":
				config_nulls.erase("equip.jewelrytype")
				DataEquip_Jewelrytype._init_from_stream(stream, _errors)
			"equip.rank":
				config_nulls.erase("equip.rank")
				DataEquip_Rank._init_from_stream(stream, _errors)
			"other.drop":
				config_nulls.erase("other.drop")
				DataOther_Drop._init_from_stream(stream, _errors)
			"other.loot":
				config_nulls.erase("other.loot")
				DataOther_Loot._init_from_stream(stream, _errors)
			"other.monster":
				config_nulls.erase("other.monster")
				DataOther_Monster._init_from_stream(stream, _errors)
			"other.signin":
				config_nulls.erase("other.signin")
				DataOther_Signin._init_from_stream(stream, _errors)
			"task.completeconditiontype":
				config_nulls.erase("task.completeconditiontype")
				DataTask_Completeconditiontype._init_from_stream(stream, _errors)
			"task.task":
				config_nulls.erase("task.task")
				DataTask_Task._init_from_stream(stream, _errors)
			"task.task2":
				config_nulls.erase("task.task2")
				DataTask_Task2._init_from_stream(stream, _errors)
			"task.taskextraexp":
				config_nulls.erase("task.taskextraexp")
				DataTask_Taskextraexp._init_from_stream(stream, _errors)
			_:
				# 未知表，跳过
				stream.skip_bytes(table_size)

	# 检查缺失的配置表
	for table_name in config_nulls:
		_errors.config_null(table_name)

	# 解析外键引用
	DataEquip_Jewelry._resolve_refs(_errors)
	DataOther_Monster._resolve_refs(_errors)
	DataOther_Signin._resolve_refs(_errors)
	DataTask_Task._resolve_refs(_errors)
	DataTask_Task2._resolve_refs(_errors)

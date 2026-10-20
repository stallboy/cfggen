class_name ConfigProcessor

const TABLE_NAMES: Array[String] = [
	"ai.ai",
	"ai.ai_action",
	"ai.ai_condition",
	"equip.ability",
	"equip.equip_config",
	"equip.jewelry",
	"equip.jewelry_suit",
	"equip.jewelry_type",
	"equip.rank",
	"other.ArgCaptureMode",
	"other.drop",
	"other.loot",
	"other.monster",
	"other.sign_in",
	"task.complete_condition_type",
	"task.task",
	"task.task2",
	"task.task_extra_exp",
]

func load_from_package(package: ConfigPackage, errors: ConfigErrors) -> ConfigStream:
	return ConfigLoader.load_package(package, load_from_stream, errors)

# 从流加载所有配置（新格式）
func load_from_stream(stream: ConfigStream, _errors: ConfigErrors):
	# 读取表数量
	var tables = stream.read_tables()
	if not stream.valid():
		return
	for table_name in TABLE_NAMES:
		if not tables.has(table_name):
			stream.fail("Missing table in standalone package: " + table_name)
			return
	for table_name in tables:
		var table_input: ConfigStream = tables[table_name]

		match table_name:
			"ai.ai":
				DataAi_Ai._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"ai.ai_action":
				DataAi_Ai_action._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"ai.ai_condition":
				DataAi_Ai_condition._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"equip.ability":
				DataEquip_Ability._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"equip.equip_config":
				DataEquip_Equip_config._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"equip.jewelry":
				DataEquip_Jewelry._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"equip.jewelry_suit":
				DataEquip_Jewelry_suit._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"equip.jewelry_type":
				DataEquip_Jewelry_type._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"equip.rank":
				DataEquip_Rank._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"other.ArgCaptureMode":
				DataOther_ArgCaptureMode._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"other.drop":
				DataOther_Drop._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"other.loot":
				DataOther_Loot._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"other.monster":
				DataOther_Monster._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"other.sign_in":
				DataOther_Sign_in._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"task.complete_condition_type":
				DataTask_Complete_condition_type._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"task.task":
				DataTask_Task._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"task.task2":
				DataTask_Task2._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"task.task_extra_exp":
				DataTask_Task_extra_exp._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			_:
				# 未知表，跳过
				pass

	# 解析外键引用
	DataEquip_Jewelry._resolve_refs(_errors)
	DataOther_Monster._resolve_refs(_errors)
	DataOther_Sign_in._resolve_refs(_errors)
	DataTask_Task._resolve_refs(_errors)
	DataTask_Task2._resolve_refs(_errors)

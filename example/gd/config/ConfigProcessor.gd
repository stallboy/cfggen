class_name ConfigProcessor

const TABLE_NAMES: Array[String] = [
	"ai.ai",
	"ai.ai_action",
	"ai.ai_condition",
	"combat.ability",
	"combat.branchreward",
	"combat.cast",
	"combat.encounter",
	"combat.phasebook",
	"combat.timeline",
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
	var previous: Array = [
		[DataAi_Ai._data],
		[DataAi_Ai_action._data],
		[DataAi_Ai_condition._data],
		[DataCombat_Ability._data, DataCombat_Ability._code_map],
		[DataCombat_Branchreward._data],
		[DataCombat_Cast._data],
		[DataCombat_Encounter._data],
		[DataCombat_Phasebook._data, DataCombat_Phasebook._code_edition_map],
		[DataCombat_Timeline._data],
		[DataEquip_Ability._data, DataEquip_Ability.Attack, DataEquip_Ability.Defence, DataEquip_Ability.Hp, DataEquip_Ability.Critical, DataEquip_Ability.Critical_resist, DataEquip_Ability.Block, DataEquip_Ability.Break_armor],
		[DataEquip_Equip_config._data, DataEquip_Equip_config.Instance, DataEquip_Equip_config.Instance2],
		[DataEquip_Jewelry._data],
		[DataEquip_Jewelry_suit._data, DataEquip_Jewelry_suit.SpecialSuit],
		[DataEquip_Jewelry_type._data, DataEquip_Jewelry_type.Jade, DataEquip_Jewelry_type.Bracelet, DataEquip_Jewelry_type.Magic, DataEquip_Jewelry_type.Bottle],
		[DataEquip_Rank._data, DataEquip_Rank.White, DataEquip_Rank.Green, DataEquip_Rank.Blue, DataEquip_Rank.Purple, DataEquip_Rank.Yellow, DataEquip_Rank.Red],
		[DataOther_ArgCaptureMode._data, DataOther_ArgCaptureMode._id_map, DataOther_ArgCaptureMode.Snapshot, DataOther_ArgCaptureMode.Dynamic],
		[DataOther_Drop._data],
		[DataOther_Loot._data],
		[DataOther_Monster._data],
		[DataOther_Sign_in._data],
		[DataTask_Complete_condition_type._data, DataTask_Complete_condition_type.KillMonster, DataTask_Complete_condition_type.TalkNpc, DataTask_Complete_condition_type.CollectItem, DataTask_Complete_condition_type.ConditionAnd, DataTask_Complete_condition_type.Chat, DataTask_Complete_condition_type.TestNoColumn, DataTask_Complete_condition_type.Aa],
		[DataTask_Task._data],
		[DataTask_Task2._data],
		[DataTask_Task_extra_exp._data],
	]
	var before := errors.get_error_count()
	var result := ConfigLoader.load_package(package, load_from_stream, errors)
	if result == null or errors.get_error_count() != before:
		DataAi_Ai._data = previous[0][0]
		DataAi_Ai_action._data = previous[1][0]
		DataAi_Ai_condition._data = previous[2][0]
		DataCombat_Ability._data = previous[3][0]
		DataCombat_Ability._code_map = previous[3][1]
		DataCombat_Branchreward._data = previous[4][0]
		DataCombat_Cast._data = previous[5][0]
		DataCombat_Encounter._data = previous[6][0]
		DataCombat_Phasebook._data = previous[7][0]
		DataCombat_Phasebook._code_edition_map = previous[7][1]
		DataCombat_Timeline._data = previous[8][0]
		DataEquip_Ability._data = previous[9][0]
		DataEquip_Ability.Attack = previous[9][1]
		DataEquip_Ability.Defence = previous[9][2]
		DataEquip_Ability.Hp = previous[9][3]
		DataEquip_Ability.Critical = previous[9][4]
		DataEquip_Ability.Critical_resist = previous[9][5]
		DataEquip_Ability.Block = previous[9][6]
		DataEquip_Ability.Break_armor = previous[9][7]
		DataEquip_Equip_config._data = previous[10][0]
		DataEquip_Equip_config.Instance = previous[10][1]
		DataEquip_Equip_config.Instance2 = previous[10][2]
		DataEquip_Jewelry._data = previous[11][0]
		DataEquip_Jewelry_suit._data = previous[12][0]
		DataEquip_Jewelry_suit.SpecialSuit = previous[12][1]
		DataEquip_Jewelry_type._data = previous[13][0]
		DataEquip_Jewelry_type.Jade = previous[13][1]
		DataEquip_Jewelry_type.Bracelet = previous[13][2]
		DataEquip_Jewelry_type.Magic = previous[13][3]
		DataEquip_Jewelry_type.Bottle = previous[13][4]
		DataEquip_Rank._data = previous[14][0]
		DataEquip_Rank.White = previous[14][1]
		DataEquip_Rank.Green = previous[14][2]
		DataEquip_Rank.Blue = previous[14][3]
		DataEquip_Rank.Purple = previous[14][4]
		DataEquip_Rank.Yellow = previous[14][5]
		DataEquip_Rank.Red = previous[14][6]
		DataOther_ArgCaptureMode._data = previous[15][0]
		DataOther_ArgCaptureMode._id_map = previous[15][1]
		DataOther_ArgCaptureMode.Snapshot = previous[15][2]
		DataOther_ArgCaptureMode.Dynamic = previous[15][3]
		DataOther_Drop._data = previous[16][0]
		DataOther_Loot._data = previous[17][0]
		DataOther_Monster._data = previous[18][0]
		DataOther_Sign_in._data = previous[19][0]
		DataTask_Complete_condition_type._data = previous[20][0]
		DataTask_Complete_condition_type.KillMonster = previous[20][1]
		DataTask_Complete_condition_type.TalkNpc = previous[20][2]
		DataTask_Complete_condition_type.CollectItem = previous[20][3]
		DataTask_Complete_condition_type.ConditionAnd = previous[20][4]
		DataTask_Complete_condition_type.Chat = previous[20][5]
		DataTask_Complete_condition_type.TestNoColumn = previous[20][6]
		DataTask_Complete_condition_type.Aa = previous[20][7]
		DataTask_Task._data = previous[21][0]
		DataTask_Task2._data = previous[22][0]
		DataTask_Task_extra_exp._data = previous[23][0]
		return null
	return result
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
			"combat.ability":
				DataCombat_Ability._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"combat.branchreward":
				DataCombat_Branchreward._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"combat.cast":
				DataCombat_Cast._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"combat.encounter":
				DataCombat_Encounter._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"combat.phasebook":
				DataCombat_Phasebook._init_from_stream(table_input, _errors)
				if not table_input.require_end():
					stream.fail("Invalid table: " + table_name)
					return
			"combat.timeline":
				DataCombat_Timeline._init_from_stream(table_input, _errors)
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
	DataCombat_Ability._resolve_refs(_errors)
	DataCombat_Cast._resolve_refs(_errors)
	DataCombat_Encounter._resolve_refs(_errors)
	DataCombat_Timeline._resolve_refs(_errors)
	DataEquip_Jewelry._resolve_refs(_errors)
	DataOther_Monster._resolve_refs(_errors)
	DataOther_Sign_in._resolve_refs(_errors)
	DataTask_Task._resolve_refs(_errors)
	DataTask_Task2._resolve_refs(_errors)
	var resolver := _ObjectRelations.new(stream, _errors)
	resolver.resolve()
	if stream.valid():
		stream.set_meta(&"_cfggen_snapshot", resolver._tables)
# Keys contain raw scalar values, never object addresses or derived references.
static func _object_key(parts: Array) -> String:
	var values: Array = []
	for value in parts:
		values.append(_key_value(value))
	return var_to_bytes(values).hex_encode()

static func _key_value(value: Variant) -> Variant:
	if value is Object:
		var fields: Array = value.get_meta(&"_cfggen_key_fields", [])
		var parts: Array = [value.get_meta(&"_cfggen_key_type", "")]
		for field in fields:
			parts.append(_key_value(value.get(field)))
		return parts
	return value

class _ObjectRelations:
	extends RefCounted
	var _stream: ConfigStream
	var _errors: ConfigErrors
	var _fields: Dictionary = {}
	var _indexes: Dictionary = {}
	var _maps: Dictionary = {}
	var _occurrences: Array = []
	var _shapes: Dictionary = {
		DataLevelRank: { "name": "LevelRank", "fields": ["level", "rank"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataPosition: { "name": "Position", "fields": ["x", "y", "z"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataRange: { "name": "Range", "fields": ["min", "max"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataAiTriggerTick_ByLevel: { "name": "ai.TriggerTick.ByLevel", "fields": ["init", "coefficient"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataAiTriggerTick_ByServerUpDay: { "name": "ai.TriggerTick.ByServerUpDay", "fields": ["init", "coefficient1", "coefficient2"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataAiTriggerTick_ConstValue: { "name": "ai.TriggerTick.ConstValue", "fields": ["value"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataAi_Ai: { "name": "ai.ai", "fields": ["iD", "desc", "condID", "trigTick", "trigOdds", "actionID", "deathRemove"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataAi_Ai_action: { "name": "ai.ai_action", "fields": ["iD", "desc", "formulaID", "argIList", "argSList"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataAi_Ai_condition: { "name": "ai.ai_condition", "fields": ["iD", "desc", "formulaID", "argIList", "argSList"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataCombat_AbilityUse: { "name": "combat.AbilityUse", "fields": ["binding", "machines"], "maps": [
		], "indexes": [
		], "plans": [
			{ "root": "combat.cast", "name": "RefAbility", "nullable": false, "sources": [
				{ "scope": "", "fields": [0, 0] },
			], "target": "table:combat.ability", "record_key": [0], "steps": [
			] },
		] },
		DataCombat_Anchor: { "name": "combat.Anchor", "fields": [], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataCombat_AnchorUse: { "name": "combat.AnchorUse", "fields": [], "maps": [
		], "indexes": [
		], "plans": [
			{ "root": "combat.ability", "name": "RefAnchor", "nullable": false, "sources": [
			], "target": "ancestor:combat.Machine", "record_key": [], "steps": [
				{ "field": 8, "kind": 0, "keys": [] },
			] },
		] },
		DataCombat_Binding: { "name": "combat.Binding", "fields": ["abilityId"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataCombat_BookEntry: { "name": "combat.BookEntry", "fields": ["name"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataCombat_Combo: { "name": "combat.Combo", "fields": ["weapon", "tier", "airborne", "effect"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataCombatCommand_Jump: { "name": "combat.Command.Jump", "fields": ["key"], "maps": [
		], "indexes": [
		], "plans": [
			{ "root": "combat.encounter", "name": "NullableRefKey", "nullable": true, "sources": [
				{ "scope": "", "fields": [0] },
			], "target": "ancestor:combat.Command.StateMachine", "record_key": [], "steps": [
				{ "field": 0, "kind": 1, "keys": [0] },
			] },
		] },
		DataCombatCommand_Noop: { "name": "combat.Command.Noop", "fields": [], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataCombatCommand_StateMachine: { "name": "combat.Command.StateMachine", "fields": ["phases"], "maps": [
		], "indexes": [
			{ "field": 0, "keys": [0] },
		], "plans": [
		] },
		DataCombatEffect_Damage: { "name": "combat.Effect.Damage", "fields": ["amount"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataCombatEffect_Heal: { "name": "combat.Effect.Heal", "fields": ["amount"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataCombatEffect_None: { "name": "combat.Effect.None", "fields": [], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataCombat_Exit: { "name": "combat.Exit", "fields": ["effect"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataCombat_Frame: { "name": "combat.Frame", "fields": ["tick", "effect"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataCombat_Impact: { "name": "combat.Impact", "fields": ["ordinal", "damage"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataCombat_Machine: { "name": "combat.Machine", "fields": ["code", "phases", "impacts", "frames", "stances", "variants", "combos", "markers", "anchor", "anchors", "onExit", "initial", "impact", "frame", "stance", "variant", "weapon", "tier", "airborne"], "maps": [
		], "indexes": [
			{ "field": 1, "keys": [0] },
			{ "field": 2, "keys": [0] },
			{ "field": 3, "keys": [0] },
			{ "field": 4, "keys": [0] },
			{ "field": 5, "keys": [0] },
			{ "field": 6, "keys": [0, 1, 2] },
			{ "field": 7, "keys": [0] },
		], "plans": [
			{ "root": "combat.ability", "name": "RefInitial", "nullable": false, "sources": [
				{ "scope": "", "fields": [11] },
			], "target": "ancestor:combat.Machine", "record_key": [], "steps": [
				{ "field": 1, "kind": 1, "keys": [0] },
			] },
			{ "root": "combat.ability", "name": "RefImpact", "nullable": false, "sources": [
				{ "scope": "", "fields": [12] },
			], "target": "ancestor:combat.Machine", "record_key": [], "steps": [
				{ "field": 2, "kind": 1, "keys": [0] },
			] },
			{ "root": "combat.ability", "name": "RefFrame", "nullable": false, "sources": [
				{ "scope": "", "fields": [13] },
			], "target": "ancestor:combat.Machine", "record_key": [], "steps": [
				{ "field": 3, "kind": 1, "keys": [0] },
			] },
			{ "root": "combat.ability", "name": "RefStance", "nullable": false, "sources": [
				{ "scope": "", "fields": [14] },
			], "target": "ancestor:combat.Machine", "record_key": [], "steps": [
				{ "field": 4, "kind": 1, "keys": [0] },
			] },
			{ "root": "combat.ability", "name": "RefVariant", "nullable": false, "sources": [
				{ "scope": "", "fields": [15] },
			], "target": "ancestor:combat.Machine", "record_key": [], "steps": [
				{ "field": 5, "kind": 1, "keys": [0] },
			] },
			{ "root": "combat.ability", "name": "RefCombo", "nullable": false, "sources": [
				{ "scope": "", "fields": [16] },
				{ "scope": "", "fields": [17] },
				{ "scope": "", "fields": [18] },
			], "target": "ancestor:combat.Machine", "record_key": [], "steps": [
				{ "field": 6, "kind": 1, "keys": [0, 1, 2] },
			] },
		] },
		DataCombat_MachineUse: { "name": "combat.MachineUse", "fields": ["machineKey", "targets"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataCombat_Marker: { "name": "combat.Marker", "fields": ["key"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataCombat_NestedPhase: { "name": "combat.NestedPhase", "fields": ["key", "action"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataCombat_OptionalAim: { "name": "combat.OptionalAim", "fields": ["abilityId", "machineKey", "phaseKey"], "maps": [
		], "indexes": [
		], "plans": [
			{ "root": "combat.cast", "name": "NullableRefTarget", "nullable": true, "sources": [
				{ "scope": "", "fields": [0] },
				{ "scope": "", "fields": [1] },
				{ "scope": "", "fields": [2] },
			], "target": "table:combat.ability", "record_key": [0], "steps": [
				{ "field": 3, "kind": 1, "keys": [0] },
				{ "field": 1, "kind": 1, "keys": [0] },
			] },
		] },
		DataCombat_Origin: { "name": "combat.Origin", "fields": ["x", "y"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataCombat_Phase: { "name": "combat.Phase", "fields": ["key", "next"], "maps": [
		], "indexes": [
		], "plans": [
			{ "root": "combat.ability", "name": "NullableRefNext", "nullable": true, "sources": [
				{ "scope": "", "fields": [1] },
			], "target": "ancestor:combat.Machine", "record_key": [], "steps": [
				{ "field": 1, "kind": 1, "keys": [0] },
			] },
			{ "root": "combat.ability", "name": "RefExit", "nullable": false, "sources": [
			], "target": "ancestor:combat.Machine", "record_key": [], "steps": [
				{ "field": 10, "kind": 0, "keys": [] },
				{ "field": 0, "kind": 0, "keys": [] },
			] },
		] },
		DataCombat_PhaseKey: { "name": "combat.PhaseKey", "fields": ["name", "variant"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataCombat_PhaseUse: { "name": "combat.PhaseUse", "fields": ["phaseKey"], "maps": [
		], "indexes": [
		], "plans": [
			{ "root": "combat.cast", "name": "RefTarget", "nullable": false, "sources": [
				{ "scope": "ancestor:combat.AbilityUse", "fields": [0, 0] },
				{ "scope": "ancestor:combat.MachineUse", "fields": [0] },
				{ "scope": "", "fields": [0] },
			], "target": "table:combat.ability", "record_key": [0], "steps": [
				{ "field": 3, "kind": 1, "keys": [0] },
				{ "field": 1, "kind": 1, "keys": [0] },
			] },
			{ "root": "combat.cast", "name": "RefByCode", "nullable": false, "sources": [
				{ "scope": "root", "fields": [1] },
				{ "scope": "ancestor:combat.MachineUse", "fields": [0] },
				{ "scope": "", "fields": [0] },
			], "target": "table:combat.ability", "record_key": [1], "steps": [
				{ "field": 3, "kind": 1, "keys": [0] },
				{ "field": 1, "kind": 1, "keys": [0] },
			] },
			{ "root": "combat.cast", "name": "RefOrigin", "nullable": false, "sources": [
			], "target": "root", "record_key": [], "steps": [
				{ "field": 2, "kind": 0, "keys": [] },
			] },
		] },
		DataCombat_PhaseVariant: { "name": "combat.PhaseVariant", "fields": ["key", "effect"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataCombat_Reward: { "name": "combat.Reward", "fields": ["count"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataCombat_Stance: { "name": "combat.Stance", "fields": ["airborne", "effect"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataCombat_Ability: { "name": "combat.ability", "fields": ["id", "code", "origin", "machines", "startMachine", "startPhase"], "maps": [
		], "indexes": [
			{ "field": 3, "keys": [0] },
		], "plans": [
			{ "root": "combat.ability", "name": "RefInitial", "nullable": false, "sources": [
				{ "scope": "", "fields": [4] },
				{ "scope": "", "fields": [5] },
			], "target": "root", "record_key": [], "steps": [
				{ "field": 3, "kind": 1, "keys": [0] },
				{ "field": 1, "kind": 1, "keys": [0] },
			] },
			{ "root": "combat.ability", "name": "RefSpawn", "nullable": false, "sources": [
			], "target": "root", "record_key": [], "steps": [
				{ "field": 2, "kind": 0, "keys": [] },
			] },
		] },
		DataCombat_Branchreward: { "name": "combat.branchreward", "fields": ["key", "reward"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataCombat_Cast: { "name": "combat.cast", "fields": ["id", "abilityCode", "origin", "uses", "optional", "school", "tier", "airborne", "bookCode", "edition", "rewardKey"], "maps": [
		], "indexes": [
		], "plans": [
			{ "root": "combat.cast", "name": "RefBook", "nullable": false, "sources": [
				{ "scope": "", "fields": [5] },
				{ "scope": "", "fields": [6] },
				{ "scope": "", "fields": [7] },
			], "target": "table:combat.phasebook", "record_key": [0, 1, 2], "steps": [
				{ "field": 5, "kind": 0, "keys": [] },
			] },
			{ "root": "combat.cast", "name": "RefBookByCode", "nullable": false, "sources": [
				{ "scope": "", "fields": [8] },
				{ "scope": "", "fields": [9] },
			], "target": "table:combat.phasebook", "record_key": [3, 4], "steps": [
				{ "field": 5, "kind": 0, "keys": [] },
			] },
			{ "root": "combat.cast", "name": "RefBookRecord", "nullable": false, "sources": [
				{ "scope": "root", "fields": [5] },
				{ "scope": "root", "fields": [6] },
				{ "scope": "root", "fields": [7] },
			], "target": "table:combat.phasebook", "record_key": [0, 1, 2], "steps": [
			] },
			{ "root": "combat.cast", "name": "RefReward", "nullable": false, "sources": [
				{ "scope": "", "fields": [10] },
			], "target": "table:combat.branchreward", "record_key": [0], "steps": [
				{ "field": 1, "kind": 0, "keys": [] },
			] },
		] },
		DataCombat_Encounter: { "name": "combat.encounter", "fields": ["id", "command"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataCombat_Phasebook: { "name": "combat.phasebook", "fields": ["school", "tier", "airborne", "code", "edition", "entry"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataCombat_Timeline: { "name": "combat.timeline", "fields": ["id", "events", "channels", "slots", "clock", "modes", "eventKey", "channel", "slot", "frame", "airborne", "fallback"], "maps": [
			1,
			2,
			3,
			4,
			5,
		], "indexes": [
		], "plans": [
			{ "root": "combat.timeline", "name": "RefEventKey", "nullable": false, "sources": [
				{ "scope": "", "fields": [6] },
			], "target": "root", "record_key": [], "steps": [
				{ "field": 1, "kind": 2, "keys": [] },
			] },
			{ "root": "combat.timeline", "name": "RefChannel", "nullable": false, "sources": [
				{ "scope": "", "fields": [7] },
			], "target": "root", "record_key": [], "steps": [
				{ "field": 2, "kind": 2, "keys": [] },
			] },
			{ "root": "combat.timeline", "name": "RefSlot", "nullable": false, "sources": [
				{ "scope": "", "fields": [8] },
			], "target": "root", "record_key": [], "steps": [
				{ "field": 3, "kind": 2, "keys": [] },
			] },
			{ "root": "combat.timeline", "name": "RefFrame", "nullable": false, "sources": [
				{ "scope": "", "fields": [9] },
			], "target": "root", "record_key": [], "steps": [
				{ "field": 4, "kind": 2, "keys": [] },
			] },
			{ "root": "combat.timeline", "name": "RefAirborne", "nullable": false, "sources": [
				{ "scope": "", "fields": [10] },
			], "target": "root", "record_key": [], "steps": [
				{ "field": 5, "kind": 2, "keys": [] },
			] },
			{ "root": "combat.timeline", "name": "RefDefault", "nullable": false, "sources": [
			], "target": "root", "record_key": [], "steps": [
				{ "field": 11, "kind": 0, "keys": [] },
			] },
		] },
		DataEquip_TestPackBean: { "name": "equip.TestPackBean", "fields": ["name", "iRange"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataEquip_Ability: { "name": "equip.ability", "fields": ["id", "name"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataEquip_Equip_config: { "name": "equip.equip_config", "fields": ["entry", "stone_count_for_set", "draw_protect_name", "broadcastid", "broadcast_least_quality", "week_reward_mailid"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataEquip_Jewelry: { "name": "equip.jewelry", "fields": ["iD", "name", "iconFile", "lvlRank", "jType", "suitID", "keyAbility", "keyAbilityValue", "salePrice", "description"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataEquip_Jewelry_suit: { "name": "equip.jewelry_suit", "fields": ["suitID", "ename", "name", "ability1", "ability1Value", "ability2", "ability2Value", "ability3", "ability3Value", "suitList"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataEquip_Jewelry_type: { "name": "equip.jewelry_type", "fields": ["typeName"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataEquip_Rank: { "name": "equip.rank", "fields": ["rankID", "rankName", "rankShowName"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataOther_ArgCaptureMode: { "name": "other.ArgCaptureMode", "fields": ["name", "id", "comment"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataOther_DropItem: { "name": "other.DropItem", "fields": ["chance", "itemids", "countmin", "countmax"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataOther_Drop: { "name": "other.drop", "fields": ["dropid", "name", "items", "testmap"], "maps": [
			3,
		], "indexes": [
		], "plans": [
		] },
		DataOther_Loot: { "name": "other.loot", "fields": ["lootid", "ename", "name", "chanceList"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataOther_Monster: { "name": "other.monster", "fields": ["id", "posList", "lootId", "lootItemId", "enumMap1", "enumMap2"], "maps": [
			4,
			5,
		], "indexes": [
		], "plans": [
		] },
		DataOther_Sign_in: { "name": "other.sign_in", "fields": ["id", "item2countMap", "vipitem2vipcountMap", "viplevel", "iconFile"], "maps": [
			1,
			2,
		], "indexes": [
		], "plans": [
		] },
		DataTask_TestDefaultBean: { "name": "task.TestDefaultBean", "fields": ["testInt", "testBool", "testString", "testSubBean", "testList", "testList2", "testMap"], "maps": [
			6,
		], "indexes": [
		], "plans": [
		] },
		DataTask_Complete_condition_type: { "name": "task.complete_condition_type", "fields": ["id", "name"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataTaskCompletecondition_Chat: { "name": "task.completecondition.Chat", "fields": ["msg"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataTaskCompletecondition_CollectItem: { "name": "task.completecondition.CollectItem", "fields": ["itemid", "count"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataTaskCompletecondition_ConditionAnd: { "name": "task.completecondition.ConditionAnd", "fields": ["cond1", "cond2"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataTaskCompletecondition_KillMonster: { "name": "task.completecondition.KillMonster", "fields": ["monsterid", "count"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataTaskCompletecondition_TalkNpc: { "name": "task.completecondition.TalkNpc", "fields": ["npcid"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataTaskCompletecondition_TestNoColumn: { "name": "task.completecondition.TestNoColumn", "fields": [], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataTaskCompletecondition_Aa: { "name": "task.completecondition.aa", "fields": [], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataTask_Task: { "name": "task.task", "fields": ["taskid", "name", "nexttask", "completecondition", "exp", "testDefaultBean"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataTask_Task2: { "name": "task.task2", "fields": ["taskid", "name", "nexttask", "completecondition", "exp", "testBool", "testString", "testStruct", "testList", "testListStruct", "testListInterface"], "maps": [
		], "indexes": [
		], "plans": [
		] },
		DataTask_Task_extra_exp: { "name": "task.task_extra_exp", "fields": ["taskid", "extraexp", "test1", "test2", "fielda", "fieldb", "fieldc", "fieldd"], "maps": [
		], "indexes": [
		], "plans": [
		] },
	}
	var _tables: Dictionary = {}

	func _init(stream: ConfigStream, errors: ConfigErrors):
		_stream = stream
		_errors = errors
		_tables["ai.ai"] = DataAi_Ai.all()
		_tables["ai.ai_action"] = DataAi_Ai_action.all()
		_tables["ai.ai_condition"] = DataAi_Ai_condition.all()
		_tables["combat.ability"] = DataCombat_Ability.all()
		_tables["combat.branchreward"] = DataCombat_Branchreward.all()
		_tables["combat.cast"] = DataCombat_Cast.all()
		_tables["combat.encounter"] = DataCombat_Encounter.all()
		_tables["combat.phasebook"] = DataCombat_Phasebook.all()
		_tables["combat.timeline"] = DataCombat_Timeline.all()
		_tables["equip.ability"] = DataEquip_Ability.all()
		_tables["equip.equip_config"] = DataEquip_Equip_config.all()
		_tables["equip.jewelry"] = DataEquip_Jewelry.all()
		_tables["equip.jewelry_suit"] = DataEquip_Jewelry_suit.all()
		_tables["equip.jewelry_type"] = DataEquip_Jewelry_type.all()
		_tables["equip.rank"] = DataEquip_Rank.all()
		_tables["other.ArgCaptureMode"] = DataOther_ArgCaptureMode.all()
		_tables["other.drop"] = DataOther_Drop.all()
		_tables["other.loot"] = DataOther_Loot.all()
		_tables["other.monster"] = DataOther_Monster.all()
		_tables["other.sign_in"] = DataOther_Sign_in.all()
		_tables["task.complete_condition_type"] = DataTask_Complete_condition_type.all()
		_tables["task.task"] = DataTask_Task.all()
		_tables["task.task2"] = DataTask_Task2.all()
		_tables["task.task_extra_exp"] = DataTask_Task_extra_exp.all()
	func _read(value: Object) -> Array:
		var identity: int = value.get_instance_id()
		if not _fields.has(identity):
			var fields: Array = []
			for name in _shapes[value.get_script()].fields:
				fields.append(value.get(name))
			_fields[identity] = fields
		return _fields[identity]

	func _cache_key(owner: Object, field: int) -> String:
		return "%d:%d" % [owner.get_instance_id(), field]

	func _index(identity: String, rows: Array, fields: Array) -> Dictionary:
		var definition: String = identity + ":" + str(fields)
		if _indexes.has(definition):
			return _indexes[definition]
		var index: Dictionary = {}
		_indexes[definition] = index
		for row in rows:
			var values: Array = _read(row)
			var parts: Array = []
			for field in fields:
				parts.append(values[field])
			var key: String = ConfigProcessor._object_key(parts)
			if index.has(key):
				_stream.fail("Duplicate local index key: " + key)
			index[key] = row
		return index

	func _map(identity: String, rows: Dictionary) -> Dictionary:
		if _maps.has(identity):
			return _maps[identity]
		var index: Dictionary = {}
		_maps[identity] = index
		for key in rows:
			var canonical: String = ConfigProcessor._object_key([key])
			if index.has(canonical):
				_stream.fail("Duplicate map key: " + canonical)
			index[canonical] = rows[key]
		return index

	func _walk(value: Variant, root: Object, table: String, parent: Dictionary):
		if value is Object and _shapes.has(value.get_script()):
			var shape: Dictionary = _shapes[value.get_script()]
			var at: Dictionary = { "value": value, "root": root, "table": table, "parent": parent }
			if not shape.plans.is_empty():
				_occurrences.append(at)
			var fields: Array = _read(value)
			for definition in shape.indexes:
				_index(_cache_key(value, definition.field), fields[definition.field], definition.keys)
			for field in shape.maps:
				_map(_cache_key(value, field), fields[field])
			for field in fields:
				_walk(field, root, table, at)
		elif value is Array:
			for item in value:
				_walk(item, root, table, parent)
		elif value is Dictionary:
			for key in value:
				_walk(key, root, table, parent)
				_walk(value[key], root, table, parent)

	func _scope(name: String, at: Dictionary) -> Object:
		if name.is_empty():
			return at.value
		if name == "root":
			return at.root
		while not at.is_empty():
			var value: Object = at.value
			if "ancestor:" + _shapes[value.get_script()].name == name:
				return value
			at = at.parent
		return null

	func resolve():
		for table in _tables:
			for row in _tables[table]:
				_walk(row, row, table, {})
		if not _stream.valid():
			return
		for at in _occurrences:
			for plan in _shapes[at.value.get_script()].plans:
				if plan.root == at.table:
					_bind(at, plan)

	func _bind(at: Dictionary, plan: Dictionary):
		var keys: Array = []
		var valid: bool = true
		for source in plan.sources:
			var value: Variant = _scope(source.scope, at)
			if value != null:
				for field in source.fields:
					value = _read(value)[field]
				keys.append(value)
			else:
				valid = false
		var external: bool = plan.target.begins_with("table:")
		var target: Variant
		if external:
			target = _tables.get(plan.target.substr(6))
		else:
			target = _scope(plan.target, at)
		if not valid or target == null:
			_stream.fail("Missing reference scope: " + str(at.table) + "." + plan.name)
			return
		var used: int = 0
		if external:
			target = _index(plan.target, target, plan.record_key).get(ConfigProcessor._object_key(keys.slice(0, plan.record_key.size())))
			used = plan.record_key.size()
		for step in plan.steps:
			if target == null:
				break
			var member: Variant = _read(target)[step.field]
			if step.kind == 0:
				target = member
			elif step.kind == 1:
				target = _index(_cache_key(target, step.field), member, step.keys).get(ConfigProcessor._object_key(keys.slice(used, used + step.keys.size())))
			else:
				target = _map(_cache_key(target, step.field), member).get(ConfigProcessor._object_key([keys[used]]))
			used += 1 if step.kind == 2 else step.keys.size()
		if target == null and not plan.nullable:
			_errors.ref_null(at.table, plan.name)
			_stream.fail("Missing object reference: " + str(at.table) + "." + plan.name)
		at.value.set(plan.name, target)





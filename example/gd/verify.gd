extends SceneTree

var failed: bool = false

func check(condition: bool, message: String):
	if not condition:
		failed = true
		push_error(message)

func _initialize():
	var errors = ConfigErrors.new()
	var processor = ConfigProcessor.new()
	# 当前快照必须存活到使用配置结束。
	var snapshot = processor.load_from_package(ConfigPackage.read_file("res://base.bytes"), errors)
	if snapshot == null or errors.get_error_count() != 0:
		errors.print_all()
		quit(1)
		return

	var task = DataTask_Task.find(1)
	check(task.name[0] == "杀个怪", "原有文本")
	check(task.NullableRefNexttask == DataTask_Task.find(2), "原有可空外键")
	check(task.completecondition.RefMonsterid == DataOther_Monster.find(1), "原有接口外键")
	check(DataEquip_Ability.Attack == DataEquip_Ability.find(1), "原有枚举")
	check(DataAi_Ai.find(10012) != null, "原有 Excel 数据")
	check(DataOther_Sign_in.find(4).RefVipitem2vipcountMap[10001] == DataOther_Loot.find(10), "旧 map value 外键的目标身份")

	var ability = DataCombat_Ability.find(1001)
	var cast = DataCombat_Cast.find(1)
	var machine = ability.machines[0]
	check(ability.machines.size() == 2, "两套机器作用域均被验证")
	check(ability.RefInitial == machine.phases[0], "本 record 多层索引")
	check(ability.RefSpawn == ability.origin, "固定对象")
	var anchor_uses = {}
	for i in range(ability.machines.size()):
		var current = ability.machines[i]
		check(current.phases.size() == (3 if i == 0 else 1) and current.anchors.size() == (2 if i == 0 else 1), "阶段与空来源覆盖两个作用域")
		check(current.RefInitial != null and current.RefInitial == current.phases[0], "祖先字符串索引")
		check(current.RefImpact != null and current.RefImpact == current.impacts[0], "int 索引")
		check(current.RefFrame != null and current.RefFrame == current.frames[0], "long 索引")
		check(current.RefStance != null and current.RefStance == current.stances[0], "bool 索引")
		check(current.RefVariant != null and current.RefVariant == current.variants[0], "struct 索引")
		check(current.RefCombo != null and current.RefCombo == current.combos[0], "三字段复合索引")
		for phase in current.phases:
			check(phase.RefExit == current.onExit.effect, "固定路径的接口终点")
		for anchor in current.anchors:
			check(not anchor_uses.has(anchor), "空来源对象保留各自身份")
			anchor_uses[anchor] = true
			check(anchor.RefAnchor == current.anchor, "空来源绑定所属机器")
	check(machine.RefFrame.tick == 9007199254740993 and ability.machines[1].RefFrame.tick == 0, "long 大整数与零键")
	check(machine.phases[0].NullableRefNext == machine.phases[1] and machine.phases[1].NullableRefNext == machine.phases[0], "循环明确经过 Idle→Hit→Idle")
	check(machine.phases[2].NullableRefNext == null, "缺失阶段可空")
	check(ability.machines[1].phases[0].NullableRefNext == null, "局部键不回退到其他容器")
	check(machine.anchor != ability.machines[1].anchor, "空目标对象保留身份")
	check(cast.uses.size() == 1 and cast.uses[0].machines.size() == 2, "混合来源覆盖两个机器作用域")
	var ability_use = cast.uses[0]
	check(ability_use.RefAbility == ability, "固定源路径的 record 引用")
	for i in range(ability_use.machines.size()):
		var machine_use = ability_use.machines[i]
		check(machine_use.targets.size() == (3 if i == 0 else 1), "叶来源不能为空循环")
		for j in range(machine_use.targets.size()):
			var use = machine_use.targets[j]
			var target = ability.machines[i].phases[j]
			check(use.RefTarget == target and use.RefByCode == target, "混合祖先、源根与唯一键的目标身份")
			check(use.RefOrigin == cast.origin, "源根固定对象")
	var book = DataCombat_Phasebook.find("Fire", 0, false)
	check(cast.RefBook == book.entry and cast.RefBookByCode == book.entry and cast.RefBookRecord == book, "复合主键和唯一键")
	var reward = DataCombat_Branchreward.find(cast.rewardKey)
	check(reward != null and cast.RefReward == reward.reward and cast.RefReward.count == 5, "struct 主键的目标身份")
	for i in range(3):
		check(cast.optional[i].NullableRefTarget == null, "整条链可空: %d" % i)
	check(cast.optional[3].NullableRefTarget == machine.phases[2], "空字符串是合法键")
	var timeline = DataCombat_Timeline.find(1)
	var event: DataCombat_Effect = null
	for key in timeline.events:
		if key.name == timeline.eventKey.name and key.variant == timeline.eventKey.variant:
			event = timeline.events[key]
	check(event != null and timeline.RefEventKey == event and event is DataCombatEffect_Damage, "struct map 键的目标身份")
	check(timeline.RefChannel != null and timeline.RefChannel == timeline.channels["impact"], "str map 键")
	check(timeline.RefSlot != null and timeline.RefSlot == timeline.slots[0], "int map 键")
	check(timeline.RefFrame != null and timeline.RefFrame == timeline.clock[9007199254740993], "long map 键")
	check(timeline.RefAirborne != null and timeline.RefAirborne == timeline.modes[false], "bool map 键")
	check(timeline.RefDefault != null and timeline.RefDefault == timeline.fallback, "无源键的接口固定终点")
	var empty_effects = {}
	for effect in [machine.onExit.effect, ability.machines[1].onExit.effect, timeline.fallback, timeline.modes[true]]:
		check(effect is DataCombatEffect_None and not empty_effects.has(effect), "值相同的 None 终点保留各自身份")
		empty_effects[effect] = true
	var nested = DataCombat_Encounter.find(1).command.phases[1].action
	check(nested.phases[0].action.NullableRefKey == null, "最近接口祖先不回退")
	check(nested.phases[1].action.NullableRefKey == nested.phases[0], "最近接口祖先索引")

	var second = processor.load_from_package(ConfigPackage.read_file("res://base.bytes"), errors)
	check(second != null and errors.get_error_count() == 0, "重新加载")
	check(machine.RefInitial == machine.phases[0] and ability != DataCombat_Ability.find(1001), "旧快照与新快照独立")
	check(cast.uses[0].machines[0].targets[1].RefTarget == machine.phases[1], "旧快照的跨 record 引用不重新绑定")
	if not failed:
		print("gd example ok")
	quit(1 if failed else 0)

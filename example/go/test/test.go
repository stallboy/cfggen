package test

import (
	"cfgtest/config"
	"fmt"
	"reflect"
)

func require(ok bool, message string) {
	if !ok {
		panic(message)
	}
}

func DoTest() {
	task := config.GetTaskTaskMgr().Get(1)
	require(task != nil && task == config.GetTaskTaskMgr().GetAll()[0], "task lookup identity")
	require(task.Taskid() == 1 && task.Nexttask() == 2 && reflect.DeepEqual(task.Name(), []string{"杀个怪", "杀怪"}), "task fields and list")
	require(task.String() != "", "task string representation")

	condition, ok := task.Completecondition().(*config.TaskCompleteconditionKillMonster)
	require(ok && condition.Monsterid() == 1 && condition.Count() == 3, "interface implementation")
	monster := config.GetOtherMonsterMgr().Get(condition.Monsterid())
	require(condition.RefMonsterid() == monster && monster.PosList()[1].Y() == 22, "nested record reference")
	require(task.NullableRefNexttask() == config.GetTaskTaskMgr().Get(2), "nullable record reference hit")
	require(task.NullableRefTaskid() == config.GetTaskTask_extra_expMgr().Get(1), "nullable cross-table reference")
	missing := config.GetTaskTaskMgr().Get(3)
	require(missing.NullableRefNexttask() == nil && missing.NullableRefTaskid() == nil, "nullable record reference miss")

	item := config.GetOtherLoot_itemMgr().Get(2, 40007)
	require(item != nil && item.Lootid() == 2 && item.Itemid() == 40007, "composite primary key")
	loot := config.GetOtherLootMgr().Get(2)
	require(len(loot.ListRefLootid()) == 7, "reverse list reference")
	found := false
	for _, row := range loot.ListRefLootid() {
		require(row.Lootid() == 2 && row == config.GetOtherLoot_itemMgr().Get(row.Lootid(), row.Itemid()), "reverse reference scope and identity")
		found = found || row == item
	}
	require(found, "reverse reference identity")

	signIn := config.GetOtherSign_inMgr().Get(4)
	require(len(signIn.Item2countMap()) == 3 && signIn.Item2countMap()[10001] == 5, "map field")
	require(signIn.Vipitem2vipcountMap()[10001] == 10 && signIn.RefVipitem2vipcountMap()[10001] == config.GetOtherLootMgr().Get(10), "map value reference")
	listRef := config.GetOtherKey_testMgr().Get(1, 2).RefIds()
	listTarget := config.GetOtherSign_inMgr().Get(12)
	require(listTarget != nil && len(listRef) == 2 && listRef[1] == listTarget && listRef[1].Id() == 12, "list field reference identity")

	enum := config.GetTaskComplete_condition_typeMgr().GetKillMonster()
	require(enum == config.GetTaskComplete_condition_typeMgr().Get(1) && enum.Name() == "KillMonster", "enum identity")
	entry := config.GetEquipEquip_configMgr().GetInstance()
	require(entry.Broadcastid() == 9500 && entry.Draw_protect_name() == "测试", "named entry")
	require(config.GetEquipEquip_configMgr().GetInstance2().Week_reward_mailid() == 33, "column-mode CSV")

	jewelry := config.GetEquipJewelryMgr().Get(1)
	key := *jewelry.LvlRank()
	random := config.GetEquipJewelry_randomMgr().Get(&key)
	require(random != nil && jewelry.RefLvlRank() == random, "struct primary key compares field values")

	defaults := task.TestDefaultBean()
	require(defaults.TestInt() == 0 && !defaults.TestBool() && defaults.TestString() == "", "default primitives")
	require(defaults.TestSubBean().X() == 0 && defaults.TestSubBean().Y() == 0, "default struct")
	require(len(defaults.TestList()) == 0 && len(defaults.TestList2()) == 0 && len(defaults.TestMap()) == 0, "default containers")

	ai := config.GetAiAiMgr()
	constant, ok := ai.Get(10019).TrigTick().(*config.AiTriggerTickConstValue)
	require(ok && constant.Value() == 30000, "numeric interface discriminator")
	level, ok := ai.Get(10020).TrigTick().(*config.AiTriggerTickByLevel)
	require(ok && level.Coefficient() == 0.1, "level interface discriminator")
	serverDay, ok := ai.Get(10021).TrigTick().(*config.AiTriggerTickByServerUpDay)
	require(ok && serverDay.Coefficient2() == 0.2, "server-day interface discriminator")
	fmt.Println("old configuration assertions passed")
}

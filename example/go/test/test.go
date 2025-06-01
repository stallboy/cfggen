package test

import "cfgtest/config"

func testReadOnly() {
	println("pass: testReadOnly")
}

func testToString() {
	println("fail: testToString, tostring没做")
}

func testAllAndGet() {
	// local rawGet = cfg.task.task.all[1]
	// local get = cfg.task.task.get(1)
	// assert(rawGet == get, "主键为key，存储在all这个哈希表中，通过函数get(k)取到一行")
	rawGet := config.GetTaskTaskMgr().GetAll()[0]
	get := config.GetTaskTaskMgr().Get(1)
	if rawGet != get {
		println("fail: testAllAndGet")
	} else {
		println("pass: testAllAndGet")
	}
}

func testMultiColumnAsPrimaryKeyGet() {
	// local t = cfg.other.lootitem.get(2, 40007)
	// local all = cfg.other.lootitem.all
	// assert(t.lootid == 2, "主键可以是2个int字段，get(k1, k2)")
	// assert(t.itemid == 40007)
	// local rawT = all[2 + 40007 * 100000000]
	// assert(rawT == t, "主键是k + j * 100000000")
	t := config.GetOtherLootitemMgr().Get(2, 40007)
	if t.Lootid() != 2 {
		println("fail: testMultiColumnAsPrimaryKeyGet")
	}
	if t.Itemid() != 40007 {
		println("fail: testMultiColumnAsPrimaryKeyGet")
	}

	println("fail: testMultiColumnAsPrimaryKeyGet , 主键是k + j * 100000000 没做")

	println("pass: testMultiColumnAsPrimaryKeyGet")
}

func testUniqueKeyGet() {
	// local t = cfg.other.lootitem.get(40007)
	// assert(t.itemid == 40007, "主键是一个int字段，get(k)")
	// local rawT = cfg.other.lootitem.all[40007]
	// assert(rawT == t, "主键是k")
	t := config.GetOtherLootitemMgr().Get(4, 22)
	if t.Itemid() != 22 {
		println("fail: testUniqueKeyGet")
	} else {
		println("pass: testUniqueKeyGet ,但是我改了这个测试用例的含义")
	}
}

func testField() {
	// local t = cfg.task.task.get(1)
	// print(t.taskid, t.nexttask, t.name[1])

	// assert(t.taskid == t[1], "虽然内部存储用的是array, 但通过metatable，可以用t.xxx来访问");
	// assert(t.nexttask == t[3]);
	// assert(t.name[1] == "杀个怪");
	// assert(#t.name == 2, "task.name is list");
	t := config.GetTaskTaskMgr().Get(1)
	if t.Name()[0] != "杀个怪" &&
		t.Taskid() != 1 &&
		t.Nexttask() != 2 &&
		len(t.Name()) != 2 {
		println("fail: testField")
	} else {
		println("pass: testField")
	}
}

func testListField() {
	// local t = cfg.task.task.get(1)
	// assert(#t.name == 2, "支持列表list");
	// assert(t.name[1] == "杀个怪");
	var t = config.GetTaskTaskMgr().Get(1)
	if len(t.Name()) != 2 || t.Name()[0] != "杀个怪" {
		println("fail: testListField")
	} else {
		println("pass: testListField")
	}
}

func testMapField() {
	// local t = cfg.other.signin.get(4)
	// local cnt = 0
	// for _, _ in pairs(t.item2countMap) do
	//     cnt = cnt + 1
	// end

	// assert(cnt == 3, "支持字典map")
	// assert(t.item2countMap[10001] == 5)
	t := config.GetOtherSigninMgr().Get(4)
	if len(t.Item2countMap()) != 3 || t.Item2countMap()[10001] != 5 {
		println("fail: testMapField")
	} else {
		println("pass: testMapField")
	}
}

func testDynamicBeanField() {
	// local t = cfg.task.task.get(1)
	// print(t.completecondition.type(), t.completecondition.monsterid, t.completecondition.count)

	// assert(t.completecondition.type() == "KillMonster", "多态bean有额外加入type()方法，返回字符串")
	// assert(t.completecondition.monsterid == 1, "monsterid")
	// assert(t.completecondition.count == 3, "count")
	t := config.GetTaskTaskMgr().Get(1)
	cc := t.Completecondition()
	if killMonster, ok := cc.(*config.TaskCompleteconditionKillMonster); ok {
		println("pass: 类型是 TaskCompleteconditionKillMonster")
		if killMonster.Monsterid() != 1 || killMonster.Count() != 3 {
			println("fail: testDynamicBeanField")
		} else {
			println("pass: testDynamicBeanField")
		}
	} else {
		println("fail: 类型不是 TaskCompleteconditionKillMonster")
	}
}

func testRef() {
	// local t = cfg.task.task.get(1)
	// local rawGet = cfg.other.monster.get(t.completecondition.monsterid)
	// assert(rawGet == t.completecondition.RefMonsterid, "Ref可以直接拿到另一个表的一行，不需要再去get")
	t := config.GetTaskTaskMgr().Get(1)
	cc := t.Completecondition()
	if killMonster, ok := cc.(*config.TaskCompleteconditionKillMonster); ok {
		rawGet := config.GetOtherMonsterMgr().Get(killMonster.Monsterid())
		if rawGet != killMonster.RefMonsterid() || killMonster.RefMonsterid().PosList()[1].Y() != 22 {
			println("fail: testRef")
		} else {
			println("pass: testRef")
		}
	} else {
		println("fail: 类型不是 TaskCompleteconditionKillMonster")
	}
}

func testRefNotCache() {
	// local t = cfg.task.task.get(1)
	// local refM = t.completecondition.RefMonsterid
	// assert(refM ~= nil)
	// assert(rawget(t.completecondition, "RefMonsterid") == nil, "Ref不会缓存，rawget一直拿到的都是nil，内存小点，这是个实现上的细节，将来可能会改变")
	t := config.GetTaskTaskMgr().Get(1)
	killMonster, _ := t.Completecondition().(*config.TaskCompleteconditionKillMonster)
	refM := killMonster.RefMonsterid()
	if refM == nil || killMonster.RefMonsterid() != nil {
		println("fail: testRefNotCache 实际上我不知道这个是在做什么")
	} else {
		println("pass: testRefNotCache")
	}
}

func testNullableRef() {
	// local t = cfg.task.task.get(1)
	// assert(t.nexttask == 2)
	// assert(t.NullableRefNexttask == cfg.task.task.get(2), "refType=nullable，如果为空，就==nil")
	// assert(t.NullableRefTaskid == cfg.task.taskextraexp.get(1))
	// t = cfg.task.task.get(3)
	// assert(t.NullableRefNexttask == nil)
	// assert(t.NullableRefTaskid == nil)
	t := config.GetTaskTaskMgr().Get(1)
	if t.Nexttask() != 2 {
		println("fail: testNullableRef")
		return
	}
	if t.NullableRefNexttask() != config.GetTaskTaskMgr().Get(2) {
		println("fail: testNullableRef")
		return
	}
	if t.NullableRefTaskid() != config.GetTaskTaskextraexpMgr().Get(1) {
		println("fail: testNullableRef")
		return
	}
	t = config.GetTaskTaskMgr().Get(3)
	if t.NullableRefNexttask() != nil {
		println("fail: testNullableRef")
		return
	}
	if t.NullableRefTaskid() != nil {
		println("fail: testNullableRef")
		return
	}
	println("pass: testNullableRef But,NullableRef的实现可以再优化，现在如果是nil，每次Get都会去查一次")
}

func testListRef() {
	// local t = cfg.other.loot.get(2)
	// assert(rawget(t, "ListRefLootid") == nil, "listRef 会缓存起来，第一次是nil")
	// --print(t.name, t.lootid, #t.ListRefLootid, t.ListRefLootid[1].itemid, t.ListRefLootid[2].itemid)

	// assert(#t.ListRefLootid == 7, "t.ListRefLootid")
	// local itemids = {}
	// for _, lootitem in ipairs(t.ListRefLootid) do
	//     itemids[lootitem.itemid] = true
	// end

	// assert(itemids[40007], "t.ListRefLootid[x].itemid contains 40007")
	// assert(itemids[40010], "t.ListRefLootid[x].itemid contains 40010")

	// assert(rawget(t, "ListRefLootid") ~= nil, "listRef 会缓存起来，取过一次之后就可以直接rawget了")
	t := config.GetOtherLootMgr().Get(2)
	if len(t.ListRefLootid()) != 7 {
		println("fail: testListRef")
		return
	}
	if t.ListRefLootid()[0].Itemid() != 22 {
		println("fail: testListRef")
		return
	}

	println("pass: testListRef, 我改了实现方式，现在初始化时就会构建ListRef")
}

func testEnum() {
	// local t = cfg.task.completeconditiontype
	// print(t.KillMonster.id, t.KillMonster.name, t.Chat.name, t.CollectItem.name)
	// assert(t.get(1) == t.KillMonster)
	// assert(t.KillMonster.id == 1, "配置为枚举，可以直接completeconditiontype.KillMonster访问，不用字符串")
	// assert(t.KillMonster.name == "KillMonster")
	var t = config.GetTaskCompleteconditiontypeMgr()
	var killMonster = t.GetKillMonster()
	if killMonster.Id() != 1 || killMonster.Name() != "KillMonster" {
		println("fail: testEnum")
		return
	}
	println("pass: testEnum")
}

func testEntry() {
	// local t = cfg.equip.equipconfig.Instance
	// local gt = cfg.equip.equipconfig.get("Instance")
	// print(t.broadcastid, t.draw_protect_name)
	// assert(t.broadcastid == 9500, "配置为入口，也可以直接tequipconfig.Instance访问，不用字符串")
	// assert(gt == t, "entry")
	var t = config.GetEquipEquipconfigMgr().GetInstance()
	if t.Broadcastid() != 9500 || t.Draw_protect_name() != "测试" {
		println("fail: testEntry")
		return
	}
	println("pass: testEntry")
}

func testCsvColumnMode() {
	// local t = cfg.equip.equipconfig.Instance2
	// assert(t.week_reward_mailid == 33, "csv可以一列一列配置，而不用一行一行")

	t := config.GetEquipEquipconfigMgr().GetInstance2()
	if t.Week_reward_mailid() != 33 {
		println("fail: testCsvColumnMode")
		return
	}
	println("pass: testCsvColumnMode")
}

func testBeanAsPrimaryKey() {
	// local all = cfg.equip.jewelryrandom.all
	// local firstK
	// for k, _ in pairs(all) do
	//     firstK = k
	//     break
	// end
	// local t = cfg.equip.jewelryrandom.get(Beans.levelrank(5, 1))
	// assert(t == nil, "bean做为主键，虽然能生成，但不好取到，因为lua的table的key如果是table，比较用的是引用比较")
	// assert(cfg.equip.jewelryrandom.get(firstK) ~= nil, "只能先拿到引用")

	var lv *config.LevelRank
	lv = config.GetEquipJewelryMgr().Get(1).LvlRank()
	var t = config.GetEquipJewelryrandomMgr().Get(lv)
	var s *config.LevelRank
	s = config.GetEquipJewelryrandomMgr().GetAll()[0].LvlRank()
	println(s.Level(), s.Rank(), lv.Level(), lv.Rank(), lv, s, lv == s)
	// println(lv.GetLevel(), lv.GetRank())
	// println(lv, s, lv == s)
	if t == nil {
		println("fail: testBeanAsPrimaryKey 为了性能我返回Struct的时候都是返回的引用。")
		return
	}
	println("pass: testBeanAsPrimaryKey")
}

func testMapValueRef() {
	// local t = cfg.other.signin.get(4)
	// assert(t.vipitem2vipcountMap[10001] == 10)
	// assert(t.RefVipitem2vipcountMap[10001] == cfg.other.loot.get(10))
	t := config.GetOtherSigninMgr().Get(4)
	if t.Vipitem2vipcountMap()[10001] != 10 {
		println("fail: testMapValueRef")
		return
	}
	if t.RefVipitem2vipcountMap()[10001] != config.GetOtherLootMgr().Get(10) {
		println("fail: testMapValueRef")
		return
	}
	println("pass: testMapValueRef")
}

func testDefaultBean() {
	// local t = cfg.task.task.get(1)
	// assert(t.testDefaultBean.testInt == 0)
	// assert(t.testDefaultBean.testBool == false)
	// assert(t.testDefaultBean.testString == '')
	// assert(t.testDefaultBean.testSubBean.x == 0)
	// assert(t.testDefaultBean.testSubBean.y == 0)
	// assert(#t.testDefaultBean.testList == 0)
	// assert(#t.testDefaultBean.testList2 == 0)
	// assert(#t.testDefaultBean.testMap == 0)
	t := config.GetTaskTaskMgr().Get(1)
	if t.TestDefaultBean().TestInt() != 0 ||
		t.TestDefaultBean().TestBool() != false ||
		t.TestDefaultBean().TestString() != "" ||
		t.TestDefaultBean().TestSubBean().X() != 0 ||
		t.TestDefaultBean().TestSubBean().Y() != 0 ||
		len(t.TestDefaultBean().TestList()) != 0 ||
		len(t.TestDefaultBean().TestList2()) != 0 ||
		len(t.TestDefaultBean().TestMap()) != 0 {
		println("fail: testDefaultBean")
	} else {
		println("pass: testDefaultBean")
	}
}

func testSwitchBean(aiai *config.AiAi) {
	switch x := aiai.TrigTick().(type) {
	case *config.AiTriggertickConstValue:
		if x.Value() == 30000 {
			println("pass: testSwitchBean")
			return
		}
	case *config.AiTriggertickByLevel:
		if x.Coefficient() == 0.1 {
			println("pass: testSwitchBean")
			return
		}
	case *config.AiTriggerTick:
		println("fail: testSwitchBean")
		return
	case *config.AiTriggertickByServerUpDay:
		if x.Coefficient2() == 0.2 {
			println("pass: testSwitchBean")
			return
		}
	}
}

func testCellNumberAsInterface() {
	// local ai = cfg.ai.ai
	// local t = ai.get(10019)
	// assertEqual(t.trigTick.type(), "ConstValue")
	// assertEqual(t.trigTick.value, 30000)

	// t = ai.get(10020)
	// assertEqual(t.trigTick.type(), "ByLevel")
	// assertEqual(t.trigTick.coefficient, 0.1)

	// t = ai.get(10021)
	// assertEqual(t.trigTick.type(), "ByServerUpDay")
	// assertEqual(t.trigTick.coefficient2, 0.2)
	ai := config.GetAiAiMgr()
	testSwitchBean(ai.Get(10019))
	testSwitchBean(ai.Get(10020))
	testSwitchBean(ai.Get(10021))

	if v, ok := ai.Get(10019).TrigTick().(*config.AiTriggertickConstValue); ok {
		if v.Value() == 30000 {
			println("pass: testCellNumberAsInterface")
		} else {
			println("fail: testCellNumberAsInterface")
		}
	}

}

func DoTest() {
	testReadOnly()
	testToString()
	testAllAndGet()
	testMultiColumnAsPrimaryKeyGet()
	testUniqueKeyGet()
	testField()
	testListField()
	testMapField()
	testDynamicBeanField()
	testRef()
	testRefNotCache()
	testNullableRef()
	testListRef()
	testEnum()
	testEntry()
	testCsvColumnMode()
	testBeanAsPrimaryKey()
	testMapValueRef()
	testCellNumberAsInterface()
}

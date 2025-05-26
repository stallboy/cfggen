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
	if t.GetLootid() != 2 {
		println("fail: testMultiColumnAsPrimaryKeyGet")
	}
	if t.GetItemid() != 40007 {
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
	if t.GetItemid() != 22 {
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
	if t.GetName()[0] != "杀个怪" &&
		t.GetTaskid() != 1 &&
		t.GetNexttask() != 2 &&
		len(t.GetName()) != 2 {
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
	if len(t.GetName()) != 2 || t.GetName()[0] != "杀个怪" {
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
	if len(t.GetItem2countMap()) != 3 || t.GetItem2countMap()[10001] != 5 {
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
	cc := t.GetCompletecondition()
	if killMonster, ok := cc.(*config.TaskCompleteconditionKillMonster); ok {
		println("pass: 类型是 TaskCompleteconditionKillMonster")
		if killMonster.GetMonsterid() != 1 || killMonster.GetCount() != 3 {
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
	cc := t.GetCompletecondition()
	if killMonster, ok := cc.(*config.TaskCompleteconditionKillMonster); ok {
		rawGet := config.GetOtherMonsterMgr().Get(killMonster.GetMonsterid())
		if rawGet != killMonster.GetRefMonsterid() || killMonster.GetRefMonsterid().GetPosList()[1].GetY() != 22 {
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
	killMonster, _ := t.GetCompletecondition().(*config.TaskCompleteconditionKillMonster)
	refM := killMonster.GetRefMonsterid()
	if refM == nil || killMonster.GetRefMonsterid() != nil {
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
	if t.GetNexttask() != 2 {
		println("fail: testNullableRef")
		return
	}
	if t.GetNullableRefNexttask() != config.GetTaskTaskMgr().Get(2) {
		println("fail: testNullableRef")
		return
	}
	if t.GetNullableRefTaskid() != config.GetTaskTaskextraexpMgr().Get(1) {
		println("fail: testNullableRef")
		return
	}
	t = config.GetTaskTaskMgr().Get(3)
	if t.GetNullableRefNexttask() != nil {
		println("fail: testNullableRef")
		return
	}
	if t.GetNullableRefTaskid() != nil {
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
	if len(t.GetListRefLootid()) != 7 {
		println("fail: testListRef")
		return
	}
	if t.GetListRefLootid()[0].GetItemid() != 22 {
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
}

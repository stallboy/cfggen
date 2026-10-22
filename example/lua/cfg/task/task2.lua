local cfg = require "cfg._cfgs"
local Beans = require "cfg._beans"

---@class cfg.task.task2
---@field taskid number , 任务完成条件类型（id的范围为1-100）
---@field name table<number,text>
---@field nexttask number
---@field completecondition Beans.task.completecondition
---@field exp number
---@field testBool boolean
---@field testString string
---@field testStruct Beans.position
---@field testList table<number,number>
---@field testListStruct table<number,Beans.position>
---@field testListInterface table<number,Beans.ai.triggertick>
---@field get fun(taskid:number):cfg.task.task2
---@field all table<any,cfg.task.task2>
---@field NullableRefTaskid cfg.task.task_extra_exp
---@field NullableRefNexttask cfg.task.task

local this = cfg.task.task2

local mk = cfg[0].table('task.task2', this, {{'all','get',{'taskid'}}}, {fields={
    'taskid', -- int, 任务完成条件类型（id的范围为1-100）
    'name', -- list<text>
    'nexttask', -- int
    'completecondition', -- task.completecondition
    'exp', -- int
    'testBool', -- bool
    'testString', -- str
    'testStruct', -- Position
    'testList', -- list<int>
    'testListStruct', -- list<Position>
    'testListInterface' -- list<ai.TriggerTick>
    }, uniqkeys={ { 'all', 'get', 1 }, }, enumidx=nil, refs={
    { 'NullableRefTaskid', 0, cfg.task.task_extra_exp, 'get', 1 },
    { 'NullableRefNexttask', 0, cfg.task.task, 'get', 3 }, }}, nil)

local constvalue = Beans.ai.triggertick.constvalue
local position = Beans.position
local chat = Beans.task.completecondition.chat
local collectitem = Beans.task.completecondition.collectitem
local conditionand = Beans.task.completecondition.conditionand
local killmonster = Beans.task.completecondition.killmonster
local talknpc = Beans.task.completecondition.talknpc
local testnocolumn = Beans.task.completecondition.testnocolumn

local function E() return setmetatable({}, getmetatable(cfg._mk.E)) end

mk(1, {"测试用"}, 2, cfg[0].node("task.completecondition.ConditionAnd", conditionand(cfg[0].node("task.completecondition.ConditionAnd", conditionand(cfg[0].node("task.completecondition.KillMonster", killmonster(1, 0)), cfg[0].node("task.completecondition.CollectItem", collectitem(0, 0)))), cfg[0].node("task.completecondition.TalkNpc", talknpc(0)))), 10001, true, "", position(1, 0, 0), {12}, {position(1, 2, 3)}, {cfg[0].node("ai.TriggerTick.ConstValue", constvalue(1234))})
mk(2, {"和npc对话", "和npc对话"}, 3, cfg[0].node("task.completecondition.TalkNpc", talknpc(1)), 2000, false, "", position(0, 0, 0), E(), E(), E())
mk(3, {"收集物品", "收集物品"}, 0, cfg[0].node("task.completecondition.CollectItem", collectitem(11, 1)), 3000, false, "", position(0, 0, 0), E(), E(), E())
mk(4, {"杀怪并且收集物品", "杀怪并且收集物品"}, 0, cfg[0].node("task.completecondition.ConditionAnd", conditionand(cfg[0].node("task.completecondition.KillMonster", killmonster(1, 3)), cfg[0].node("task.completecondition.CollectItem", collectitem(11, 1)))), 4000, false, "", position(0, 0, 0), E(), E(), E())
mk(5, {"杀怪对话并且收集物品", "杀怪对话并且收集物品"}, 0, cfg[0].node("task.completecondition.ConditionAnd", conditionand(cfg[0].node("task.completecondition.ConditionAnd", conditionand(cfg[0].node("task.completecondition.KillMonster", killmonster(1, 3)), cfg[0].node("task.completecondition.TalkNpc", talknpc(1)))), cfg[0].node("task.completecondition.CollectItem", collectitem(11, 1)))), 5000, false, "", position(0, 0, 0), E(), E(), E())
mk(6, {"聊天并且杀怪", "测试转义符号"}, 0, cfg[0].node("task.completecondition.ConditionAnd", conditionand(cfg[0].node("task.completecondition.Chat", chat("葵花宝典,123")), cfg[0].node("task.completecondition.KillMonster", killmonster(1, 3)))), 5000, false, "", position(0, 0, 0), E(), E(), E())
mk(7, {"测试", "测试无参数得bean"}, 0, cfg[0].node("task.completecondition.TestNoColumn", testnocolumn()), 2000, false, "", position(0, 0, 0), E(), E(), E())
mk(8, {"测试2", "测试默认bean"}, 0, cfg[0].node("task.completecondition.TestNoColumn", testnocolumn()), 3000, false, "", position(0, 0, 0), E(), E(), E())

cfg[0].complete('task.task2')
return this

local cfg = require "cfg._cfgs"
local Beans = require "cfg._beans"

---@class cfg.task.task
---@field taskid number , 任务完成条件类型（id的范围为1-100）
---@field name table<number,text> , 程序用名字
---@field nexttask number
---@field completecondition Beans.task.completecondition
---@field exp number
---@field testDefaultBean Beans.task.testdefaultbean , 测试
---@field get fun(taskid:number):cfg.task.task
---@field all table<any,cfg.task.task>
---@field NullableRefTaskid cfg.task.task_extra_exp
---@field NullableRefNexttask cfg.task.task

local this = cfg.task.task

local mk = cfg[0].table('task.task', this, {{'all','get',{'taskid'}}}, {fields={
    'taskid', -- int, 任务完成条件类型（id的范围为1-100）
    'name', -- list<text>, 程序用名字
    'nexttask', -- int
    'completecondition', -- task.completecondition
    'exp', -- int
    'testDefaultBean' -- TestDefaultBean, 测试
    }, uniqkeys={ { 'all', 'get', 1 }, }, enumidx=nil, refs={
    { 'NullableRefTaskid', 0, cfg.task.task_extra_exp, 'get', 1 },
    { 'NullableRefNexttask', 0, cfg.task.task, 'get', 3 }, }}, nil)

local position = Beans.position
local chat = Beans.task.completecondition.chat
local collectitem = Beans.task.completecondition.collectitem
local conditionand = Beans.task.completecondition.conditionand
local killmonster = Beans.task.completecondition.killmonster
local talknpc = Beans.task.completecondition.talknpc
local testnocolumn = Beans.task.completecondition.testnocolumn
local testdefaultbean = Beans.task.testdefaultbean

local function E() return setmetatable({}, getmetatable(cfg._mk.E)) end

mk(1, {"杀个怪", "杀怪"}, 2, cfg[0].node("task.completecondition.KillMonster", killmonster(1, 3)), 1000, testdefaultbean(0, false, "", position(0, 0, 0), E(), E(), E()))
mk(2, {"和npc对话", "和npc对话"}, 3, cfg[0].node("task.completecondition.TalkNpc", talknpc(1)), 2000, testdefaultbean(22, false, "text", position(3, 4, 5), {11, 22}, {3, 4, 5}, {[1] = "str in map"}))
mk(3, {"收集物品", "收集物品"}, 0, cfg[0].node("task.completecondition.CollectItem", collectitem(11, 1)), 3000, testdefaultbean(0, false, "", position(0, 0, 0), E(), E(), E()))
mk(4, {"杀怪并且收集物品", "杀怪并且收集物品"}, 0, cfg[0].node("task.completecondition.ConditionAnd", conditionand(cfg[0].node("task.completecondition.KillMonster", killmonster(1, 3)), cfg[0].node("task.completecondition.CollectItem", collectitem(11, 1)))), 4000, testdefaultbean(0, false, "", position(0, 0, 0), E(), E(), E()))
mk(5, {"杀怪对话并且收集物品", "杀怪对话并且收集物品"}, 0, cfg[0].node("task.completecondition.ConditionAnd", conditionand(cfg[0].node("task.completecondition.ConditionAnd", conditionand(cfg[0].node("task.completecondition.KillMonster", killmonster(1, 3)), cfg[0].node("task.completecondition.TalkNpc", talknpc(1)))), cfg[0].node("task.completecondition.CollectItem", collectitem(11, 1)))), 5000, testdefaultbean(0, false, "", position(0, 0, 0), E(), E(), E()))
mk(6, {"聊天并且杀怪", "测试转义符号"}, 0, cfg[0].node("task.completecondition.ConditionAnd", conditionand(cfg[0].node("task.completecondition.Chat", chat("葵花宝典,123")), cfg[0].node("task.completecondition.KillMonster", killmonster(1, 3)))), 5000, testdefaultbean(0, false, "", position(0, 0, 0), E(), E(), E()))
mk(7, {"测试", "测试无参数得bean"}, 0, cfg[0].node("task.completecondition.TestNoColumn", testnocolumn()), 2000, testdefaultbean(0, false, "", position(0, 0, 0), E(), E(), E()))
mk(8, {"测试2", "测试默认bean"}, 0, cfg[0].node("task.completecondition.TestNoColumn", testnocolumn()), 3000, testdefaultbean(0, false, "", position(0, 0, 0), E(), E(), E()))

cfg[0].complete('task.task')
return this

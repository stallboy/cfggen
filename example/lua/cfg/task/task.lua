local cfg = require "cfg._cfgs"
local Beans = cfg._beans

---@class cfg.task.task
---@field taskid number , 任务完成条件类型（id的范围为1-100）
---@field name table<number,text> , 程序用名字
---@field nexttask number 
---@field completecondition Beans.task.completecondition 
---@field exp number 
---@field testDefaultBean Beans.task.testdefaultbean , 测试
---@field get fun(taskid:number):cfg.task.task
---@field all table<any,cfg.task.task>
---@field NullableRefTaskid cfg.task.taskextraexp
---@field NullableRefNexttask cfg.task.task

local this = cfg.task.task

local mk = cfg._mk.table(this, { { 'all', 'get', 1 }, }, nil, { 
    { 'NullableRefTaskid', 0, cfg.task.taskextraexp, 'get', 1 }, 
    { 'NullableRefNexttask', 0, cfg.task.task, 'get', 3 }, }, 
    'taskid', -- int, 任务完成条件类型（id的范围为1-100）
    'name', -- list<text>, 程序用名字
    'nexttask', -- int
    'completecondition', -- task.completecondition
    'exp', -- int
    'testDefaultBean' -- TestDefaultBean, 测试
    )

local position = Beans.position
local chat = Beans.task.completecondition.chat
local collectitem = Beans.task.completecondition.collectitem
local conditionand = Beans.task.completecondition.conditionand
local killmonster = Beans.task.completecondition.killmonster
local talknpc = Beans.task.completecondition.talknpc
local testnocolumn = Beans.task.completecondition.testnocolumn
local testdefaultbean = Beans.task.testdefaultbean

local E = cfg._mk.E
local R = cfg._mk.R

local A = {}
A[1] = killmonster(1, 3)
A[2] = talknpc(1)
A[3] = collectitem(11, 1)
A[4] = testdefaultbean(0, false, "", position(0, 0, 0), E, E, E)
A[5] = testnocolumn

mk(1, R({"杀个怪", "杀怪"}), 2, A[1], 1000, A[4])
mk(2, R({"和npc对话", "和npc对话"}), 3, A[2], 2000, testdefaultbean(22, false, "text", position(3, 4, 5), R({11, 22}), R({3, 4, 5}), R({[1] = "str in map"})))
mk(3, R({"收集物品", "收集物品"}), 0, A[3], 3000, A[4])
mk(4, R({"杀怪并且收集物品", "杀怪并且收集物品"}), 0, conditionand(A[1], A[3]), 4000, A[4])
mk(5, R({"杀怪对话并且收集物品", "杀怪对话并且收集物品"}), 0, conditionand(conditionand(A[1], A[2]), A[3]), 5000, A[4])
mk(6, R({"聊天并且杀怪", "测试转义符号"}), 0, conditionand(chat("葵花宝典,123"), A[1]), 5000, A[4])
mk(7, R({"测试", "测试无参数得bean"}), 0, A[5], 2000, A[4])
mk(8, R({"测试2", "测试默认bean"}), 0, A[5], 3000, A[4])

return this

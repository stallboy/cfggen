local cfg = require "cfg._cfgs"
local Beans = cfg._beans

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
---@field NullableRefTaskid cfg.task.taskextraexp
---@field NullableRefNexttask cfg.task.task

local this = cfg.task.task2

local mk = cfg._mk.i18n_table(this, { { 'all', 'get', 1 }, }, nil, { 
    { 'NullableRefTaskid', 0, cfg.task.taskextraexp, 'get', 1 }, 
    { 'NullableRefNexttask', 0, cfg.task.task, 'get', 3 }, }, 
    { name = 2 },
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
    )

local constvalue = Beans.ai.triggertick.constvalue
local position = Beans.position
local chat = Beans.task.completecondition.chat
local collectitem = Beans.task.completecondition.collectitem
local conditionand = Beans.task.completecondition.conditionand
local killmonster = Beans.task.completecondition.killmonster
local talknpc = Beans.task.completecondition.talknpc
local testnocolumn = Beans.task.completecondition.testnocolumn

local E = cfg._mk.E

local A = {}
A[1] = killmonster(1, 3)
A[2] = talknpc(1)
A[3] = collectitem(11, 1)
A[4] = position(0, 0, 0)
A[5] = testnocolumn

mk(1, {40}, 2, conditionand(conditionand(killmonster(1, 0), collectitem(0, 0)), talknpc(0)), 10001, true, "", position(1, 0, 0), {12}, {position(1, 2, 3)}, {constvalue(1234)})
mk(2, {41, 42}, 3, A[2], 2000, false, "", A[4], E, E, E)
mk(3, {43, 44}, 0, A[3], 3000, false, "", A[4], E, E, E)
mk(4, {45, 46}, 0, conditionand(A[1], A[3]), 4000, false, "", A[4], E, E, E)
mk(5, {47, 48}, 0, conditionand(conditionand(A[1], A[2]), A[3]), 5000, false, "", A[4], E, E, E)
mk(6, {49, 50}, 0, conditionand(chat("葵花宝典,123"), A[1]), 5000, false, "", A[4], E, E, E)
mk(7, {51, 52}, 0, A[5], 2000, false, "", A[4], E, E, E)
mk(8, {53, 54}, 0, A[5], 3000, false, "", A[4], E, E, E)

return this

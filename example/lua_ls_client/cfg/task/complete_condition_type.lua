local cfg = require "cfg._cfgs"

---@class cfg.task.complete_condition_type
---@field id number , 任务完成条件类型（id的范围为1-100）
---@field name string , 程序用名字
---@field get fun(id:number):cfg.task.complete_condition_type
---@field KillMonster cfg.task.complete_condition_type
---@field TalkNpc cfg.task.complete_condition_type
---@field CollectItem cfg.task.complete_condition_type
---@field ConditionAnd cfg.task.complete_condition_type
---@field Chat cfg.task.complete_condition_type
---@field TestNoColumn cfg.task.complete_condition_type
---@field aa cfg.task.complete_condition_type
---@field all table<any,cfg.task.complete_condition_type>

local this = cfg.task.complete_condition_type

local mk = cfg[0].table('task.complete_condition_type', this, {{'all','get',{'id'}}}, {fields={
    'id', -- int, 任务完成条件类型（id的范围为1-100）
    'name' -- str, 程序用名字
    }, uniqkeys={ { 'all', 'get', 1 }, }, enumidx=2, refs=nil}, nil)

mk(1, "KillMonster")
mk(2, "TalkNpc")
mk(3, "CollectItem")
mk(4, "ConditionAnd")
mk(5, "Chat")
mk(6, "TestNoColumn")
mk(7, "aa")

cfg[0].complete('task.complete_condition_type')
return this

local cfg = require "cfg._cfgs"

---@class cfg.task.task_extra_exp
---@field taskid number , 任务完成条件类型（id的范围为1-100）
---@field extraexp number , 额外奖励经验
---@field test1 string
---@field test2 string
---@field fielda string
---@field fieldb string
---@field fieldc string
---@field fieldd string
---@field get fun(taskid:number):cfg.task.task_extra_exp
---@field all table<any,cfg.task.task_extra_exp>

local this = cfg.task.task_extra_exp

local mk = cfg[0].table('task.task_extra_exp', this, {{'all','get',{'taskid'}}}, {fields={
    'taskid', -- int, 任务完成条件类型（id的范围为1-100）
    'extraexp', -- int, 额外奖励经验
    'test1', -- str
    'test2', -- str
    'fielda', -- str
    'fieldb', -- str
    'fieldc', -- str
    'fieldd' -- str
    }, uniqkeys={ { 'all', 'get', 1 }, }, enumidx=nil, refs=nil}, nil)

mk(1, 1000, "1", "2", "3", "cc", "ee", "dd")

cfg[0].complete('task.task_extra_exp')
return this

local cfg = require "cfg._cfgs"

---@class cfg.task.taskextraexp
---@field taskid number , 任务完成条件类型（id的范围为1-100）
---@field extraexp number , 额外奖励经验
---@field test1 string 
---@field test2 string 
---@field fielda string 
---@field fieldb string 
---@field fieldc string 
---@field fieldd string 
---@field get fun(taskid:number):cfg.task.taskextraexp
---@field all table<any,cfg.task.taskextraexp>

local this = cfg.task.taskextraexp

local mk = cfg._mk.table(this, { { 'all', 'get', 1 }, }, nil, nil, 
    'taskid', -- int, 任务完成条件类型（id的范围为1-100）
    'extraexp', -- int, 额外奖励经验
    'test1', -- str
    'test2', -- str
    'fielda', -- str
    'fieldb', -- str
    'fieldc', -- str
    'fieldd' -- str
    )

mk(1, 1000, "1", "2", "3", "cc", "ee", "dd")

return this

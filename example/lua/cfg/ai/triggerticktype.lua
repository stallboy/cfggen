local cfg = require "cfg._cfgs"

---@class cfg.ai.triggerticktype
---@field enumName string , 枚举名称
---@field get fun(enumName:string):cfg.ai.triggerticktype
---@field ConstValue cfg.ai.triggerticktype
---@field ByLevel cfg.ai.triggerticktype
---@field ByServerUpDay cfg.ai.triggerticktype
---@field all table<any,cfg.ai.triggerticktype>

local this = cfg.ai.triggerticktype

local mk = cfg._mk.table(this, { { 'all', 'get', 1 }, }, 1, nil, 
    'enumName' -- str, 枚举名称
    )

mk("ConstValue")
mk("ByLevel")
mk("ByServerUpDay")

return this

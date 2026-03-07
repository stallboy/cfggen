local cfg = require "cfg._cfgs"

---@class cfg.other.argcapturemode
---@field name string 
---@field comment string 
---@field get fun(name:string):cfg.other.argcapturemode
---@field Snapshot cfg.other.argcapturemode
---@field Dynamic cfg.other.argcapturemode
---@field all table<any,cfg.other.argcapturemode>

local this = cfg.other.argcapturemode

local mk = cfg._mk.table(this, { { 'all', 'get', 1 }, }, 1, nil, 
    'name', -- str
    'comment' -- str
    )

mk("Snapshot", "快照模式")
mk("Dynamic", "动态模式")

return this

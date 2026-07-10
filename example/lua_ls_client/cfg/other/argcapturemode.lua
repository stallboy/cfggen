local cfg = require "cfg._cfgs"

---@class cfg.other.argcapturemode
---@field name string 
---@field id number 
---@field comment text 
---@field get fun(name:string):cfg.other.argcapturemode
---@field getById fun(id:number):cfg.other.argcapturemode
---@field Snapshot cfg.other.argcapturemode
---@field Dynamic cfg.other.argcapturemode
---@field all table<any,cfg.other.argcapturemode>

local this = cfg.other.argcapturemode

local mk = cfg._mk.i18n_table(this, { { 'all', 'get', 1 }, { 'IdMap', 'getById', 2 }, }, 1, nil, 
    { comment = 1 },
    'name', -- str
    'id', -- int
    'comment' -- text
    )

mk("Snapshot", 1, 9)
mk("Dynamic", 2, 10)

return this

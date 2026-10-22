local cfg = require "cfg._cfgs"

---@class cfg.other.argcapturemode
---@field name string
---@field id number
---@field comment string
---@field get fun(name:string):cfg.other.argcapturemode
---@field getById fun(id:number):cfg.other.argcapturemode
---@field Snapshot cfg.other.argcapturemode
---@field Dynamic cfg.other.argcapturemode
---@field all table<any,cfg.other.argcapturemode>

local this = cfg.other.argcapturemode

local mk = cfg[0].table('other.ArgCaptureMode', this, {{'all','get',{'name'}},{'IdMap','getById',{'id'}}}, {fields={
    'name', -- str
    'id', -- int
    'comment' -- str
    }, uniqkeys={ { 'all', 'get', 1 }, { 'IdMap', 'getById', 2 }, }, enumidx=1, refs=nil}, nil)

mk("Snapshot", 1, "快照模式")
mk("Dynamic", 2, "动态模式")

cfg[0].complete('other.ArgCaptureMode')
return this

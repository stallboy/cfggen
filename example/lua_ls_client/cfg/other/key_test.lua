local cfg = require "cfg._cfgs"

---@class cfg.other.key_test
---@field id1 number 
---@field id2 number 
---@field id3 number 
---@field ids table<number,number> 
---@field enumTest string 
---@field enumList table<number,string> 
---@field get fun(id1:number,id2:number):cfg.other.key_test
---@field getById1Id3 fun(id1:number,id3:number):cfg.other.key_test
---@field getById2 fun(id2:number):cfg.other.key_test
---@field getById2Id3 fun(id2:number,id3:number):cfg.other.key_test
---@field all table<any,cfg.other.key_test>
---@field RefIds table<number,cfg.other.sign_in>
---@field RefEnumTest cfg.other.argcapturemode
---@field RefEnumList table<number,cfg.other.argcapturemode>

local this = cfg.other.key_test

local mk = cfg._mk.table(this, { { 'all', 'get', 1, 2 }, { 'Id1Id3Map', 'getById1Id3', 1, 3 }, { 'Id2Map', 'getById2', 2 }, { 'Id2Id3Map', 'getById2Id3', 2, 3 }, }, nil, { 
    { 'RefIds', 1, cfg.other.sign_in, 'get', 4 }, 
    { 'RefEnumTest', 0, cfg.other.argcapturemode, 'get', 5 }, 
    { 'RefEnumList', 1, cfg.other.argcapturemode, 'get', 6 }, }, 
    'id1', -- int
    'id2', -- long
    'id3', -- int
    'ids', -- list<int>
    'enumTest', -- ArgCaptureMode
    'enumList' -- list<ArgCaptureMode>
    )

local E = cfg._mk.E

mk(0, 0, 0, E, "Snapshot", E)
mk(1, 2, 3, {11, 12}, "Dynamic", E)

return this

local cfg = require "cfg._cfgs"

---@class cfg.other.keytest
---@field id1 number 
---@field id2 number 
---@field id3 number 
---@field ids table<number,number> 
---@field get fun(id1:number,id2:number):cfg.other.keytest
---@field getById1Id3 fun(id1:number,id3:number):cfg.other.keytest
---@field getById2 fun(id2:number):cfg.other.keytest
---@field getById2Id3 fun(id2:number,id3:number):cfg.other.keytest
---@field all table<any,cfg.other.keytest>
---@field RefIds table<number,cfg.other.signin>

local this = cfg.other.keytest

local mk = cfg._mk.table(this, { { 'all', 'get', 1, 2 }, { 'Id1Id3Map', 'getById1Id3', 1, 3 }, { 'Id2Map', 'getById2', 2 }, { 'Id2Id3Map', 'getById2Id3', 2, 3 }, }, nil, { 
    { 'RefIds', 1, cfg.other.signin, 'get', 4 }, }, 
    'id1', -- int
    'id2', -- long
    'id3', -- int
    'ids' -- list<int>
    )

local E = cfg._mk.E
local R = cfg._mk.R

mk(0, 0, 0, E)
mk(1, 2, 3, R({11, 12}))

return this

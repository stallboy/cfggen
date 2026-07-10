local cfg = require "cfg._cfgs"
local Beans = cfg._beans

---@class cfg.other.monster
---@field id number 
---@field posList table<number,Beans.position> 
---@field lootId number , loot
---@field lootItemId number , item
---@field enumMap1 table<string,number> 
---@field enumMap2 table<number,string> 
---@field get fun(id:number):cfg.other.monster
---@field all table<any,cfg.other.monster>
---@field RefLoot cfg.other.lootitem
---@field RefAllLoot cfg.other.loot
---@field RefEnumMap2 cfg.other.argcapturemode

local this = cfg.other.monster

local mk = cfg._mk.table(this, { { 'all', 'get', 1 }, }, nil, { 
    { 'RefLoot', 0, cfg.other.lootitem, 'get', 3, 4 }, 
    { 'RefAllLoot', 0, cfg.other.loot, 'get', 3 }, 
    { 'RefEnumMap2', 3, cfg.other.argcapturemode, 'get', 6 }, }, 
    'id', -- int
    'posList', -- list<Position>
    'lootId', -- int, loot
    'lootItemId', -- int, item
    'enumMap1', -- map<str,int>
    'enumMap2' -- map<int,ArgCaptureMode>
    )

local position = Beans.position

local E = cfg._mk.E

mk(1, {position(1, 2, 3), position(11, 22, 33), position(111, 222, 333)}, 2, 40005, E, E)
mk(2, {position(33, 44, 55)}, 2, 40006, E, E)

return this

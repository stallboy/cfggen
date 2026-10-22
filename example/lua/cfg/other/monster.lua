local cfg = require "cfg._cfgs"
local Beans = require "cfg._beans"

---@class cfg.other.monster
---@field id number
---@field posList table<number,Beans.position>
---@field lootId number , loot
---@field lootItemId number , item
---@field enumMap1 table<string,number>
---@field enumMap2 table<number,string>
---@field get fun(id:number):cfg.other.monster
---@field all table<any,cfg.other.monster>
---@field RefLoot cfg.other.loot_item
---@field RefAllLoot cfg.other.loot
---@field RefEnumMap2 cfg.other.argcapturemode

local this = cfg.other.monster

local mk = cfg[0].table('other.monster', this, {{'all','get',{'id'}}}, {fields={
    'id', -- int
    'posList', -- list<Position>
    'lootId', -- int, loot
    'lootItemId', -- int, item
    'enumMap1', -- map<str,int>
    'enumMap2' -- map<int,ArgCaptureMode>
    }, uniqkeys={ { 'all', 'get', 1 }, }, enumidx=nil, refs={
    { 'RefLoot', 0, cfg.other.loot_item, 'get', 3, 4 },
    { 'RefAllLoot', 0, cfg.other.loot, 'get', 3 },
    { 'RefEnumMap2', 3, cfg.other.argcapturemode, 'get', 6 }, }}, nil)

local position = Beans.position

local function E() return setmetatable({}, getmetatable(cfg._mk.E)) end

mk(1, {position(1, 2, 3), position(11, 22, 33), position(111, 222, 333)}, 2, 40005, E(), E())
mk(2, {position(33, 44, 55)}, 2, 40006, E(), E())

cfg[0].complete('other.monster')
return this

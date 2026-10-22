local cfg = require "cfg._cfgs"

---@class cfg.other.loot
---@field lootid number , 序号
---@field ename string
---@field name text , 名字
---@field chanceList table<number,number> , 掉落0件物品的概率
---@field get fun(lootid:number):cfg.other.loot
---@field all table<any,cfg.other.loot>
---@field ListRefLootid table<number,cfg.other.loot_item>
---@field ListRefAnotherWay table<number,cfg.other.loot_item>

local this = cfg.other.loot

local mk = cfg[0].table('other.loot', this, {{'all','get',{'lootid'}}}, {fields={
    'lootid', -- int, 序号
    'ename', -- str
    'name', -- text, 名字
    'chanceList' -- list<int>, 掉落0件物品的概率
    }, uniqkeys={ { 'all', 'get', 1 }, }, enumidx=nil, refs={
    { 'ListRefLootid', 2, cfg.other.loot_item, 'all', 1, 1 },
    { 'ListRefAnotherWay', 2, cfg.other.loot_item, 'all', 1, 1 }, }}, { name = 1 })

mk(1, "", 12, {100, 200, 200, 200, 200, 50, 50})
mk(2, "combo1", 13, {0, 100, 0, 0, 0, 0, 0})
mk(3, "combo2", 14, {0, 100, 0, 0, 0, 0, 0})
mk(4, "combo3", 15, {0, 100, 0, 0, 0, 0, 0})
mk(5, "", 16, {20, 10, 10, 20, 20, 10, 10})
mk(6, "", 17, {0, 100, 0, 0, 0, 0, 0})
mk(7, "", 18, {0, 100, 0, 0, 0, 0, 0})
mk(8, "", 19, {80, 20, 0, 0, 0, 0, 0})
mk(9, "", 20, {80, 20, 0, 0, 0, 0, 0})
mk(10, "", 21, {80, 20, 0, 0, 0, 0, 0})

cfg[0].complete('other.loot')
return this

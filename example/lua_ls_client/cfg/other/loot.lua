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

local mk = cfg._mk.i18n_table(this, { { 'all', 'get', 1 }, }, nil, {
    { 'ListRefLootid', 2, cfg.other.loot_item, 'all', 1, 1 },
    { 'ListRefAnotherWay', 2, cfg.other.loot_item, 'all', 1, 1 }, },
    { name = 1 },
    'lootid', -- int, 序号
    'ename', -- str
    'name', -- text, 名字
    'chanceList' -- list<int>, 掉落0件物品的概率
    )

local A = {}
A[1] = {0, 100, 0, 0, 0, 0, 0}
A[2] = {80, 20, 0, 0, 0, 0, 0}

mk(1, "", 13, {100, 200, 200, 200, 200, 50, 50})
mk(2, "combo1", 14, A[1])
mk(3, "combo2", 15, A[1])
mk(4, "combo3", 16, A[1])
mk(5, "", 17, {20, 10, 10, 20, 20, 10, 10})
mk(6, "", 18, A[1])
mk(7, "", 19, A[1])
mk(8, "", 20, A[2])
mk(9, "", 21, A[2])
mk(10, "", 22, A[2])

return this

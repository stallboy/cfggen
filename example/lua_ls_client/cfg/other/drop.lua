local cfg = require "cfg._cfgs"
local Beans = cfg._beans

---@class cfg.other.drop
---@field dropid number , 序号
---@field name text , 名字
---@field items table<number,Beans.other.dropitem> , 掉落概率
---@field testmap table<number,number> , 测试map block
---@field get fun(dropid:number):cfg.other.drop
---@field all table<any,cfg.other.drop>

local this = cfg.other.drop

local mk = cfg._mk.i18n_table(this, { { 'all', 'get', 1 }, }, nil, nil, 
    { name = 1 },
    'dropid', -- int, 序号
    'name', -- text, 名字
    'items', -- list<DropItem>, 掉落概率
    'testmap' -- map<int,int>, 测试map block
    )

local dropitem = Beans.other.dropitem

local E = cfg._mk.E
local R = cfg._mk.R

mk(1, 9, R({dropitem(100, R({1001, 1002, 1003}), 10, 20), dropitem(10, R({2001}), 10, 10), dropitem(10, R({2002}), 0, 1), dropitem(50, R({3001}), 1, 1)}), R({[1] = 11, [3] = 33, [5] = 55}))
mk(2, 10, R({dropitem(100, R({10001}), 1, 1)}), E)
mk(3, 11, R({dropitem(80, R({20001}), 10, 20)}), E)

return this

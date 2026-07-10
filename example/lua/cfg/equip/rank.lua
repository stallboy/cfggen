local cfg = require "cfg._cfgs"

---@class cfg.equip.rank
---@field rankID number , 稀有度
---@field rankName string , 程序用名字
---@field rankShowName string , 显示名称
---@field get fun(RankID:number):cfg.equip.rank
---@field white cfg.equip.rank
---@field green cfg.equip.rank
---@field blue cfg.equip.rank
---@field purple cfg.equip.rank
---@field yellow cfg.equip.rank
---@field red cfg.equip.rank
---@field all table<any,cfg.equip.rank>

local this = cfg.equip.rank

local mk = cfg._mk.table(this, { { 'all', 'get', 1 }, }, 2, nil, 
    'rankID', -- int, 稀有度
    'rankName', -- str, 程序用名字
    'rankShowName' -- str, 显示名称
    )

mk(0, "white", "下品")
mk(1, "green", "中品")
mk(2, "blue", "上品")
mk(3, "purple", "绝品")
mk(4, "yellow", "准神")
mk(5, "red", "神")

return this

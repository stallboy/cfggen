local cfg = require "cfg._cfgs"

---@class cfg.equip.jewelry_type
---@field typeName string , 程序用名字
---@field get fun(TypeName:string):cfg.equip.jewelry_type
---@field Jade cfg.equip.jewelry_type
---@field Bracelet cfg.equip.jewelry_type
---@field Magic cfg.equip.jewelry_type
---@field Bottle cfg.equip.jewelry_type
---@field all table<any,cfg.equip.jewelry_type>

local this = cfg.equip.jewelry_type

local mk = cfg._mk.table(this, { { 'all', 'get', 1 }, }, 1, nil, 
    'typeName' -- str, 程序用名字
    )

mk("Jade")
mk("Bracelet")
mk("Magic")
mk("Bottle")

return this

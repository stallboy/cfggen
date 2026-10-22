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

local mk = cfg[0].table('equip.jewelry_type', this, {{'all','get',{'typeName'}}}, {fields={
    'typeName' -- str, 程序用名字
    }, uniqkeys={ { 'all', 'get', 1 }, }, enumidx=1, refs=nil}, nil)

mk("Jade")
mk("Bracelet")
mk("Magic")
mk("Bottle")

cfg[0].complete('equip.jewelry_type')
return this

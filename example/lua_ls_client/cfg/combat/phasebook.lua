local cfg = require "cfg._cfgs"
local Beans = require "cfg._beans"

---@class cfg.combat.phasebook
---@field school string
---@field tier number
---@field airborne boolean
---@field code string
---@field edition number
---@field entry Beans.combat.bookentry
---@field get fun(school:string,tier:number,airborne:boolean):cfg.combat.phasebook
---@field getByCodeEdition fun(code:string,edition:number):cfg.combat.phasebook
---@field all table<any,cfg.combat.phasebook>

local this = cfg.combat.phasebook

local mk = cfg[0].table('combat.phasebook', this, {{'all','get',{'school','tier','airborne'}},{'CodeEditionMap','getByCodeEdition',{'code','edition'}}}, {fields={
    'school', -- str
    'tier', -- int
    'airborne', -- bool
    'code', -- str
    'edition', -- int
    'entry' -- BookEntry
    }, uniqkeys={ { 'all', 'get', 1, 2, 3 }, { 'CodeEditionMap', 'getByCodeEdition', 4, 5 }, }, enumidx=nil, refs=nil}, nil)

local bookentry = Beans.combat.bookentry

mk("Fire", 0, false, "Starter", 1, bookentry("Grounded Fireball"))

cfg[0].complete('combat.phasebook')
return this

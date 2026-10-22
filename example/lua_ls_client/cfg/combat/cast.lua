local cfg = require "cfg._cfgs"
local Beans = require "cfg._beans"

---@class cfg.combat.cast
---@field id number
---@field abilityCode string
---@field origin Beans.combat.origin
---@field uses table<number,Beans.combat.abilityuse>
---@field optional table<number,Beans.combat.optionalaim>
---@field school string
---@field tier number
---@field airborne boolean
---@field bookCode string
---@field edition number
---@field rewardKey Beans.combat.phasekey
---@field get fun(id:number):cfg.combat.cast
---@field all table<any,cfg.combat.cast>
---@field RefBook Beans.combat.bookentry
---@field RefBookByCode Beans.combat.bookentry
---@field RefBookRecord cfg.combat.phasebook
---@field RefReward Beans.combat.reward

local this = cfg.combat.cast

local mk = cfg[0].table('combat.cast', this, {{'all','get',{'id'}}}, {fields={
    'id', -- int
    'abilityCode', -- str
    'origin', -- Origin
    'uses', -- list<AbilityUse>
    'optional', -- list<OptionalAim>
    'school', -- str
    'tier', -- int
    'airborne', -- bool
    'bookCode', -- str
    'edition', -- int
    'rewardKey' -- PhaseKey
    }, uniqkeys={ { 'all', 'get', 1 }, }, enumidx=nil, refs=nil}, nil)

local abilityuse = Beans.combat.abilityuse
local binding = Beans.combat.binding
local machineuse = Beans.combat.machineuse
local optionalaim = Beans.combat.optionalaim
local origin = Beans.combat.origin
local phasekey = Beans.combat.phasekey
local phaseuse = Beans.combat.phaseuse

mk(1, "Fireball", origin(10, 20), {abilityuse(binding(1001), {machineuse("Combat", {phaseuse("Idle"), phaseuse("Hit"), phaseuse("")}), machineuse("Movement", {phaseuse("Idle")})})}, {optionalaim(9999, "Combat", "Idle"), optionalaim(1001, "Missing", "Idle"), optionalaim(1001, "Combat", "Missing"), optionalaim(1001, "Combat", "")}, "Fire", 0, false, "Starter", 1, phasekey("Hit", 0))

cfg[0].complete('combat.cast')
return this

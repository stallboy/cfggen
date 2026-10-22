local cfg = require "cfg._cfgs"
local Beans = require "cfg._beans"

---@class cfg.combat.ability
---@field id number
---@field code string
---@field origin Beans.combat.origin
---@field machines table<number,Beans.combat.machine>
---@field startMachine string
---@field startPhase string
---@field get fun(id:number):cfg.combat.ability
---@field getByCode fun(code:string):cfg.combat.ability
---@field all table<any,cfg.combat.ability>
---@field RefInitial Beans.combat.phase
---@field RefSpawn Beans.combat.origin

local this = cfg.combat.ability

local mk = cfg[0].table('combat.ability', this, {{'all','get',{'id'}},{'CodeMap','getByCode',{'code'}}}, {fields={
    'id', -- int
    'code', -- str
    'origin', -- Origin
    'machines', -- list<Machine>
    'startMachine', -- str
    'startPhase' -- str
    }, uniqkeys={ { 'all', 'get', 1 }, { 'CodeMap', 'getByCode', 2 }, }, enumidx=nil, refs=nil}, nil)

local anchor = Beans.combat.anchor
local anchoruse = Beans.combat.anchoruse
local combo = Beans.combat.combo
local damage = Beans.combat.effect.damage
local heal = Beans.combat.effect.heal
local none = Beans.combat.effect.none
local exit = Beans.combat.exit
local frame = Beans.combat.frame
local impact = Beans.combat.impact
local machine = Beans.combat.machine
local marker = Beans.combat.marker
local origin = Beans.combat.origin
local phase = Beans.combat.phase
local phasekey = Beans.combat.phasekey
local phasevariant = Beans.combat.phasevariant
local stance = Beans.combat.stance

local function E() return setmetatable({}, getmetatable(cfg._mk.E)) end

mk(1001, "Fireball", origin(0, 0), {machine("Combat", {phase("Idle", "Hit"), phase("Hit", "Idle"), phase("", "NotConfigured")}, {impact(0, 25)}, {frame(9007199254740993, cfg[0].node("combat.Effect.Damage", damage(25)))}, {stance(false, cfg[0].node("combat.Effect.Damage", damage(25))), stance(true, cfg[0].node("combat.Effect.Heal", heal(2)))}, {phasevariant(phasekey("Hit", 0), cfg[0].node("combat.Effect.Damage", damage(30)))}, {combo("", 0, false, cfg[0].node("combat.Effect.Damage", damage(10)))}, {marker("Release")}, anchor(), {anchoruse(), anchoruse()}, exit(cfg[0].node("combat.Effect.None", none())), "Idle", 0, 9007199254740993, false, phasekey("Hit", 0), "", 0, false), machine("Movement", {phase("Idle", "Hit")}, {impact(0, 0)}, {frame(0, cfg[0].node("combat.Effect.None", none()))}, {stance(false, cfg[0].node("combat.Effect.None", none()))}, {phasevariant(phasekey("Idle", 0), cfg[0].node("combat.Effect.None", none()))}, {combo("", 0, false, cfg[0].node("combat.Effect.None", none()))}, E(), anchor(), {anchoruse()}, exit(cfg[0].node("combat.Effect.None", none())), "Idle", 0, 0, false, phasekey("Idle", 0), "", 0, false)}, "Combat", "Idle")

cfg[0].complete('combat.ability')
return this

local cfg = require "cfg._cfgs"
local Beans = require "cfg._beans"

---@class cfg.combat.timeline
---@field id number
---@field events table<Beans.combat.phasekey,Beans.combat.effect>
---@field channels table<string,Beans.combat.effect>
---@field slots table<number,Beans.combat.effect>
---@field clock table<number,Beans.combat.effect>
---@field modes table<boolean,Beans.combat.effect>
---@field eventKey Beans.combat.phasekey
---@field channel string
---@field slot number
---@field frame number
---@field airborne boolean
---@field fallback Beans.combat.effect
---@field get fun(id:number):cfg.combat.timeline
---@field all table<any,cfg.combat.timeline>
---@field RefEventKey Beans.combat.effect
---@field RefChannel Beans.combat.effect
---@field RefSlot Beans.combat.effect
---@field RefFrame Beans.combat.effect
---@field RefAirborne Beans.combat.effect
---@field RefDefault Beans.combat.effect

local this = cfg.combat.timeline

local mk = cfg[0].table('combat.timeline', this, {{'all','get',{'id'}}}, {fields={
    'id', -- int
    'events', -- map<PhaseKey,Effect>
    'channels', -- map<str,Effect>
    'slots', -- map<int,Effect>
    'clock', -- map<long,Effect>
    'modes', -- map<bool,Effect>
    'eventKey', -- PhaseKey
    'channel', -- str
    'slot', -- int
    'frame', -- long
    'airborne', -- bool
    'fallback' -- Effect
    }, uniqkeys={ { 'all', 'get', 1 }, }, enumidx=nil, refs=nil}, nil)

local damage = Beans.combat.effect.damage
local heal = Beans.combat.effect.heal
local none = Beans.combat.effect.none
local phasekey = Beans.combat.phasekey

mk(1, {[phasekey("Hit", 0)] = cfg[0].node("combat.Effect.Damage", damage(25))}, {impact = cfg[0].node("combat.Effect.Heal", heal(5))}, {[0] = cfg[0].node("combat.Effect.Damage", damage(10))}, {[9007199254740993] = cfg[0].node("combat.Effect.Damage", damage(15))}, {[false] = cfg[0].node("combat.Effect.Heal", heal(2)), [true] = cfg[0].node("combat.Effect.None", none())}, phasekey("Hit", 0), "impact", 0, 9007199254740993, false, cfg[0].node("combat.Effect.None", none()))

cfg[0].complete('combat.timeline')
return this

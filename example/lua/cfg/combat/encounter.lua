local cfg = require "cfg._cfgs"
local Beans = require "cfg._beans"

---@class cfg.combat.encounter
---@field id number
---@field command Beans.combat.command
---@field get fun(id:number):cfg.combat.encounter
---@field all table<any,cfg.combat.encounter>

local this = cfg.combat.encounter

local mk = cfg[0].table('combat.encounter', this, {{'all','get',{'id'}}}, {fields={
    'id', -- int
    'command' -- Command
    }, uniqkeys={ { 'all', 'get', 1 }, }, enumidx=nil, refs=nil}, nil)

local jump = Beans.combat.command.jump
local noop = Beans.combat.command.noop
local statemachine = Beans.combat.command.statemachine
local nestedphase = Beans.combat.nestedphase

mk(1, cfg[0].node("combat.Command.StateMachine", statemachine({nestedphase("Done", cfg[0].node("combat.Command.Noop", noop())), nestedphase("Nested", cfg[0].node("combat.Command.StateMachine", statemachine({nestedphase("Else", cfg[0].node("combat.Command.Jump", jump("Done"))), nestedphase("Local", cfg[0].node("combat.Command.Jump", jump("Else")))})))})))

cfg[0].complete('combat.encounter')
return this

local cfg = require "cfg._cfgs"
local Beans = require "cfg._beans"

---@class cfg.combat.branchreward
---@field key Beans.combat.phasekey
---@field reward Beans.combat.reward
---@field get fun(key:Beans.combat.phasekey):cfg.combat.branchreward
---@field all table<any,cfg.combat.branchreward>

local this = cfg.combat.branchreward

local mk = cfg[0].table('combat.branchreward', this, {{'all','get',{'key'}}}, {fields={
    'key', -- PhaseKey
    'reward' -- Reward
    }, uniqkeys={ { 'all', 'get', 1 }, }, enumidx=nil, refs=nil}, nil)

local phasekey = Beans.combat.phasekey
local reward = Beans.combat.reward

mk(phasekey("Hit", 0), reward(5))

cfg[0].complete('combat.branchreward')
return this

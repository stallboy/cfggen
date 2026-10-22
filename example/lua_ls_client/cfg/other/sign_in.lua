local cfg = require "cfg._cfgs"

---@class cfg.other.sign_in
---@field id number , 礼包ID
---@field item2countMap table<number,number> , 普通奖励
---@field vipitem2vipcountMap table<number,number> , vip奖励
---@field viplevel number , 领取vip奖励的最低等级
---@field iconFile string , 礼包图标
---@field get fun(id:number):cfg.other.sign_in
---@field all table<any,cfg.other.sign_in>
---@field RefVipitem2vipcountMap cfg.other.loot

local this = cfg.other.sign_in

local mk = cfg[0].table('other.sign_in', this, {{'all','get',{'id'}}}, {fields={
    'id', -- int, 礼包ID
    'item2countMap', -- map<int,int>, 普通奖励
    'vipitem2vipcountMap', -- map<int,int>, vip奖励
    'viplevel', -- int, 领取vip奖励的最低等级
    'iconFile' -- str, 礼包图标
    }, uniqkeys={ { 'all', 'get', 1 }, }, enumidx=nil, refs={
    { 'RefVipitem2vipcountMap', 3, cfg.other.loot, 'get', 3 }, }}, nil)

local function E() return setmetatable({}, getmetatable(cfg._mk.E)) end

mk(1, {[10001] = 1}, E(), 0, "texture/t_i10005.bundle")
mk(2, {[10014] = 1}, E(), 0, "texture/t_i10006.bundle")
mk(3, {[30001] = 1}, E(), 0, "texture/t_i10007.bundle")
mk(4, {[10001] = 5, [30002] = 5, [30001] = 5}, {[10001] = 10}, 0, "texture/t_i10008.bundle")
mk(5, {[10001] = 1}, E(), 0, "texture/t_i10009.bundle")
mk(6, {[10025] = 1}, E(), 0, "texture/t_i10010.bundle")
mk(7, {[30001] = 1}, E(), 0, "texture/t_i10005.bundle")
mk(8, {[10001] = 5, [30002] = 5, [30001] = 5}, {[10001] = 10}, 0, "texture/t_i10006.bundle")
mk(9, {[10001] = 1}, E(), 0, "texture/t_i10007.bundle")
mk(10, {[10040] = 1}, E(), 0, "texture/t_i10008.bundle")
mk(11, {[30001] = 1}, E(), 0, "texture/t_i10009.bundle")
mk(12, {[10001] = 5, [30002] = 5, [30001] = 5}, {[10001] = 10}, 0, "texture/t_i10010.bundle")

cfg[0].complete('other.sign_in')
return this

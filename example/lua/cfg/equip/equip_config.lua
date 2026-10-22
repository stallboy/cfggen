local cfg = require "cfg._cfgs"

---@class cfg.equip.equip_config
---@field entry string , 入口，程序填
---@field stone_count_for_set number , 形成套装的音石数量
---@field draw_protect_name string , 保底策略名称
---@field broadcastid number , 公告Id
---@field broadcast_least_quality number , 公告的最低品质
---@field week_reward_mailid number , 抽卡周奖励的邮件id
---@field get fun(entry:string):cfg.equip.equip_config
---@field Instance cfg.equip.equip_config
---@field Instance2 cfg.equip.equip_config
---@field all table<any,cfg.equip.equip_config>

local this = cfg.equip.equip_config

local mk = cfg[0].table('equip.equip_config', this, {{'all','get',{'entry'}}}, {fields={
    'entry', -- str, 入口，程序填
    'stone_count_for_set', -- int, 形成套装的音石数量
    'draw_protect_name', -- str, 保底策略名称
    'broadcastid', -- int, 公告Id
    'broadcast_least_quality', -- int, 公告的最低品质
    'week_reward_mailid' -- int, 抽卡周奖励的邮件id
    }, uniqkeys={ { 'all', 'get', 1 }, }, enumidx=1, refs=nil}, nil)

mk("Instance", 2, "测试", 9500, 1003, 100)
mk("Instance2", 3, "aa", 1, 2, 33)

cfg[0].complete('equip.equip_config')
return this

local cfg = {}

cfg._mk = require "common.mkcfg"
local pre = cfg._mk.pretable

cfg.ai = {}
---@type cfg.ai.ai
cfg.ai.ai = pre("cfg.ai.ai")
---@type cfg.ai.ai_action
cfg.ai.ai_action = pre("cfg.ai.ai_action")
---@type cfg.ai.ai_condition
cfg.ai.ai_condition = pre("cfg.ai.ai_condition")
cfg.equip = {}
---@type cfg.equip.ability
cfg.equip.ability = pre("cfg.equip.ability")
---@type cfg.equip.equip_config
cfg.equip.equip_config = pre("cfg.equip.equip_config")
---@type cfg.equip.jewelry
cfg.equip.jewelry = pre("cfg.equip.jewelry")
---@type cfg.equip.jewelry_random
cfg.equip.jewelry_random = pre("cfg.equip.jewelry_random")
---@type cfg.equip.jewelry_suit
cfg.equip.jewelry_suit = pre("cfg.equip.jewelry_suit")
---@type cfg.equip.jewelry_type
cfg.equip.jewelry_type = pre("cfg.equip.jewelry_type")
---@type cfg.equip.rank
cfg.equip.rank = pre("cfg.equip.rank")
cfg.other = {}
---@type cfg.other.argcapturemode
cfg.other.argcapturemode = pre("cfg.other.argcapturemode")
---@type cfg.other.drop
cfg.other.drop = pre("cfg.other.drop")
---@type cfg.other.key_test
cfg.other.key_test = pre("cfg.other.key_test")
---@type cfg.other.loot
cfg.other.loot = pre("cfg.other.loot")
---@type cfg.other.loot_item
cfg.other.loot_item = pre("cfg.other.loot_item")
---@type cfg.other.monster
cfg.other.monster = pre("cfg.other.monster")
---@type cfg.other.sign_in
cfg.other.sign_in = pre("cfg.other.sign_in")
cfg.task = {}
---@type cfg.task.complete_condition_type
cfg.task.complete_condition_type = pre("cfg.task.complete_condition_type")
---@type cfg.task.task
cfg.task.task = pre("cfg.task.task")
---@type cfg.task.task2
cfg.task.task2 = pre("cfg.task.task2")
---@type cfg.task.task_extra_exp
cfg.task.task_extra_exp = pre("cfg.task.task_extra_exp")

return cfg

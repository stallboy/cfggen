print("中文1234")

local mkcfg = require("cfg.mkcfg")
local init = require("cfg.mkcfginit")

mkcfg.tostring = init.tostring
mkcfg.action_tostring = init.action_tostring
mkcfg.newindex = init.newindex
mkcfg.E = init.E
mkcfg.R = init.R

local Beans = require("cfg._beans")
local cfg = require("cfg._cfgs")

print("zh_cn:")
cfg._set_lang("zh_cn") -- 初始设置语言，必须要设置，才能正确显示text文本
local t = cfg.task.task.get(1)
print(t)
print()


print("en:")
cfg._set_lang("en")  -- 切换语言
local t = cfg.task.task.get(1)
print(t)
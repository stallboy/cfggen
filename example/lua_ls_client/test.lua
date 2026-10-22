print("中文1234")

local mkcfg = require("cfg.mkcfg")
local init = require("cfg.mkcfginit")

mkcfg.tostring = init.tostring
mkcfg.action_tostring = init.action_tostring
mkcfg.newindex = init.newindex
mkcfg.E = init.E
mkcfg.R = init.R

local cfg = require("cfg._cfgs")

print("zh_cn:")
cfg._set_lang("zh_cn") -- 初始设置语言，必须要设置，才能正确显示text文本
local t = cfg.task.task.get(1)
print(t)
assert(t.name[1] == "杀个怪")
assert(rawequal(t.NullableRefNexttask, cfg.task.task.get(2)))
assert(rawequal(t.completecondition.RefMonsterid, cfg.other.monster.get(1)))
print()


print("en:")
cfg._set_lang("en")  -- 切换语言
local t = cfg.task.task.get(1)
print(t)
assert(t.name[1] == "kill a monster")
cfg._set_lang("zh_cn")
assert(t.name[1] == "杀个怪", "切换语言后，同一记录立即返回新文本")

local list = cfg.other.key_test.get(1, 2).RefIds
assert(#list == 2 and rawequal(list[2], cfg.other.sign_in.get(12)))
assert(rawequal(cfg.other.sign_in.get(4).RefVipitem2vipcountMap[10001], cfg.other.loot.get(10)))
local loot = cfg.other.loot.get(2)
assert(#loot.ListRefLootid == 7)
for _, row in ipairs(loot.ListRefLootid) do
    assert(row.lootid == 2 and rawequal(row, cfg.other.loot_item.get(row.lootid, row.itemid)))
end

dofile("../combat_assertions.lua")(cfg)
print("ok")

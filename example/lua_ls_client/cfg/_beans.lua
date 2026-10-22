local cfg = require "cfg._cfgs"

local Beans = {}
cfg._beans = Beans

local bean = cfg._mk.bean
local action = cfg._mk.action
local i18n_bean = cfg._mk.i18n_bean
local i18n_action = cfg._mk.i18n_action

---@class Beans.levelrank
---@field level number , 等级
---@field rank number , 品质
---@field RefRank cfg.equip.rank

---@type Beans.levelrank
Beans.levelrank = bean({
    { 'RefRank', 0, cfg.equip.rank, 'get', 2 }, },
    'level', -- int, 等级
    'rank' -- int, 品质
    )
Beans.levelrank = cfg[0].bean('LevelRank', Beans.levelrank)

---@class Beans.position
---@field x number
---@field y number
---@field z number

---@type Beans.position
Beans.position = bean(nil,
    'x', -- int
    'y', -- int
    'z' -- int
    )
Beans.position = cfg[0].bean('Position', Beans.position)

---@class Beans.range
---@field min number , 最小
---@field max number , 最大

---@type Beans.range
Beans.range = bean(nil,
    'min', -- int, 最小
    'max' -- int, 最大
    )
Beans.range = cfg[0].bean('Range', Beans.range)

Beans.ai = {}
---@class Beans.ai.triggertick

---@type Beans.ai.triggertick
Beans.ai.triggertick = {}

---@class Beans.ai.triggertick.constvalue : Beans.ai.triggertick
---@field value number

---@type Beans.ai.triggertick.constvalue
Beans.ai.triggertick.constvalue = action("ConstValue", nil,
    'value' -- int
    )

---@class Beans.ai.triggertick.bylevel : Beans.ai.triggertick
---@field init number
---@field coefficient number

---@type Beans.ai.triggertick.bylevel
Beans.ai.triggertick.bylevel = action("ByLevel", nil,
    'init', -- int
    'coefficient' -- float
    )

---@class Beans.ai.triggertick.byserverupday : Beans.ai.triggertick
---@field init number
---@field coefficient1 number
---@field coefficient2 number

---@type Beans.ai.triggertick.byserverupday
Beans.ai.triggertick.byserverupday = action("ByServerUpDay", nil,
    'init', -- int
    'coefficient1', -- float
    'coefficient2' -- float
    )

Beans.combat = {}
---@class Beans.combat.abilityuse
---@field binding Beans.combat.binding
---@field machines table<number,Beans.combat.machineuse>
---@field RefAbility cfg.combat.ability

---@type Beans.combat.abilityuse
Beans.combat.abilityuse = bean(nil,
    'binding', -- Binding
    'machines' -- list<MachineUse>
    )
Beans.combat.abilityuse = cfg[0].bean('combat.AbilityUse', Beans.combat.abilityuse)

---@class Beans.combat.anchor

---@type Beans.combat.anchor
Beans.combat.anchor = bean(nil)
Beans.combat.anchor = cfg[0].bean('combat.Anchor', Beans.combat.anchor)

---@class Beans.combat.anchoruse
---@field RefAnchor Beans.combat.anchor

---@type Beans.combat.anchoruse
Beans.combat.anchoruse = bean(nil)
Beans.combat.anchoruse = cfg[0].bean('combat.AnchorUse', Beans.combat.anchoruse)

---@class Beans.combat.binding
---@field abilityId number

---@type Beans.combat.binding
Beans.combat.binding = bean(nil,
    'abilityId' -- int
    )
Beans.combat.binding = cfg[0].bean('combat.Binding', Beans.combat.binding)

---@class Beans.combat.bookentry
---@field name string

---@type Beans.combat.bookentry
Beans.combat.bookentry = bean(nil,
    'name' -- str
    )
Beans.combat.bookentry = cfg[0].bean('combat.BookEntry', Beans.combat.bookentry)

---@class Beans.combat.combo
---@field weapon string
---@field tier number
---@field airborne boolean
---@field effect Beans.combat.effect

---@type Beans.combat.combo
Beans.combat.combo = bean(nil,
    'weapon', -- str
    'tier', -- int
    'airborne', -- bool
    'effect' -- Effect
    )
Beans.combat.combo = cfg[0].bean('combat.Combo', Beans.combat.combo)

---@class Beans.combat.command

---@type Beans.combat.command
Beans.combat.command = {}

---@class Beans.combat.command.statemachine : Beans.combat.command
---@field phases table<number,Beans.combat.nestedphase>

---@type Beans.combat.command.statemachine
Beans.combat.command.statemachine = action("StateMachine", nil,
    'phases' -- list<NestedPhase>
    )

---@class Beans.combat.command.jump : Beans.combat.command
---@field key string
---@field NullableRefKey Beans.combat.nestedphase|nil

---@type Beans.combat.command.jump
Beans.combat.command.jump = action("Jump", nil,
    'key' -- str
    )

---@class Beans.combat.command.noop : Beans.combat.command

---@type Beans.combat.command.noop
Beans.combat.command.noop = action("Noop", nil)

---@class Beans.combat.effect

---@type Beans.combat.effect
Beans.combat.effect = {}

---@class Beans.combat.effect.damage : Beans.combat.effect
---@field amount number

---@type Beans.combat.effect.damage
Beans.combat.effect.damage = action("Damage", nil,
    'amount' -- int
    )

---@class Beans.combat.effect.heal : Beans.combat.effect
---@field amount number

---@type Beans.combat.effect.heal
Beans.combat.effect.heal = action("Heal", nil,
    'amount' -- int
    )

---@class Beans.combat.effect.none : Beans.combat.effect

---@type Beans.combat.effect.none
Beans.combat.effect.none = action("None", nil)

---@class Beans.combat.exit
---@field effect Beans.combat.effect

---@type Beans.combat.exit
Beans.combat.exit = bean(nil,
    'effect' -- Effect
    )
Beans.combat.exit = cfg[0].bean('combat.Exit', Beans.combat.exit)

---@class Beans.combat.frame
---@field tick number
---@field effect Beans.combat.effect

---@type Beans.combat.frame
Beans.combat.frame = bean(nil,
    'tick', -- long
    'effect' -- Effect
    )
Beans.combat.frame = cfg[0].bean('combat.Frame', Beans.combat.frame)

---@class Beans.combat.impact
---@field ordinal number
---@field damage number

---@type Beans.combat.impact
Beans.combat.impact = bean(nil,
    'ordinal', -- int
    'damage' -- int
    )
Beans.combat.impact = cfg[0].bean('combat.Impact', Beans.combat.impact)

---@class Beans.combat.machine
---@field code string
---@field phases table<number,Beans.combat.phase>
---@field impacts table<number,Beans.combat.impact>
---@field frames table<number,Beans.combat.frame>
---@field stances table<number,Beans.combat.stance>
---@field variants table<number,Beans.combat.phasevariant>
---@field combos table<number,Beans.combat.combo>
---@field markers table<number,Beans.combat.marker> , 未被引用也必须保持唯一。
---@field anchor Beans.combat.anchor
---@field anchors table<number,Beans.combat.anchoruse>
---@field onExit Beans.combat.exit
---@field initial string
---@field impact number
---@field frame number
---@field stance boolean
---@field variant Beans.combat.phasekey
---@field weapon string
---@field tier number
---@field airborne boolean
---@field RefInitial Beans.combat.phase
---@field RefImpact Beans.combat.impact
---@field RefFrame Beans.combat.frame
---@field RefStance Beans.combat.stance
---@field RefVariant Beans.combat.phasevariant
---@field RefCombo Beans.combat.combo

---@type Beans.combat.machine
Beans.combat.machine = bean(nil,
    'code', -- str
    'phases', -- list<Phase>
    'impacts', -- list<Impact>
    'frames', -- list<Frame>
    'stances', -- list<Stance>
    'variants', -- list<PhaseVariant>
    'combos', -- list<Combo>
    'markers', -- list<Marker>, 未被引用也必须保持唯一。
    'anchor', -- Anchor
    'anchors', -- list<AnchorUse>
    'onExit', -- Exit
    'initial', -- str
    'impact', -- int
    'frame', -- long
    'stance', -- bool
    'variant', -- PhaseKey
    'weapon', -- str
    'tier', -- int
    'airborne' -- bool
    )
Beans.combat.machine = cfg[0].bean('combat.Machine', Beans.combat.machine)

---@class Beans.combat.machineuse
---@field machineKey string
---@field targets table<number,Beans.combat.phaseuse>

---@type Beans.combat.machineuse
Beans.combat.machineuse = bean(nil,
    'machineKey', -- str
    'targets' -- list<PhaseUse>
    )
Beans.combat.machineuse = cfg[0].bean('combat.MachineUse', Beans.combat.machineuse)

---@class Beans.combat.marker
---@field key string

---@type Beans.combat.marker
Beans.combat.marker = bean(nil,
    'key' -- str
    )
Beans.combat.marker = cfg[0].bean('combat.Marker', Beans.combat.marker)

---@class Beans.combat.nestedphase
---@field key string
---@field action Beans.combat.command

---@type Beans.combat.nestedphase
Beans.combat.nestedphase = bean(nil,
    'key', -- str
    'action' -- Command
    )
Beans.combat.nestedphase = cfg[0].bean('combat.NestedPhase', Beans.combat.nestedphase)

---@class Beans.combat.optionalaim
---@field abilityId number
---@field machineKey string
---@field phaseKey string
---@field NullableRefTarget Beans.combat.phase|nil

---@type Beans.combat.optionalaim
Beans.combat.optionalaim = bean(nil,
    'abilityId', -- int
    'machineKey', -- str
    'phaseKey' -- str
    )
Beans.combat.optionalaim = cfg[0].bean('combat.OptionalAim', Beans.combat.optionalaim)

---@class Beans.combat.origin
---@field x number
---@field y number

---@type Beans.combat.origin
Beans.combat.origin = bean(nil,
    'x', -- int
    'y' -- int
    )
Beans.combat.origin = cfg[0].bean('combat.Origin', Beans.combat.origin)

---@class Beans.combat.phase
---@field key string
---@field next string
---@field NullableRefNext Beans.combat.phase|nil
---@field RefExit Beans.combat.effect

---@type Beans.combat.phase
Beans.combat.phase = bean(nil,
    'key', -- str
    'next' -- str
    )
Beans.combat.phase = cfg[0].bean('combat.Phase', Beans.combat.phase)

---@class Beans.combat.phasekey
---@field name string
---@field variant number

---@type Beans.combat.phasekey
Beans.combat.phasekey = bean(nil,
    'name', -- str
    'variant' -- int
    )
Beans.combat.phasekey = cfg[0].bean('combat.PhaseKey', Beans.combat.phasekey)

---@class Beans.combat.phaseuse
---@field phaseKey string
---@field RefTarget Beans.combat.phase
---@field RefByCode Beans.combat.phase
---@field RefOrigin Beans.combat.origin

---@type Beans.combat.phaseuse
Beans.combat.phaseuse = bean(nil,
    'phaseKey' -- str
    )
Beans.combat.phaseuse = cfg[0].bean('combat.PhaseUse', Beans.combat.phaseuse)

---@class Beans.combat.phasevariant
---@field key Beans.combat.phasekey
---@field effect Beans.combat.effect

---@type Beans.combat.phasevariant
Beans.combat.phasevariant = bean(nil,
    'key', -- PhaseKey
    'effect' -- Effect
    )
Beans.combat.phasevariant = cfg[0].bean('combat.PhaseVariant', Beans.combat.phasevariant)

---@class Beans.combat.reward
---@field count number

---@type Beans.combat.reward
Beans.combat.reward = bean(nil,
    'count' -- int
    )
Beans.combat.reward = cfg[0].bean('combat.Reward', Beans.combat.reward)

---@class Beans.combat.stance
---@field airborne boolean
---@field effect Beans.combat.effect

---@type Beans.combat.stance
Beans.combat.stance = bean(nil,
    'airborne', -- bool
    'effect' -- Effect
    )
Beans.combat.stance = cfg[0].bean('combat.Stance', Beans.combat.stance)

Beans.equip = {}
---@class Beans.equip.testpackbean
---@field name string
---@field iRange Beans.range

---@type Beans.equip.testpackbean
Beans.equip.testpackbean = bean(nil,
    'name', -- str
    'iRange' -- Range
    )
Beans.equip.testpackbean = cfg[0].bean('equip.TestPackBean', Beans.equip.testpackbean)

Beans.other = {}
---@class Beans.other.dropitem
---@field chance number , 掉落概率
---@field itemids table<number,number> , 掉落物品
---@field countmin number , 数量下限
---@field countmax number , 数量上限

---@type Beans.other.dropitem
Beans.other.dropitem = bean(nil,
    'chance', -- int, 掉落概率
    'itemids', -- list<int>, 掉落物品
    'countmin', -- int, 数量下限
    'countmax' -- int, 数量上限
    )
Beans.other.dropitem = cfg[0].bean('other.DropItem', Beans.other.dropitem)

Beans.task = {}
---@class Beans.task.testdefaultbean
---@field testInt number
---@field testBool boolean
---@field testString string
---@field testSubBean Beans.position
---@field testList table<number,number>
---@field testList2 table<number,number>
---@field testMap table<number,string>

---@type Beans.task.testdefaultbean
Beans.task.testdefaultbean = bean(nil,
    'testInt', -- int
    'testBool', -- bool
    'testString', -- str
    'testSubBean', -- Position
    'testList', -- list<int>
    'testList2', -- list<int>
    'testMap' -- map<int,str>
    )
Beans.task.testdefaultbean = cfg[0].bean('task.TestDefaultBean', Beans.task.testdefaultbean)

---@class Beans.task.completecondition

---@type Beans.task.completecondition
Beans.task.completecondition = {}

---@class Beans.task.completecondition.killmonster : Beans.task.completecondition
---@field monsterid number
---@field count number
---@field RefMonsterid cfg.other.monster

---@type Beans.task.completecondition.killmonster
Beans.task.completecondition.killmonster = action("KillMonster", {
    { 'RefMonsterid', 0, cfg.other.monster, 'get', 1 }, },
    'monsterid', -- int
    'count' -- int
    )

---@class Beans.task.completecondition.talknpc : Beans.task.completecondition
---@field npcid number

---@type Beans.task.completecondition.talknpc
Beans.task.completecondition.talknpc = action("TalkNpc", nil,
    'npcid' -- int
    )

---@class Beans.task.completecondition.testnocolumn : Beans.task.completecondition

---@type Beans.task.completecondition.testnocolumn
Beans.task.completecondition.testnocolumn = action("TestNoColumn", nil)

---@class Beans.task.completecondition.chat : Beans.task.completecondition
---@field msg string

---@type Beans.task.completecondition.chat
Beans.task.completecondition.chat = action("Chat", nil,
    'msg' -- str
    )

---@class Beans.task.completecondition.conditionand : Beans.task.completecondition
---@field cond1 Beans.task.completecondition
---@field cond2 Beans.task.completecondition

---@type Beans.task.completecondition.conditionand
Beans.task.completecondition.conditionand = action("ConditionAnd", nil,
    'cond1', -- task.completecondition
    'cond2' -- task.completecondition
    )

---@class Beans.task.completecondition.collectitem : Beans.task.completecondition
---@field itemid number
---@field count number

---@type Beans.task.completecondition.collectitem
Beans.task.completecondition.collectitem = action("CollectItem", nil,
    'itemid', -- int
    'count' -- int
    )

---@class Beans.task.completecondition.aa : Beans.task.completecondition

---@type Beans.task.completecondition.aa
Beans.task.completecondition.aa = action("aa", nil)


return Beans

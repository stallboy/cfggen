package config

import "io"
import "encoding/json"
var aiAiMgr *AiAiMgr

func GetAiAiMgr() *AiAiMgr {
    return aiAiMgr
}

var aiAi_actionMgr *AiAi_actionMgr

func GetAiAi_actionMgr() *AiAi_actionMgr {
    return aiAi_actionMgr
}

var aiAi_conditionMgr *AiAi_conditionMgr

func GetAiAi_conditionMgr() *AiAi_conditionMgr {
    return aiAi_conditionMgr
}

var combatAbilityMgr *CombatAbilityMgr

func GetCombatAbilityMgr() *CombatAbilityMgr {
    return combatAbilityMgr
}

var combatBranchrewardMgr *CombatBranchrewardMgr

func GetCombatBranchrewardMgr() *CombatBranchrewardMgr {
    return combatBranchrewardMgr
}

var combatCastMgr *CombatCastMgr

func GetCombatCastMgr() *CombatCastMgr {
    return combatCastMgr
}

var combatEncounterMgr *CombatEncounterMgr

func GetCombatEncounterMgr() *CombatEncounterMgr {
    return combatEncounterMgr
}

var combatPhasebookMgr *CombatPhasebookMgr

func GetCombatPhasebookMgr() *CombatPhasebookMgr {
    return combatPhasebookMgr
}

var combatTimelineMgr *CombatTimelineMgr

func GetCombatTimelineMgr() *CombatTimelineMgr {
    return combatTimelineMgr
}

var equipAbilityMgr *EquipAbilityMgr

func GetEquipAbilityMgr() *EquipAbilityMgr {
    return equipAbilityMgr
}

var equipEquip_configMgr *EquipEquip_configMgr

func GetEquipEquip_configMgr() *EquipEquip_configMgr {
    return equipEquip_configMgr
}

var equipJewelryMgr *EquipJewelryMgr

func GetEquipJewelryMgr() *EquipJewelryMgr {
    return equipJewelryMgr
}

var equipJewelry_randomMgr *EquipJewelry_randomMgr

func GetEquipJewelry_randomMgr() *EquipJewelry_randomMgr {
    return equipJewelry_randomMgr
}

var equipJewelry_suitMgr *EquipJewelry_suitMgr

func GetEquipJewelry_suitMgr() *EquipJewelry_suitMgr {
    return equipJewelry_suitMgr
}

var equipJewelry_typeMgr *EquipJewelry_typeMgr

func GetEquipJewelry_typeMgr() *EquipJewelry_typeMgr {
    return equipJewelry_typeMgr
}

var equipRankMgr *EquipRankMgr

func GetEquipRankMgr() *EquipRankMgr {
    return equipRankMgr
}

var otherArgCaptureModeMgr *OtherArgCaptureModeMgr

func GetOtherArgCaptureModeMgr() *OtherArgCaptureModeMgr {
    return otherArgCaptureModeMgr
}

var otherDropMgr *OtherDropMgr

func GetOtherDropMgr() *OtherDropMgr {
    return otherDropMgr
}

var otherKey_testMgr *OtherKey_testMgr

func GetOtherKey_testMgr() *OtherKey_testMgr {
    return otherKey_testMgr
}

var otherLootMgr *OtherLootMgr

func GetOtherLootMgr() *OtherLootMgr {
    return otherLootMgr
}

var otherLoot_itemMgr *OtherLoot_itemMgr

func GetOtherLoot_itemMgr() *OtherLoot_itemMgr {
    return otherLoot_itemMgr
}

var otherMonsterMgr *OtherMonsterMgr

func GetOtherMonsterMgr() *OtherMonsterMgr {
    return otherMonsterMgr
}

var otherSign_inMgr *OtherSign_inMgr

func GetOtherSign_inMgr() *OtherSign_inMgr {
    return otherSign_inMgr
}

var taskComplete_condition_typeMgr *TaskComplete_condition_typeMgr

func GetTaskComplete_condition_typeMgr() *TaskComplete_condition_typeMgr {
    return taskComplete_condition_typeMgr
}

var taskTaskMgr *TaskTaskMgr

func GetTaskTaskMgr() *TaskTaskMgr {
    return taskTaskMgr
}

var taskTask2Mgr *TaskTask2Mgr

func GetTaskTask2Mgr() *TaskTask2Mgr {
    return taskTask2Mgr
}

var taskTask_extra_expMgr *TaskTask_extra_expMgr

func GetTaskTask_extra_expMgr() *TaskTask_extra_expMgr {
    return taskTask_extra_expMgr
}

func Init(reader io.Reader) *Stream {
    data, err := io.ReadAll(reader)
    if err != nil {
        panic(err)
    }
    return InitPackage(ReadPackage(data))
}

// InitPackage reads a complete versioned package. Mod composition remains a Java/C# API.
func InitPackage(pack *ConfigPackage) *Stream {
    pack.Name()
    if pack.metadata["requires"] != "" {
        panic("this loader supports standalone packages; Mod composition requires Java/C#")
    }
    if _, exists := pack.blocks[".ai.ai"]; !exists {
        panic("missing table in standalone package: ai.ai")
    }
    if _, exists := pack.blocks[".ai.ai_action"]; !exists {
        panic("missing table in standalone package: ai.ai_action")
    }
    if _, exists := pack.blocks[".ai.ai_condition"]; !exists {
        panic("missing table in standalone package: ai.ai_condition")
    }
    if _, exists := pack.blocks[".combat.ability"]; !exists {
        panic("missing table in standalone package: combat.ability")
    }
    if _, exists := pack.blocks[".combat.branchreward"]; !exists {
        panic("missing table in standalone package: combat.branchreward")
    }
    if _, exists := pack.blocks[".combat.cast"]; !exists {
        panic("missing table in standalone package: combat.cast")
    }
    if _, exists := pack.blocks[".combat.encounter"]; !exists {
        panic("missing table in standalone package: combat.encounter")
    }
    if _, exists := pack.blocks[".combat.phasebook"]; !exists {
        panic("missing table in standalone package: combat.phasebook")
    }
    if _, exists := pack.blocks[".combat.timeline"]; !exists {
        panic("missing table in standalone package: combat.timeline")
    }
    if _, exists := pack.blocks[".equip.ability"]; !exists {
        panic("missing table in standalone package: equip.ability")
    }
    if _, exists := pack.blocks[".equip.equip_config"]; !exists {
        panic("missing table in standalone package: equip.equip_config")
    }
    if _, exists := pack.blocks[".equip.jewelry"]; !exists {
        panic("missing table in standalone package: equip.jewelry")
    }
    if _, exists := pack.blocks[".equip.jewelry_random"]; !exists {
        panic("missing table in standalone package: equip.jewelry_random")
    }
    if _, exists := pack.blocks[".equip.jewelry_suit"]; !exists {
        panic("missing table in standalone package: equip.jewelry_suit")
    }
    if _, exists := pack.blocks[".equip.jewelry_type"]; !exists {
        panic("missing table in standalone package: equip.jewelry_type")
    }
    if _, exists := pack.blocks[".equip.rank"]; !exists {
        panic("missing table in standalone package: equip.rank")
    }
    if _, exists := pack.blocks[".other.ArgCaptureMode"]; !exists {
        panic("missing table in standalone package: other.ArgCaptureMode")
    }
    if _, exists := pack.blocks[".other.drop"]; !exists {
        panic("missing table in standalone package: other.drop")
    }
    if _, exists := pack.blocks[".other.key_test"]; !exists {
        panic("missing table in standalone package: other.key_test")
    }
    if _, exists := pack.blocks[".other.loot"]; !exists {
        panic("missing table in standalone package: other.loot")
    }
    if _, exists := pack.blocks[".other.loot_item"]; !exists {
        panic("missing table in standalone package: other.loot_item")
    }
    if _, exists := pack.blocks[".other.monster"]; !exists {
        panic("missing table in standalone package: other.monster")
    }
    if _, exists := pack.blocks[".other.sign_in"]; !exists {
        panic("missing table in standalone package: other.sign_in")
    }
    if _, exists := pack.blocks[".task.complete_condition_type"]; !exists {
        panic("missing table in standalone package: task.complete_condition_type")
    }
    if _, exists := pack.blocks[".task.task"]; !exists {
        panic("missing table in standalone package: task.task")
    }
    if _, exists := pack.blocks[".task.task2"]; !exists {
        panic("missing table in standalone package: task.task2")
    }
    if _, exists := pack.blocks[".task.task_extra_exp"]; !exists {
        panic("missing table in standalone package: task.task_extra_exp")
    }
    return initTables(pack.OpenData())
}

func initTables(stream *Stream) *Stream {
    tables := stream.ReadTables()
    // 4. 处理表数据
    for _, table := range tables {
        switch table.Name {
        case "ai.ai":
            aiAiMgr = &AiAiMgr{}
            aiAiMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "ai.ai_action":
            aiAi_actionMgr = &AiAi_actionMgr{}
            aiAi_actionMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "ai.ai_condition":
            aiAi_conditionMgr = &AiAi_conditionMgr{}
            aiAi_conditionMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "combat.ability":
            combatAbilityMgr = &CombatAbilityMgr{}
            combatAbilityMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "combat.branchreward":
            combatBranchrewardMgr = &CombatBranchrewardMgr{}
            combatBranchrewardMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "combat.cast":
            combatCastMgr = &CombatCastMgr{}
            combatCastMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "combat.encounter":
            combatEncounterMgr = &CombatEncounterMgr{}
            combatEncounterMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "combat.phasebook":
            combatPhasebookMgr = &CombatPhasebookMgr{}
            combatPhasebookMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "combat.timeline":
            combatTimelineMgr = &CombatTimelineMgr{}
            combatTimelineMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "equip.ability":
            equipAbilityMgr = &EquipAbilityMgr{}
            equipAbilityMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "equip.equip_config":
            equipEquip_configMgr = &EquipEquip_configMgr{}
            equipEquip_configMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "equip.jewelry":
            equipJewelryMgr = &EquipJewelryMgr{}
            equipJewelryMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "equip.jewelry_random":
            equipJewelry_randomMgr = &EquipJewelry_randomMgr{}
            equipJewelry_randomMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "equip.jewelry_suit":
            equipJewelry_suitMgr = &EquipJewelry_suitMgr{}
            equipJewelry_suitMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "equip.jewelry_type":
            equipJewelry_typeMgr = &EquipJewelry_typeMgr{}
            equipJewelry_typeMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "equip.rank":
            equipRankMgr = &EquipRankMgr{}
            equipRankMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "other.ArgCaptureMode":
            otherArgCaptureModeMgr = &OtherArgCaptureModeMgr{}
            otherArgCaptureModeMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "other.drop":
            otherDropMgr = &OtherDropMgr{}
            otherDropMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "other.key_test":
            otherKey_testMgr = &OtherKey_testMgr{}
            otherKey_testMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "other.loot":
            otherLootMgr = &OtherLootMgr{}
            otherLootMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "other.loot_item":
            otherLoot_itemMgr = &OtherLoot_itemMgr{}
            otherLoot_itemMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "other.monster":
            otherMonsterMgr = &OtherMonsterMgr{}
            otherMonsterMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "other.sign_in":
            otherSign_inMgr = &OtherSign_inMgr{}
            otherSign_inMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "task.complete_condition_type":
            taskComplete_condition_typeMgr = &TaskComplete_condition_typeMgr{}
            taskComplete_condition_typeMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "task.task":
            taskTaskMgr = &TaskTaskMgr{}
            taskTaskMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "task.task2":
            taskTask2Mgr = &TaskTask2Mgr{}
            taskTask2Mgr.Init(table.Input)
            table.Input.RequireEnd()
        case "task.task_extra_exp":
            taskTask_extra_expMgr = &TaskTask_extra_expMgr{}
            taskTask_extra_expMgr.Init(table.Input)
            table.Input.RequireEnd()
        default:
        }
    }

    stream.bindObjectReferences()
    return stream
}

// All navigation state is temporary and local to this load. Only typed targets remain in records.
func (stream *Stream) bindObjectReferences() {
    type source struct { scope string; fields []int }
    type step struct { field, kind int; keys []int }
    type plan struct { root string; slot int; name string; nullable bool; sources []source; target string; recordKey []int; steps []step }
    type index struct { field int; keys []int }
    type shape struct { name string; fields func(any) []any; indexes []index; plans []plan; bind func(any, []any) }
    type listView struct { rows []any }
    type mapView struct { entries [][2]any; lookup map[string]any }
    type occurrence struct { value, root any; table string; parent *occurrence }
    shapes := map[string]*shape {
        "LevelRank": {
            name: "LevelRank",
            fields: func(value any) []any {
                row := value.(*LevelRank)
                return []any { row.level, row.rank }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "Position": {
            name: "Position",
            fields: func(value any) []any {
                row := value.(*Position)
                return []any { row.x, row.y, row.z }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "Range": {
            name: "Range",
            fields: func(value any) []any {
                row := value.(*Range)
                return []any { row.min, row.max }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "ai.TriggerTick.ByLevel": {
            name: "ai.TriggerTick.ByLevel",
            fields: func(value any) []any {
                row := value.(*AiTriggerTickByLevel)
                return []any { row.init, row.coefficient }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "ai.TriggerTick.ByServerUpDay": {
            name: "ai.TriggerTick.ByServerUpDay",
            fields: func(value any) []any {
                row := value.(*AiTriggerTickByServerUpDay)
                return []any { row.init, row.coefficient1, row.coefficient2 }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "ai.TriggerTick.ConstValue": {
            name: "ai.TriggerTick.ConstValue",
            fields: func(value any) []any {
                row := value.(*AiTriggerTickConstValue)
                return []any { row.value }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "ai.ai": {
            name: "ai.ai",
            fields: func(value any) []any {
                row := value.(*AiAi)
                return []any { row.iD, row.desc, row.condID, row.trigTick, row.trigOdds, func() any { list := &listView{}; for _, item := range row.actionID { list.rows = append(list.rows, item) }; return list }(), row.deathRemove }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "ai.ai_action": {
            name: "ai.ai_action",
            fields: func(value any) []any {
                row := value.(*AiAi_action)
                return []any { row.iD, row.desc, row.formulaID, func() any { list := &listView{}; for _, item := range row.argIList { list.rows = append(list.rows, item) }; return list }(), func() any { list := &listView{}; for _, item := range row.argSList { list.rows = append(list.rows, item) }; return list }() }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "ai.ai_condition": {
            name: "ai.ai_condition",
            fields: func(value any) []any {
                row := value.(*AiAi_condition)
                return []any { row.iD, row.desc, row.formulaID, func() any { list := &listView{}; for _, item := range row.argIList { list.rows = append(list.rows, item) }; return list }(), func() any { list := &listView{}; for _, item := range row.argSList { list.rows = append(list.rows, item) }; return list }() }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "combat.AbilityUse": {
            name: "combat.AbilityUse",
            fields: func(value any) []any {
                row := value.(*CombatAbilityUse)
                return []any { row.binding, func() any { list := &listView{}; for _, item := range row.machines { list.rows = append(list.rows, item) }; return list }() }
            },
            indexes: []index {
            },
            plans: []plan {
                { "combat.cast", 0, "ability", false, []source {
                    { "", []int{0, 0} },
                }, "table:combat.ability", []int{0}, []step {
                } },
            },
            bind: func(value any, refs []any) {
                row := value.(*CombatAbilityUse)
                if refs[0] == nil { row.refAbility = nil } else {
                    row.refAbility = refs[0].(*CombatAbility)
                }
            },
        },
        "combat.Anchor": {
            name: "combat.Anchor",
            fields: func(value any) []any {
                return []any {  }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "combat.AnchorUse": {
            name: "combat.AnchorUse",
            fields: func(value any) []any {
                return []any {  }
            },
            indexes: []index {
            },
            plans: []plan {
                { "combat.ability", 0, "anchor", false, []source {
                }, "ancestor:combat.Machine", []int{}, []step {
                    { 8, 0, []int{} },
                } },
            },
            bind: func(value any, refs []any) {
                row := value.(*CombatAnchorUse)
                if refs[0] == nil { row.refAnchor = nil } else {
                    row.refAnchor = refs[0].(*CombatAnchor)
                }
            },
        },
        "combat.Binding": {
            name: "combat.Binding",
            fields: func(value any) []any {
                row := value.(*CombatBinding)
                return []any { row.abilityId }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "combat.BookEntry": {
            name: "combat.BookEntry",
            fields: func(value any) []any {
                row := value.(*CombatBookEntry)
                return []any { row.name }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "combat.Combo": {
            name: "combat.Combo",
            fields: func(value any) []any {
                row := value.(*CombatCombo)
                return []any { row.weapon, row.tier, row.airborne, row.effect }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "combat.Command.Jump": {
            name: "combat.Command.Jump",
            fields: func(value any) []any {
                row := value.(*CombatCommandJump)
                return []any { row.key }
            },
            indexes: []index {
            },
            plans: []plan {
                { "combat.encounter", 0, "key", true, []source {
                    { "", []int{0} },
                }, "ancestor:combat.Command.StateMachine", []int{}, []step {
                    { 0, 1, []int{0} },
                } },
            },
            bind: func(value any, refs []any) {
                row := value.(*CombatCommandJump)
                if refs[0] == nil { row.nullableRefKey = nil } else {
                    row.nullableRefKey = refs[0].(*CombatNestedPhase)
                }
            },
        },
        "combat.Command.Noop": {
            name: "combat.Command.Noop",
            fields: func(value any) []any {
                return []any {  }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "combat.Command.StateMachine": {
            name: "combat.Command.StateMachine",
            fields: func(value any) []any {
                row := value.(*CombatCommandStateMachine)
                return []any { func() any { list := &listView{}; for _, item := range row.phases { list.rows = append(list.rows, item) }; return list }() }
            },
            indexes: []index {
                { 0, []int{0} },
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "combat.Effect.Damage": {
            name: "combat.Effect.Damage",
            fields: func(value any) []any {
                row := value.(*CombatEffectDamage)
                return []any { row.amount }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "combat.Effect.Heal": {
            name: "combat.Effect.Heal",
            fields: func(value any) []any {
                row := value.(*CombatEffectHeal)
                return []any { row.amount }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "combat.Effect.None": {
            name: "combat.Effect.None",
            fields: func(value any) []any {
                return []any {  }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "combat.Exit": {
            name: "combat.Exit",
            fields: func(value any) []any {
                row := value.(*CombatExit)
                return []any { row.effect }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "combat.Frame": {
            name: "combat.Frame",
            fields: func(value any) []any {
                row := value.(*CombatFrame)
                return []any { row.tick, row.effect }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "combat.Impact": {
            name: "combat.Impact",
            fields: func(value any) []any {
                row := value.(*CombatImpact)
                return []any { row.ordinal, row.damage }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "combat.Machine": {
            name: "combat.Machine",
            fields: func(value any) []any {
                row := value.(*CombatMachine)
                return []any { row.code, func() any { list := &listView{}; for _, item := range row.phases { list.rows = append(list.rows, item) }; return list }(), func() any { list := &listView{}; for _, item := range row.impacts { list.rows = append(list.rows, item) }; return list }(), func() any { list := &listView{}; for _, item := range row.frames { list.rows = append(list.rows, item) }; return list }(), func() any { list := &listView{}; for _, item := range row.stances { list.rows = append(list.rows, item) }; return list }(), func() any { list := &listView{}; for _, item := range row.variants { list.rows = append(list.rows, item) }; return list }(), func() any { list := &listView{}; for _, item := range row.combos { list.rows = append(list.rows, item) }; return list }(), func() any { list := &listView{}; for _, item := range row.markers { list.rows = append(list.rows, item) }; return list }(), row.anchor, func() any { list := &listView{}; for _, item := range row.anchors { list.rows = append(list.rows, item) }; return list }(), row.onExit, row.initial, row.impact, row.frame, row.stance, row.variant, row.weapon, row.tier, row.airborne }
            },
            indexes: []index {
                { 1, []int{0} },
                { 2, []int{0} },
                { 3, []int{0} },
                { 4, []int{0} },
                { 5, []int{0} },
                { 6, []int{0, 1, 2} },
                { 7, []int{0} },
            },
            plans: []plan {
                { "combat.ability", 0, "initial", false, []source {
                    { "", []int{11} },
                }, "ancestor:combat.Machine", []int{}, []step {
                    { 1, 1, []int{0} },
                } },
                { "combat.ability", 1, "impact", false, []source {
                    { "", []int{12} },
                }, "ancestor:combat.Machine", []int{}, []step {
                    { 2, 1, []int{0} },
                } },
                { "combat.ability", 2, "frame", false, []source {
                    { "", []int{13} },
                }, "ancestor:combat.Machine", []int{}, []step {
                    { 3, 1, []int{0} },
                } },
                { "combat.ability", 3, "stance", false, []source {
                    { "", []int{14} },
                }, "ancestor:combat.Machine", []int{}, []step {
                    { 4, 1, []int{0} },
                } },
                { "combat.ability", 4, "variant", false, []source {
                    { "", []int{15} },
                }, "ancestor:combat.Machine", []int{}, []step {
                    { 5, 1, []int{0} },
                } },
                { "combat.ability", 5, "combo", false, []source {
                    { "", []int{16} },
                    { "", []int{17} },
                    { "", []int{18} },
                }, "ancestor:combat.Machine", []int{}, []step {
                    { 6, 1, []int{0, 1, 2} },
                } },
            },
            bind: func(value any, refs []any) {
                row := value.(*CombatMachine)
                if refs[0] == nil { row.refInitial = nil } else {
                    row.refInitial = refs[0].(*CombatPhase)
                }
                if refs[1] == nil { row.refImpact = nil } else {
                    row.refImpact = refs[1].(*CombatImpact)
                }
                if refs[2] == nil { row.refFrame = nil } else {
                    row.refFrame = refs[2].(*CombatFrame)
                }
                if refs[3] == nil { row.refStance = nil } else {
                    row.refStance = refs[3].(*CombatStance)
                }
                if refs[4] == nil { row.refVariant = nil } else {
                    row.refVariant = refs[4].(*CombatPhaseVariant)
                }
                if refs[5] == nil { row.refCombo = nil } else {
                    row.refCombo = refs[5].(*CombatCombo)
                }
            },
        },
        "combat.MachineUse": {
            name: "combat.MachineUse",
            fields: func(value any) []any {
                row := value.(*CombatMachineUse)
                return []any { row.machineKey, func() any { list := &listView{}; for _, item := range row.targets { list.rows = append(list.rows, item) }; return list }() }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "combat.Marker": {
            name: "combat.Marker",
            fields: func(value any) []any {
                row := value.(*CombatMarker)
                return []any { row.key }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "combat.NestedPhase": {
            name: "combat.NestedPhase",
            fields: func(value any) []any {
                row := value.(*CombatNestedPhase)
                return []any { row.key, row.action }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "combat.OptionalAim": {
            name: "combat.OptionalAim",
            fields: func(value any) []any {
                row := value.(*CombatOptionalAim)
                return []any { row.abilityId, row.machineKey, row.phaseKey }
            },
            indexes: []index {
            },
            plans: []plan {
                { "combat.cast", 0, "target", true, []source {
                    { "", []int{0} },
                    { "", []int{1} },
                    { "", []int{2} },
                }, "table:combat.ability", []int{0}, []step {
                    { 3, 1, []int{0} },
                    { 1, 1, []int{0} },
                } },
            },
            bind: func(value any, refs []any) {
                row := value.(*CombatOptionalAim)
                if refs[0] == nil { row.nullableRefTarget = nil } else {
                    row.nullableRefTarget = refs[0].(*CombatPhase)
                }
            },
        },
        "combat.Origin": {
            name: "combat.Origin",
            fields: func(value any) []any {
                row := value.(*CombatOrigin)
                return []any { row.x, row.y }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "combat.Phase": {
            name: "combat.Phase",
            fields: func(value any) []any {
                row := value.(*CombatPhase)
                return []any { row.key, row.next }
            },
            indexes: []index {
            },
            plans: []plan {
                { "combat.ability", 0, "next", true, []source {
                    { "", []int{1} },
                }, "ancestor:combat.Machine", []int{}, []step {
                    { 1, 1, []int{0} },
                } },
                { "combat.ability", 1, "exit", false, []source {
                }, "ancestor:combat.Machine", []int{}, []step {
                    { 10, 0, []int{} },
                    { 0, 0, []int{} },
                } },
            },
            bind: func(value any, refs []any) {
                row := value.(*CombatPhase)
                if refs[0] == nil { row.nullableRefNext = nil } else {
                    row.nullableRefNext = refs[0].(*CombatPhase)
                }
                if refs[1] == nil { row.refExit = nil } else {
                    row.refExit = refs[1].(CombatEffect)
                }
            },
        },
        "combat.PhaseKey": {
            name: "combat.PhaseKey",
            fields: func(value any) []any {
                row := value.(*CombatPhaseKey)
                return []any { row.name, row.variant }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "combat.PhaseUse": {
            name: "combat.PhaseUse",
            fields: func(value any) []any {
                row := value.(*CombatPhaseUse)
                return []any { row.phaseKey }
            },
            indexes: []index {
            },
            plans: []plan {
                { "combat.cast", 0, "target", false, []source {
                    { "ancestor:combat.AbilityUse", []int{0, 0} },
                    { "ancestor:combat.MachineUse", []int{0} },
                    { "", []int{0} },
                }, "table:combat.ability", []int{0}, []step {
                    { 3, 1, []int{0} },
                    { 1, 1, []int{0} },
                } },
                { "combat.cast", 1, "byCode", false, []source {
                    { "root", []int{1} },
                    { "ancestor:combat.MachineUse", []int{0} },
                    { "", []int{0} },
                }, "table:combat.ability", []int{1}, []step {
                    { 3, 1, []int{0} },
                    { 1, 1, []int{0} },
                } },
                { "combat.cast", 2, "origin", false, []source {
                }, "root", []int{}, []step {
                    { 2, 0, []int{} },
                } },
            },
            bind: func(value any, refs []any) {
                row := value.(*CombatPhaseUse)
                if refs[0] == nil { row.refTarget = nil } else {
                    row.refTarget = refs[0].(*CombatPhase)
                }
                if refs[1] == nil { row.refByCode = nil } else {
                    row.refByCode = refs[1].(*CombatPhase)
                }
                if refs[2] == nil { row.refOrigin = nil } else {
                    row.refOrigin = refs[2].(*CombatOrigin)
                }
            },
        },
        "combat.PhaseVariant": {
            name: "combat.PhaseVariant",
            fields: func(value any) []any {
                row := value.(*CombatPhaseVariant)
                return []any { row.key, row.effect }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "combat.Reward": {
            name: "combat.Reward",
            fields: func(value any) []any {
                row := value.(*CombatReward)
                return []any { row.count }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "combat.Stance": {
            name: "combat.Stance",
            fields: func(value any) []any {
                row := value.(*CombatStance)
                return []any { row.airborne, row.effect }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "combat.ability": {
            name: "combat.ability",
            fields: func(value any) []any {
                row := value.(*CombatAbility)
                return []any { row.id, row.code, row.origin, func() any { list := &listView{}; for _, item := range row.machines { list.rows = append(list.rows, item) }; return list }(), row.startMachine, row.startPhase }
            },
            indexes: []index {
                { 3, []int{0} },
            },
            plans: []plan {
                { "combat.ability", 0, "initial", false, []source {
                    { "", []int{4} },
                    { "", []int{5} },
                }, "root", []int{}, []step {
                    { 3, 1, []int{0} },
                    { 1, 1, []int{0} },
                } },
                { "combat.ability", 1, "spawn", false, []source {
                }, "root", []int{}, []step {
                    { 2, 0, []int{} },
                } },
            },
            bind: func(value any, refs []any) {
                row := value.(*CombatAbility)
                if refs[0] == nil { row.refInitial = nil } else {
                    row.refInitial = refs[0].(*CombatPhase)
                }
                if refs[1] == nil { row.refSpawn = nil } else {
                    row.refSpawn = refs[1].(*CombatOrigin)
                }
            },
        },
        "combat.branchreward": {
            name: "combat.branchreward",
            fields: func(value any) []any {
                row := value.(*CombatBranchreward)
                return []any { row.key, row.reward }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "combat.cast": {
            name: "combat.cast",
            fields: func(value any) []any {
                row := value.(*CombatCast)
                return []any { row.id, row.abilityCode, row.origin, func() any { list := &listView{}; for _, item := range row.uses { list.rows = append(list.rows, item) }; return list }(), func() any { list := &listView{}; for _, item := range row.optional { list.rows = append(list.rows, item) }; return list }(), row.school, row.tier, row.airborne, row.bookCode, row.edition, row.rewardKey }
            },
            indexes: []index {
            },
            plans: []plan {
                { "combat.cast", 0, "book", false, []source {
                    { "", []int{5} },
                    { "", []int{6} },
                    { "", []int{7} },
                }, "table:combat.phasebook", []int{0, 1, 2}, []step {
                    { 5, 0, []int{} },
                } },
                { "combat.cast", 1, "bookByCode", false, []source {
                    { "", []int{8} },
                    { "", []int{9} },
                }, "table:combat.phasebook", []int{3, 4}, []step {
                    { 5, 0, []int{} },
                } },
                { "combat.cast", 2, "bookRecord", false, []source {
                    { "root", []int{5} },
                    { "root", []int{6} },
                    { "root", []int{7} },
                }, "table:combat.phasebook", []int{0, 1, 2}, []step {
                } },
                { "combat.cast", 3, "reward", false, []source {
                    { "", []int{10} },
                }, "table:combat.branchreward", []int{0}, []step {
                    { 1, 0, []int{} },
                } },
            },
            bind: func(value any, refs []any) {
                row := value.(*CombatCast)
                if refs[0] == nil { row.refBook = nil } else {
                    row.refBook = refs[0].(*CombatBookEntry)
                }
                if refs[1] == nil { row.refBookByCode = nil } else {
                    row.refBookByCode = refs[1].(*CombatBookEntry)
                }
                if refs[2] == nil { row.refBookRecord = nil } else {
                    row.refBookRecord = refs[2].(*CombatPhasebook)
                }
                if refs[3] == nil { row.refReward = nil } else {
                    row.refReward = refs[3].(*CombatReward)
                }
            },
        },
        "combat.encounter": {
            name: "combat.encounter",
            fields: func(value any) []any {
                row := value.(*CombatEncounter)
                return []any { row.id, row.command }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "combat.phasebook": {
            name: "combat.phasebook",
            fields: func(value any) []any {
                row := value.(*CombatPhasebook)
                return []any { row.school, row.tier, row.airborne, row.code, row.edition, row.entry }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "combat.timeline": {
            name: "combat.timeline",
            fields: func(value any) []any {
                row := value.(*CombatTimeline)
                return []any { row.id, func() any { view := &mapView{}; for key, item := range row.events { view.entries = append(view.entries, [2]any{key, item}) }; return view }(), func() any { view := &mapView{}; for key, item := range row.channels { view.entries = append(view.entries, [2]any{key, item}) }; return view }(), func() any { view := &mapView{}; for key, item := range row.slots { view.entries = append(view.entries, [2]any{key, item}) }; return view }(), func() any { view := &mapView{}; for key, item := range row.clock { view.entries = append(view.entries, [2]any{key, item}) }; return view }(), func() any { view := &mapView{}; for key, item := range row.modes { view.entries = append(view.entries, [2]any{key, item}) }; return view }(), row.eventKey, row.channel, row.slot, row.frame, row.airborne, row.fallback }
            },
            indexes: []index {
            },
            plans: []plan {
                { "combat.timeline", 0, "eventKey", false, []source {
                    { "", []int{6} },
                }, "root", []int{}, []step {
                    { 1, 2, []int{} },
                } },
                { "combat.timeline", 1, "channel", false, []source {
                    { "", []int{7} },
                }, "root", []int{}, []step {
                    { 2, 2, []int{} },
                } },
                { "combat.timeline", 2, "slot", false, []source {
                    { "", []int{8} },
                }, "root", []int{}, []step {
                    { 3, 2, []int{} },
                } },
                { "combat.timeline", 3, "frame", false, []source {
                    { "", []int{9} },
                }, "root", []int{}, []step {
                    { 4, 2, []int{} },
                } },
                { "combat.timeline", 4, "airborne", false, []source {
                    { "", []int{10} },
                }, "root", []int{}, []step {
                    { 5, 2, []int{} },
                } },
                { "combat.timeline", 5, "default", false, []source {
                }, "root", []int{}, []step {
                    { 11, 0, []int{} },
                } },
            },
            bind: func(value any, refs []any) {
                row := value.(*CombatTimeline)
                if refs[0] == nil { row.refEventKey = nil } else {
                    row.refEventKey = refs[0].(CombatEffect)
                }
                if refs[1] == nil { row.refChannel = nil } else {
                    row.refChannel = refs[1].(CombatEffect)
                }
                if refs[2] == nil { row.refSlot = nil } else {
                    row.refSlot = refs[2].(CombatEffect)
                }
                if refs[3] == nil { row.refFrame = nil } else {
                    row.refFrame = refs[3].(CombatEffect)
                }
                if refs[4] == nil { row.refAirborne = nil } else {
                    row.refAirborne = refs[4].(CombatEffect)
                }
                if refs[5] == nil { row.refDefault = nil } else {
                    row.refDefault = refs[5].(CombatEffect)
                }
            },
        },
        "equip.TestPackBean": {
            name: "equip.TestPackBean",
            fields: func(value any) []any {
                row := value.(*EquipTestPackBean)
                return []any { row.name, row.iRange }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "equip.ability": {
            name: "equip.ability",
            fields: func(value any) []any {
                row := value.(*EquipAbility)
                return []any { row.id, row.name }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "equip.equip_config": {
            name: "equip.equip_config",
            fields: func(value any) []any {
                row := value.(*EquipEquip_config)
                return []any { row.entry, row.stone_count_for_set, row.draw_protect_name, row.broadcastid, row.broadcast_least_quality, row.week_reward_mailid }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "equip.jewelry": {
            name: "equip.jewelry",
            fields: func(value any) []any {
                row := value.(*EquipJewelry)
                return []any { row.iD, row.name, row.iconFile, row.lvlRank, row.jType, row.suitID, row.keyAbility, row.keyAbilityValue, row.salePrice, row.description }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "equip.jewelry_random": {
            name: "equip.jewelry_random",
            fields: func(value any) []any {
                row := value.(*EquipJewelry_random)
                return []any { row.lvlRank, row.attackRange, func() any { list := &listView{}; for _, item := range row.otherRange { list.rows = append(list.rows, item) }; return list }(), func() any { list := &listView{}; for _, item := range row.testPack { list.rows = append(list.rows, item) }; return list }() }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "equip.jewelry_suit": {
            name: "equip.jewelry_suit",
            fields: func(value any) []any {
                row := value.(*EquipJewelry_suit)
                return []any { row.suitID, row.ename, row.name, row.ability1, row.ability1Value, row.ability2, row.ability2Value, row.ability3, row.ability3Value, func() any { list := &listView{}; for _, item := range row.suitList { list.rows = append(list.rows, item) }; return list }() }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "equip.jewelry_type": {
            name: "equip.jewelry_type",
            fields: func(value any) []any {
                row := value.(*EquipJewelry_type)
                return []any { row.typeName }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "equip.rank": {
            name: "equip.rank",
            fields: func(value any) []any {
                row := value.(*EquipRank)
                return []any { row.rankID, row.rankName, row.rankShowName }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "other.ArgCaptureMode": {
            name: "other.ArgCaptureMode",
            fields: func(value any) []any {
                row := value.(*OtherArgCaptureMode)
                return []any { row.name, row.id, row.comment }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "other.DropItem": {
            name: "other.DropItem",
            fields: func(value any) []any {
                row := value.(*OtherDropItem)
                return []any { row.chance, func() any { list := &listView{}; for _, item := range row.itemids { list.rows = append(list.rows, item) }; return list }(), row.countmin, row.countmax }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "other.drop": {
            name: "other.drop",
            fields: func(value any) []any {
                row := value.(*OtherDrop)
                return []any { row.dropid, row.name, func() any { list := &listView{}; for _, item := range row.items { list.rows = append(list.rows, item) }; return list }(), func() any { view := &mapView{}; for key, item := range row.testmap { view.entries = append(view.entries, [2]any{key, item}) }; return view }() }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "other.key_test": {
            name: "other.key_test",
            fields: func(value any) []any {
                row := value.(*OtherKey_test)
                return []any { row.id1, row.id2, row.id3, func() any { list := &listView{}; for _, item := range row.ids { list.rows = append(list.rows, item) }; return list }(), row.enumTest, func() any { list := &listView{}; for _, item := range row.enumList { list.rows = append(list.rows, item) }; return list }() }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "other.loot": {
            name: "other.loot",
            fields: func(value any) []any {
                row := value.(*OtherLoot)
                return []any { row.lootid, row.ename, row.name, func() any { list := &listView{}; for _, item := range row.chanceList { list.rows = append(list.rows, item) }; return list }() }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "other.loot_item": {
            name: "other.loot_item",
            fields: func(value any) []any {
                row := value.(*OtherLoot_item)
                return []any { row.lootid, row.itemid, row.chance, row.countmin, row.countmax }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "other.monster": {
            name: "other.monster",
            fields: func(value any) []any {
                row := value.(*OtherMonster)
                return []any { row.id, func() any { list := &listView{}; for _, item := range row.posList { list.rows = append(list.rows, item) }; return list }(), row.lootId, row.lootItemId, func() any { view := &mapView{}; for key, item := range row.enumMap1 { view.entries = append(view.entries, [2]any{key, item}) }; return view }(), func() any { view := &mapView{}; for key, item := range row.enumMap2 { view.entries = append(view.entries, [2]any{key, item}) }; return view }() }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "other.sign_in": {
            name: "other.sign_in",
            fields: func(value any) []any {
                row := value.(*OtherSign_in)
                return []any { row.id, func() any { view := &mapView{}; for key, item := range row.item2countMap { view.entries = append(view.entries, [2]any{key, item}) }; return view }(), func() any { view := &mapView{}; for key, item := range row.vipitem2vipcountMap { view.entries = append(view.entries, [2]any{key, item}) }; return view }(), row.viplevel, row.iconFile }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "task.TestDefaultBean": {
            name: "task.TestDefaultBean",
            fields: func(value any) []any {
                row := value.(*TaskTestDefaultBean)
                return []any { row.testInt, row.testBool, row.testString, row.testSubBean, func() any { list := &listView{}; for _, item := range row.testList { list.rows = append(list.rows, item) }; return list }(), func() any { list := &listView{}; for _, item := range row.testList2 { list.rows = append(list.rows, item) }; return list }(), func() any { view := &mapView{}; for key, item := range row.testMap { view.entries = append(view.entries, [2]any{key, item}) }; return view }() }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "task.complete_condition_type": {
            name: "task.complete_condition_type",
            fields: func(value any) []any {
                row := value.(*TaskComplete_condition_type)
                return []any { row.id, row.name }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "task.completecondition.Chat": {
            name: "task.completecondition.Chat",
            fields: func(value any) []any {
                row := value.(*TaskCompleteconditionChat)
                return []any { row.msg }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "task.completecondition.CollectItem": {
            name: "task.completecondition.CollectItem",
            fields: func(value any) []any {
                row := value.(*TaskCompleteconditionCollectItem)
                return []any { row.itemid, row.count }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "task.completecondition.ConditionAnd": {
            name: "task.completecondition.ConditionAnd",
            fields: func(value any) []any {
                row := value.(*TaskCompleteconditionConditionAnd)
                return []any { row.cond1, row.cond2 }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "task.completecondition.KillMonster": {
            name: "task.completecondition.KillMonster",
            fields: func(value any) []any {
                row := value.(*TaskCompleteconditionKillMonster)
                return []any { row.monsterid, row.count }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "task.completecondition.TalkNpc": {
            name: "task.completecondition.TalkNpc",
            fields: func(value any) []any {
                row := value.(*TaskCompleteconditionTalkNpc)
                return []any { row.npcid }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "task.completecondition.TestNoColumn": {
            name: "task.completecondition.TestNoColumn",
            fields: func(value any) []any {
                return []any {  }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "task.completecondition.aa": {
            name: "task.completecondition.aa",
            fields: func(value any) []any {
                return []any {  }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "task.task": {
            name: "task.task",
            fields: func(value any) []any {
                row := value.(*TaskTask)
                return []any { row.taskid, func() any { list := &listView{}; for _, item := range row.name { list.rows = append(list.rows, item) }; return list }(), row.nexttask, row.completecondition, row.exp, row.testDefaultBean }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "task.task2": {
            name: "task.task2",
            fields: func(value any) []any {
                row := value.(*TaskTask2)
                return []any { row.taskid, func() any { list := &listView{}; for _, item := range row.name { list.rows = append(list.rows, item) }; return list }(), row.nexttask, row.completecondition, row.exp, row.testBool, row.testString, row.testStruct, func() any { list := &listView{}; for _, item := range row.testList { list.rows = append(list.rows, item) }; return list }(), func() any { list := &listView{}; for _, item := range row.testListStruct { list.rows = append(list.rows, item) }; return list }(), func() any { list := &listView{}; for _, item := range row.testListInterface { list.rows = append(list.rows, item) }; return list }() }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
        "task.task_extra_exp": {
            name: "task.task_extra_exp",
            fields: func(value any) []any {
                row := value.(*TaskTask_extra_exp)
                return []any { row.taskid, row.extraexp, row.test1, row.test2, row.fielda, row.fieldb, row.fieldc, row.fieldd }
            },
            indexes: []index {
            },
            plans: []plan {
            },
            bind: func(value any, refs []any) {
            },
        },
    }
    shapeOf := func(value any) *shape {
        switch value.(type) {
            case *LevelRank: return shapes["LevelRank"]
            case *Position: return shapes["Position"]
            case *Range: return shapes["Range"]
            case *AiTriggerTickByLevel: return shapes["ai.TriggerTick.ByLevel"]
            case *AiTriggerTickByServerUpDay: return shapes["ai.TriggerTick.ByServerUpDay"]
            case *AiTriggerTickConstValue: return shapes["ai.TriggerTick.ConstValue"]
            case *AiAi: return shapes["ai.ai"]
            case *AiAi_action: return shapes["ai.ai_action"]
            case *AiAi_condition: return shapes["ai.ai_condition"]
            case *CombatAbilityUse: return shapes["combat.AbilityUse"]
            case *CombatAnchor: return shapes["combat.Anchor"]
            case *CombatAnchorUse: return shapes["combat.AnchorUse"]
            case *CombatBinding: return shapes["combat.Binding"]
            case *CombatBookEntry: return shapes["combat.BookEntry"]
            case *CombatCombo: return shapes["combat.Combo"]
            case *CombatCommandJump: return shapes["combat.Command.Jump"]
            case *CombatCommandNoop: return shapes["combat.Command.Noop"]
            case *CombatCommandStateMachine: return shapes["combat.Command.StateMachine"]
            case *CombatEffectDamage: return shapes["combat.Effect.Damage"]
            case *CombatEffectHeal: return shapes["combat.Effect.Heal"]
            case *CombatEffectNone: return shapes["combat.Effect.None"]
            case *CombatExit: return shapes["combat.Exit"]
            case *CombatFrame: return shapes["combat.Frame"]
            case *CombatImpact: return shapes["combat.Impact"]
            case *CombatMachine: return shapes["combat.Machine"]
            case *CombatMachineUse: return shapes["combat.MachineUse"]
            case *CombatMarker: return shapes["combat.Marker"]
            case *CombatNestedPhase: return shapes["combat.NestedPhase"]
            case *CombatOptionalAim: return shapes["combat.OptionalAim"]
            case *CombatOrigin: return shapes["combat.Origin"]
            case *CombatPhase: return shapes["combat.Phase"]
            case *CombatPhaseKey: return shapes["combat.PhaseKey"]
            case *CombatPhaseUse: return shapes["combat.PhaseUse"]
            case *CombatPhaseVariant: return shapes["combat.PhaseVariant"]
            case *CombatReward: return shapes["combat.Reward"]
            case *CombatStance: return shapes["combat.Stance"]
            case *CombatAbility: return shapes["combat.ability"]
            case *CombatBranchreward: return shapes["combat.branchreward"]
            case *CombatCast: return shapes["combat.cast"]
            case *CombatEncounter: return shapes["combat.encounter"]
            case *CombatPhasebook: return shapes["combat.phasebook"]
            case *CombatTimeline: return shapes["combat.timeline"]
            case *EquipTestPackBean: return shapes["equip.TestPackBean"]
            case *EquipAbility: return shapes["equip.ability"]
            case *EquipEquip_config: return shapes["equip.equip_config"]
            case *EquipJewelry: return shapes["equip.jewelry"]
            case *EquipJewelry_random: return shapes["equip.jewelry_random"]
            case *EquipJewelry_suit: return shapes["equip.jewelry_suit"]
            case *EquipJewelry_type: return shapes["equip.jewelry_type"]
            case *EquipRank: return shapes["equip.rank"]
            case *OtherArgCaptureMode: return shapes["other.ArgCaptureMode"]
            case *OtherDropItem: return shapes["other.DropItem"]
            case *OtherDrop: return shapes["other.drop"]
            case *OtherKey_test: return shapes["other.key_test"]
            case *OtherLoot: return shapes["other.loot"]
            case *OtherLoot_item: return shapes["other.loot_item"]
            case *OtherMonster: return shapes["other.monster"]
            case *OtherSign_in: return shapes["other.sign_in"]
            case *TaskTestDefaultBean: return shapes["task.TestDefaultBean"]
            case *TaskComplete_condition_type: return shapes["task.complete_condition_type"]
            case *TaskCompleteconditionChat: return shapes["task.completecondition.Chat"]
            case *TaskCompleteconditionCollectItem: return shapes["task.completecondition.CollectItem"]
            case *TaskCompleteconditionConditionAnd: return shapes["task.completecondition.ConditionAnd"]
            case *TaskCompleteconditionKillMonster: return shapes["task.completecondition.KillMonster"]
            case *TaskCompleteconditionTalkNpc: return shapes["task.completecondition.TalkNpc"]
            case *TaskCompleteconditionTestNoColumn: return shapes["task.completecondition.TestNoColumn"]
            case *TaskCompleteconditionAa: return shapes["task.completecondition.aa"]
            case *TaskTask: return shapes["task.task"]
            case *TaskTask2: return shapes["task.task2"]
            case *TaskTask_extra_exp: return shapes["task.task_extra_exp"]
            default: return nil
        }
    }
    fields := map[any][]any{}
    read := func(value any) []any {
        if result, ok := fields[value]; ok { return result }
        result := shapeOf(value).fields(value); fields[value] = result; return result
    }
    keyOf := func(parts []any) string {
        values := make([]any, len(parts))
        for i, part := range parts { if shapeOf(part) != nil { part = read(part) }; values[i] = part }
        text, err := json.Marshal(values); if err != nil { panic(err) }; return string(text)
    }
    indexes := map[*listView]map[string]map[string]any{}
    lookup := func(list *listView, keys []int) map[string]any {
        definitions := indexes[list]; if definitions == nil { definitions = map[string]map[string]any{}; indexes[list] = definitions }
        definitionBytes, _ := json.Marshal(keys); definition := string(definitionBytes)
        if result, ok := definitions[definition]; ok { return result }
        result := map[string]any{}; definitions[definition] = result
        for _, row := range list.rows {
            members := read(row); parts := make([]any, len(keys)); for i, field := range keys { parts[i] = members[field] }
            key := keyOf(parts); if _, exists := result[key]; exists { panic("duplicate local index key: " + key) }; result[key] = row
        }
        return result
    }
    mapLookup := func(view *mapView) map[string]any {
        if view.lookup == nil {
            view.lookup = map[string]any{}
            for _, pair := range view.entries {
                key := keyOf([]any{pair[0]}); if _, exists := view.lookup[key]; exists { panic("duplicate map key: " + key) }; view.lookup[key] = pair[1]
            }
        }
        return view.lookup
    }
    tables := map[string]*listView{}
    {
        list := &listView{}
        for _, row := range GetAiAiMgr().GetAll() { list.rows = append(list.rows, row) }
        tables["ai.ai"] = list
    }
    {
        list := &listView{}
        for _, row := range GetAiAi_actionMgr().GetAll() { list.rows = append(list.rows, row) }
        tables["ai.ai_action"] = list
    }
    {
        list := &listView{}
        for _, row := range GetAiAi_conditionMgr().GetAll() { list.rows = append(list.rows, row) }
        tables["ai.ai_condition"] = list
    }
    {
        list := &listView{}
        for _, row := range GetCombatAbilityMgr().GetAll() { list.rows = append(list.rows, row) }
        tables["combat.ability"] = list
    }
    {
        list := &listView{}
        for _, row := range GetCombatBranchrewardMgr().GetAll() { list.rows = append(list.rows, row) }
        tables["combat.branchreward"] = list
    }
    {
        list := &listView{}
        for _, row := range GetCombatCastMgr().GetAll() { list.rows = append(list.rows, row) }
        tables["combat.cast"] = list
    }
    {
        list := &listView{}
        for _, row := range GetCombatEncounterMgr().GetAll() { list.rows = append(list.rows, row) }
        tables["combat.encounter"] = list
    }
    {
        list := &listView{}
        for _, row := range GetCombatPhasebookMgr().GetAll() { list.rows = append(list.rows, row) }
        tables["combat.phasebook"] = list
    }
    {
        list := &listView{}
        for _, row := range GetCombatTimelineMgr().GetAll() { list.rows = append(list.rows, row) }
        tables["combat.timeline"] = list
    }
    {
        list := &listView{}
        for _, row := range GetEquipAbilityMgr().GetAll() { list.rows = append(list.rows, row) }
        tables["equip.ability"] = list
    }
    {
        list := &listView{}
        for _, row := range GetEquipEquip_configMgr().GetAll() { list.rows = append(list.rows, row) }
        tables["equip.equip_config"] = list
    }
    {
        list := &listView{}
        for _, row := range GetEquipJewelryMgr().GetAll() { list.rows = append(list.rows, row) }
        tables["equip.jewelry"] = list
    }
    {
        list := &listView{}
        for _, row := range GetEquipJewelry_randomMgr().GetAll() { list.rows = append(list.rows, row) }
        tables["equip.jewelry_random"] = list
    }
    {
        list := &listView{}
        for _, row := range GetEquipJewelry_suitMgr().GetAll() { list.rows = append(list.rows, row) }
        tables["equip.jewelry_suit"] = list
    }
    {
        list := &listView{}
        for _, row := range GetEquipJewelry_typeMgr().GetAll() { list.rows = append(list.rows, row) }
        tables["equip.jewelry_type"] = list
    }
    {
        list := &listView{}
        for _, row := range GetEquipRankMgr().GetAll() { list.rows = append(list.rows, row) }
        tables["equip.rank"] = list
    }
    {
        list := &listView{}
        for _, row := range GetOtherArgCaptureModeMgr().GetAll() { list.rows = append(list.rows, row) }
        tables["other.ArgCaptureMode"] = list
    }
    {
        list := &listView{}
        for _, row := range GetOtherDropMgr().GetAll() { list.rows = append(list.rows, row) }
        tables["other.drop"] = list
    }
    {
        list := &listView{}
        for _, row := range GetOtherKey_testMgr().GetAll() { list.rows = append(list.rows, row) }
        tables["other.key_test"] = list
    }
    {
        list := &listView{}
        for _, row := range GetOtherLootMgr().GetAll() { list.rows = append(list.rows, row) }
        tables["other.loot"] = list
    }
    {
        list := &listView{}
        for _, row := range GetOtherLoot_itemMgr().GetAll() { list.rows = append(list.rows, row) }
        tables["other.loot_item"] = list
    }
    {
        list := &listView{}
        for _, row := range GetOtherMonsterMgr().GetAll() { list.rows = append(list.rows, row) }
        tables["other.monster"] = list
    }
    {
        list := &listView{}
        for _, row := range GetOtherSign_inMgr().GetAll() { list.rows = append(list.rows, row) }
        tables["other.sign_in"] = list
    }
    {
        list := &listView{}
        for _, row := range GetTaskComplete_condition_typeMgr().GetAll() { list.rows = append(list.rows, row) }
        tables["task.complete_condition_type"] = list
    }
    {
        list := &listView{}
        for _, row := range GetTaskTaskMgr().GetAll() { list.rows = append(list.rows, row) }
        tables["task.task"] = list
    }
    {
        list := &listView{}
        for _, row := range GetTaskTask2Mgr().GetAll() { list.rows = append(list.rows, row) }
        tables["task.task2"] = list
    }
    {
        list := &listView{}
        for _, row := range GetTaskTask_extra_expMgr().GetAll() { list.rows = append(list.rows, row) }
        tables["task.task_extra_exp"] = list
    }
    occurrences := []*occurrence{}
    var walk func(any, any, string, *occurrence)
    walk = func(value, root any, table string, parent *occurrence) {
        if shape := shapeOf(value); shape != nil {
            at := &occurrence{value, root, table, parent}
            if len(shape.plans) != 0 { occurrences = append(occurrences, at) }
            members := read(value)
            for _, index := range shape.indexes { lookup(members[index.field].(*listView), index.keys) }
            for _, member := range members { walk(member, root, table, at) }
        } else {
            switch container := value.(type) {
            case *listView: for _, item := range container.rows { walk(item, root, table, parent) }
            case *mapView:
                // Scalar keys are unique in Go maps; struct pointers still need comparison by value.
                if len(container.entries) != 0 && shapeOf(container.entries[0][0]) != nil { mapLookup(container) }
                for _, pair := range container.entries { walk(pair[0], root, table, parent); walk(pair[1], root, table, parent) }
            }
        }
    }
    for table, list := range tables { for _, row := range list.rows { walk(row, row, table, nil) } }
    scope := func(name string, at *occurrence) any {
        if name == "" { return at.value }; if name == "root" { return at.root }
        for current := at; current != nil; current = current.parent { if "ancestor:" + shapeOf(current.value).name == name { return current.value } }
        return nil
    }
    for _, at := range occurrences {
        shape := shapeOf(at.value); slots := 0
        for _, plan := range shape.plans { if plan.slot >= slots { slots = plan.slot+1 } }
        refs := make([]any, slots)
        for _, plan := range shape.plans {
            if plan.root != at.table { continue }
            keys := make([]any, len(plan.sources)); valid := true
            for i, source := range plan.sources {
                value := scope(source.scope, at)
                if value == nil { valid = false; continue }
                for _, field := range source.fields { value = read(value)[field] }
                keys[i] = value
            }
            external := len(plan.target) >= 6 && plan.target[:6] == "table:"
            var target any
            if external { target = tables[plan.target[6:]] } else { target = scope(plan.target, at) }
            if !valid || target == nil { panic("missing reference scope: " + shape.name + "." + plan.name) }
            used := 0
            if external { target = lookup(target.(*listView), plan.recordKey)[keyOf(keys[:len(plan.recordKey)])]; used = len(plan.recordKey) }
            for _, step := range plan.steps {
                if target == nil { break }
                member := read(target)[step.field]
                switch step.kind {
                case 0: target = member
                case 1: target = lookup(member.(*listView), step.keys)[keyOf(keys[used:used+len(step.keys)])]
                case 2: target = mapLookup(member.(*mapView))[keyOf(keys[used:used+1])]
                }
                if step.kind == 2 { used++ } else { used += len(step.keys) }
            }
            if target == nil && !plan.nullable { panic("missing object reference: " + shape.name + "." + plan.name + " " + keyOf(keys)) }
            refs[plan.slot] = target
        }
        shape.bind(at.value, refs)
    }
}





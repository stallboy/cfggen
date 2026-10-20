package config

import "io"

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

    return stream
}

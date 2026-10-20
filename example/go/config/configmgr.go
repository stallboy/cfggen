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

var equipEquipconfigMgr *EquipEquipconfigMgr

func GetEquipEquipconfigMgr() *EquipEquipconfigMgr {
    return equipEquipconfigMgr
}

var equipJewelryMgr *EquipJewelryMgr

func GetEquipJewelryMgr() *EquipJewelryMgr {
    return equipJewelryMgr
}

var equipJewelryrandomMgr *EquipJewelryrandomMgr

func GetEquipJewelryrandomMgr() *EquipJewelryrandomMgr {
    return equipJewelryrandomMgr
}

var equipJewelrysuitMgr *EquipJewelrysuitMgr

func GetEquipJewelrysuitMgr() *EquipJewelrysuitMgr {
    return equipJewelrysuitMgr
}

var equipJewelrytypeMgr *EquipJewelrytypeMgr

func GetEquipJewelrytypeMgr() *EquipJewelrytypeMgr {
    return equipJewelrytypeMgr
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

var otherKeytestMgr *OtherKeytestMgr

func GetOtherKeytestMgr() *OtherKeytestMgr {
    return otherKeytestMgr
}

var otherLootMgr *OtherLootMgr

func GetOtherLootMgr() *OtherLootMgr {
    return otherLootMgr
}

var otherLootitemMgr *OtherLootitemMgr

func GetOtherLootitemMgr() *OtherLootitemMgr {
    return otherLootitemMgr
}

var otherMonsterMgr *OtherMonsterMgr

func GetOtherMonsterMgr() *OtherMonsterMgr {
    return otherMonsterMgr
}

var otherSigninMgr *OtherSigninMgr

func GetOtherSigninMgr() *OtherSigninMgr {
    return otherSigninMgr
}

var taskCompleteconditiontypeMgr *TaskCompleteconditiontypeMgr

func GetTaskCompleteconditiontypeMgr() *TaskCompleteconditiontypeMgr {
    return taskCompleteconditiontypeMgr
}

var taskTaskMgr *TaskTaskMgr

func GetTaskTaskMgr() *TaskTaskMgr {
    return taskTaskMgr
}

var taskTask2Mgr *TaskTask2Mgr

func GetTaskTask2Mgr() *TaskTask2Mgr {
    return taskTask2Mgr
}

var taskTaskextraexpMgr *TaskTaskextraexpMgr

func GetTaskTaskextraexpMgr() *TaskTaskextraexpMgr {
    return taskTaskextraexpMgr
}

func Init(reader io.Reader) *Stream {
    data, err := io.ReadAll(reader)
    if err != nil { panic(err) }
    return InitPackage(ReadPackage(data))
}

// InitPackage reads a complete versioned package. Mod composition remains a Java/C# API.
func InitPackage(pack *ConfigPackage) *Stream {
    pack.Name()
    if pack.metadata["requires"] != "" { panic("this loader supports standalone packages; Mod composition requires Java/C#") }
    if _, exists := pack.blocks[".ai.ai"]; !exists { panic("missing table in standalone package: ai.ai") }
    if _, exists := pack.blocks[".ai.ai_action"]; !exists { panic("missing table in standalone package: ai.ai_action") }
    if _, exists := pack.blocks[".ai.ai_condition"]; !exists { panic("missing table in standalone package: ai.ai_condition") }
    if _, exists := pack.blocks[".equip.ability"]; !exists { panic("missing table in standalone package: equip.ability") }
    if _, exists := pack.blocks[".equip.equipconfig"]; !exists { panic("missing table in standalone package: equip.equipconfig") }
    if _, exists := pack.blocks[".equip.jewelry"]; !exists { panic("missing table in standalone package: equip.jewelry") }
    if _, exists := pack.blocks[".equip.jewelryrandom"]; !exists { panic("missing table in standalone package: equip.jewelryrandom") }
    if _, exists := pack.blocks[".equip.jewelrysuit"]; !exists { panic("missing table in standalone package: equip.jewelrysuit") }
    if _, exists := pack.blocks[".equip.jewelrytype"]; !exists { panic("missing table in standalone package: equip.jewelrytype") }
    if _, exists := pack.blocks[".equip.rank"]; !exists { panic("missing table in standalone package: equip.rank") }
    if _, exists := pack.blocks[".other.ArgCaptureMode"]; !exists { panic("missing table in standalone package: other.ArgCaptureMode") }
    if _, exists := pack.blocks[".other.drop"]; !exists { panic("missing table in standalone package: other.drop") }
    if _, exists := pack.blocks[".other.keytest"]; !exists { panic("missing table in standalone package: other.keytest") }
    if _, exists := pack.blocks[".other.loot"]; !exists { panic("missing table in standalone package: other.loot") }
    if _, exists := pack.blocks[".other.lootitem"]; !exists { panic("missing table in standalone package: other.lootitem") }
    if _, exists := pack.blocks[".other.monster"]; !exists { panic("missing table in standalone package: other.monster") }
    if _, exists := pack.blocks[".other.signin"]; !exists { panic("missing table in standalone package: other.signin") }
    if _, exists := pack.blocks[".task.completeconditiontype"]; !exists { panic("missing table in standalone package: task.completeconditiontype") }
    if _, exists := pack.blocks[".task.task"]; !exists { panic("missing table in standalone package: task.task") }
    if _, exists := pack.blocks[".task.task2"]; !exists { panic("missing table in standalone package: task.task2") }
    if _, exists := pack.blocks[".task.taskextraexp"]; !exists { panic("missing table in standalone package: task.taskextraexp") }
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
        case "equip.equipconfig":
            equipEquipconfigMgr = &EquipEquipconfigMgr{}
            equipEquipconfigMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "equip.jewelry":
            equipJewelryMgr = &EquipJewelryMgr{}
            equipJewelryMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "equip.jewelryrandom":
            equipJewelryrandomMgr = &EquipJewelryrandomMgr{}
            equipJewelryrandomMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "equip.jewelrysuit":
            equipJewelrysuitMgr = &EquipJewelrysuitMgr{}
            equipJewelrysuitMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "equip.jewelrytype":
            equipJewelrytypeMgr = &EquipJewelrytypeMgr{}
            equipJewelrytypeMgr.Init(table.Input)
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
        case "other.keytest":
            otherKeytestMgr = &OtherKeytestMgr{}
            otherKeytestMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "other.loot":
            otherLootMgr = &OtherLootMgr{}
            otherLootMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "other.lootitem":
            otherLootitemMgr = &OtherLootitemMgr{}
            otherLootitemMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "other.monster":
            otherMonsterMgr = &OtherMonsterMgr{}
            otherMonsterMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "other.signin":
            otherSigninMgr = &OtherSigninMgr{}
            otherSigninMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "task.completeconditiontype":
            taskCompleteconditiontypeMgr = &TaskCompleteconditiontypeMgr{}
            taskCompleteconditiontypeMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "task.task":
            taskTaskMgr = &TaskTaskMgr{}
            taskTaskMgr.Init(table.Input)
            table.Input.RequireEnd()
        case "task.task2":
            taskTask2Mgr = &TaskTask2Mgr{}
            taskTask2Mgr.Init(table.Input)
            table.Input.RequireEnd()
        case "task.taskextraexp":
            taskTaskextraexpMgr = &TaskTaskextraexpMgr{}
            taskTaskextraexpMgr.Init(table.Input)
            table.Input.RequireEnd()
        default:
        }
    }

    return stream
}

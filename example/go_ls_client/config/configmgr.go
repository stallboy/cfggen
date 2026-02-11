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
    stream := &Stream{reader: reader}

    // 1. 跳过 Schema（如果有）
    schemaLength := stream.ReadInt32()
    if schemaLength > 0 {
        stream.SkipBytes(int(schemaLength))
    }

    // 2. 读取 StringPool
    stream.ReadStringPool()

    // 3. 读取 LangTextPool
    stream.ReadLangTextPool()

    // 4. 处理表数据
    tableCount := stream.ReadSize()
    for i := 0; i < tableCount; i++ {
        tableName := stream.ReadString()
        tableSize := stream.ReadSize()
        switch tableName {
        case "ai.ai":
            aiAiMgr = &AiAiMgr{}
            aiAiMgr.Init(stream)
        case "ai.ai_action":
            aiAi_actionMgr = &AiAi_actionMgr{}
            aiAi_actionMgr.Init(stream)
        case "ai.ai_condition":
            aiAi_conditionMgr = &AiAi_conditionMgr{}
            aiAi_conditionMgr.Init(stream)
        case "equip.ability":
            equipAbilityMgr = &EquipAbilityMgr{}
            equipAbilityMgr.Init(stream)
        case "equip.equipconfig":
            equipEquipconfigMgr = &EquipEquipconfigMgr{}
            equipEquipconfigMgr.Init(stream)
        case "equip.jewelry":
            equipJewelryMgr = &EquipJewelryMgr{}
            equipJewelryMgr.Init(stream)
        case "equip.jewelryrandom":
            equipJewelryrandomMgr = &EquipJewelryrandomMgr{}
            equipJewelryrandomMgr.Init(stream)
        case "equip.jewelrysuit":
            equipJewelrysuitMgr = &EquipJewelrysuitMgr{}
            equipJewelrysuitMgr.Init(stream)
        case "equip.jewelrytype":
            equipJewelrytypeMgr = &EquipJewelrytypeMgr{}
            equipJewelrytypeMgr.Init(stream)
        case "equip.rank":
            equipRankMgr = &EquipRankMgr{}
            equipRankMgr.Init(stream)
        case "other.drop":
            otherDropMgr = &OtherDropMgr{}
            otherDropMgr.Init(stream)
        case "other.keytest":
            otherKeytestMgr = &OtherKeytestMgr{}
            otherKeytestMgr.Init(stream)
        case "other.loot":
            otherLootMgr = &OtherLootMgr{}
            otherLootMgr.Init(stream)
        case "other.lootitem":
            otherLootitemMgr = &OtherLootitemMgr{}
            otherLootitemMgr.Init(stream)
        case "other.monster":
            otherMonsterMgr = &OtherMonsterMgr{}
            otherMonsterMgr.Init(stream)
        case "other.signin":
            otherSigninMgr = &OtherSigninMgr{}
            otherSigninMgr.Init(stream)
        case "task.completeconditiontype":
            taskCompleteconditiontypeMgr = &TaskCompleteconditiontypeMgr{}
            taskCompleteconditiontypeMgr.Init(stream)
        case "task.task":
            taskTaskMgr = &TaskTaskMgr{}
            taskTaskMgr.Init(stream)
        case "task.task2":
            taskTask2Mgr = &TaskTask2Mgr{}
            taskTask2Mgr.Init(stream)
        case "task.taskextraexp":
            taskTaskextraexpMgr = &TaskTaskextraexpMgr{}
            taskTaskextraexpMgr.Init(stream)
        default:
            stream.SkipBytes(tableSize)
        }
    }

    return stream
}

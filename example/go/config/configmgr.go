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

func Init(reader io.Reader) {
    myStream := &Stream{reader: reader}
    for {
        cfgName := myStream.ReadString()
        if cfgName == "" {
            break
        }
        switch cfgName {
        case "ai.ai":
            aiAiMgr = &AiAiMgr{}
            aiAiMgr.Init(myStream)
        case "ai.ai_action":
            aiAi_actionMgr = &AiAi_actionMgr{}
            aiAi_actionMgr.Init(myStream)
        case "ai.ai_condition":
            aiAi_conditionMgr = &AiAi_conditionMgr{}
            aiAi_conditionMgr.Init(myStream)
        case "equip.ability":
            equipAbilityMgr = &EquipAbilityMgr{}
            equipAbilityMgr.Init(myStream)
        case "equip.equipconfig":
            equipEquipconfigMgr = &EquipEquipconfigMgr{}
            equipEquipconfigMgr.Init(myStream)
        case "equip.jewelry":
            equipJewelryMgr = &EquipJewelryMgr{}
            equipJewelryMgr.Init(myStream)
        case "equip.jewelryrandom":
            equipJewelryrandomMgr = &EquipJewelryrandomMgr{}
            equipJewelryrandomMgr.Init(myStream)
        case "equip.jewelrysuit":
            equipJewelrysuitMgr = &EquipJewelrysuitMgr{}
            equipJewelrysuitMgr.Init(myStream)
        case "equip.jewelrytype":
            equipJewelrytypeMgr = &EquipJewelrytypeMgr{}
            equipJewelrytypeMgr.Init(myStream)
        case "equip.rank":
            equipRankMgr = &EquipRankMgr{}
            equipRankMgr.Init(myStream)
        case "other.drop":
            otherDropMgr = &OtherDropMgr{}
            otherDropMgr.Init(myStream)
        case "other.keytest":
            otherKeytestMgr = &OtherKeytestMgr{}
            otherKeytestMgr.Init(myStream)
        case "other.loot":
            otherLootMgr = &OtherLootMgr{}
            otherLootMgr.Init(myStream)
        case "other.lootitem":
            otherLootitemMgr = &OtherLootitemMgr{}
            otherLootitemMgr.Init(myStream)
        case "other.monster":
            otherMonsterMgr = &OtherMonsterMgr{}
            otherMonsterMgr.Init(myStream)
        case "other.signin":
            otherSigninMgr = &OtherSigninMgr{}
            otherSigninMgr.Init(myStream)
        case "task.completeconditiontype":
            taskCompleteconditiontypeMgr = &TaskCompleteconditiontypeMgr{}
            taskCompleteconditiontypeMgr.Init(myStream)
        case "task.task":
            taskTaskMgr = &TaskTaskMgr{}
            taskTaskMgr.Init(myStream)
        case "task.task2":
            taskTask2Mgr = &TaskTask2Mgr{}
            taskTask2Mgr.Init(myStream)
        case "task.taskextraexp":
            taskTaskextraexpMgr = &TaskTaskextraexpMgr{}
            taskTaskextraexpMgr.Init(myStream)
        }
    }
}

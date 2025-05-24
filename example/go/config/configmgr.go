package config

import "io"

type ConfigMgr struct {
    AiAiMgr *AiAiMgr
    AiAi_actionMgr *AiAi_actionMgr
    AiAi_conditionMgr *AiAi_conditionMgr
    EquipAbilityMgr *EquipAbilityMgr
    EquipEquipconfigMgr *EquipEquipconfigMgr
    EquipJewelryMgr *EquipJewelryMgr
    EquipJewelryrandomMgr *EquipJewelryrandomMgr
    EquipJewelrysuitMgr *EquipJewelrysuitMgr
    EquipJewelrytypeMgr *EquipJewelrytypeMgr
    EquipRankMgr *EquipRankMgr
    OtherDropMgr *OtherDropMgr
    OtherKeytestMgr *OtherKeytestMgr
    OtherLootMgr *OtherLootMgr
    OtherLootitemMgr *OtherLootitemMgr
    OtherMonsterMgr *OtherMonsterMgr
    OtherSigninMgr *OtherSigninMgr
    TaskCompleteconditiontypeMgr *TaskCompleteconditiontypeMgr
    TaskTaskMgr *TaskTaskMgr
    TaskTask2Mgr *TaskTask2Mgr
    TaskTaskextraexpMgr *TaskTaskextraexpMgr
}

func (t *ConfigMgr) Init(reader io.Reader) {
    myStream := &Stream{reader: reader}
    for {
        cfgName := myStream.ReadString()
        switch cfgName {
        case "ai.ai":
                t.AiAiMgr = &AiAiMgr{}
                t.AiAiMgr.Init(myStream)
        case "ai.ai_action":
                t.AiAi_actionMgr = &AiAi_actionMgr{}
                t.AiAi_actionMgr.Init(myStream)
        case "ai.ai_condition":
                t.AiAi_conditionMgr = &AiAi_conditionMgr{}
                t.AiAi_conditionMgr.Init(myStream)
        case "equip.ability":
                t.EquipAbilityMgr = &EquipAbilityMgr{}
                t.EquipAbilityMgr.Init(myStream)
        case "equip.equipconfig":
                t.EquipEquipconfigMgr = &EquipEquipconfigMgr{}
                t.EquipEquipconfigMgr.Init(myStream)
        case "equip.jewelry":
                t.EquipJewelryMgr = &EquipJewelryMgr{}
                t.EquipJewelryMgr.Init(myStream)
        case "equip.jewelryrandom":
                t.EquipJewelryrandomMgr = &EquipJewelryrandomMgr{}
                t.EquipJewelryrandomMgr.Init(myStream)
        case "equip.jewelrysuit":
                t.EquipJewelrysuitMgr = &EquipJewelrysuitMgr{}
                t.EquipJewelrysuitMgr.Init(myStream)
        case "equip.jewelrytype":
                t.EquipJewelrytypeMgr = &EquipJewelrytypeMgr{}
                t.EquipJewelrytypeMgr.Init(myStream)
        case "equip.rank":
                t.EquipRankMgr = &EquipRankMgr{}
                t.EquipRankMgr.Init(myStream)
        case "other.drop":
                t.OtherDropMgr = &OtherDropMgr{}
                t.OtherDropMgr.Init(myStream)
        case "other.keytest":
                t.OtherKeytestMgr = &OtherKeytestMgr{}
                t.OtherKeytestMgr.Init(myStream)
        case "other.loot":
                t.OtherLootMgr = &OtherLootMgr{}
                t.OtherLootMgr.Init(myStream)
        case "other.lootitem":
                t.OtherLootitemMgr = &OtherLootitemMgr{}
                t.OtherLootitemMgr.Init(myStream)
        case "other.monster":
                t.OtherMonsterMgr = &OtherMonsterMgr{}
                t.OtherMonsterMgr.Init(myStream)
        case "other.signin":
                t.OtherSigninMgr = &OtherSigninMgr{}
                t.OtherSigninMgr.Init(myStream)
        case "task.completeconditiontype":
                t.TaskCompleteconditiontypeMgr = &TaskCompleteconditiontypeMgr{}
                t.TaskCompleteconditiontypeMgr.Init(myStream)
        case "task.task":
                t.TaskTaskMgr = &TaskTaskMgr{}
                t.TaskTaskMgr.Init(myStream)
        case "task.task2":
                t.TaskTask2Mgr = &TaskTask2Mgr{}
                t.TaskTask2Mgr.Init(myStream)
        case "task.taskextraexp":
                t.TaskTaskextraexpMgr = &TaskTaskextraexpMgr{}
                t.TaskTaskextraexpMgr.Init(myStream)

    }
    }
}


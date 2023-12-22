using System.Collections.Generic;

namespace Config
{
    public static class CSVProcessor
    {
        public static readonly LoadErrors Errors = new LoadErrors();

        public static void Process(Config.Stream os)
        {
            var configNulls = new List<string>
            {
                "ai.ai",
                "ai.ai_action",
                "ai.ai_condition",
                "ai.triggerticktype",
                "equip.ability",
                "equip.equipconfig",
                "equip.jewelry",
                "equip.jewelryrandom",
                "equip.jewelrysuit",
                "equip.jewelrytype",
                "equip.rank",
                "other.drop",
                "other.loot",
                "other.lootitem",
                "other.monster",
                "other.signin",
                "task.completeconditiontype",
                "task.task",
                "task.task2",
                "task.taskextraexp",
            };
            for(;;)
            {
                var csv = os.ReadCfg();
                if (csv == null)
                    break;
                switch(csv)
                {
                    case "ai.ai":
                        configNulls.Remove(csv);
                        Config.Ai.DataAi.Initialize(os, Errors);
                        break;
                    case "ai.ai_action":
                        configNulls.Remove(csv);
                        Config.Ai.DataAi_action.Initialize(os, Errors);
                        break;
                    case "ai.ai_condition":
                        configNulls.Remove(csv);
                        Config.Ai.DataAi_condition.Initialize(os, Errors);
                        break;
                    case "ai.triggerticktype":
                        configNulls.Remove(csv);
                        Config.Ai.DataTriggerticktype.Initialize(os, Errors);
                        break;
                    case "equip.ability":
                        configNulls.Remove(csv);
                        Config.Equip.DataAbility.Initialize(os, Errors);
                        break;
                    case "equip.equipconfig":
                        configNulls.Remove(csv);
                        Config.Equip.DataEquipconfig.Initialize(os, Errors);
                        break;
                    case "equip.jewelry":
                        configNulls.Remove(csv);
                        Config.Equip.DataJewelry.Initialize(os, Errors);
                        break;
                    case "equip.jewelryrandom":
                        configNulls.Remove(csv);
                        Config.Equip.DataJewelryrandom.Initialize(os, Errors);
                        break;
                    case "equip.jewelrysuit":
                        configNulls.Remove(csv);
                        Config.Equip.DataJewelrysuit.Initialize(os, Errors);
                        break;
                    case "equip.jewelrytype":
                        configNulls.Remove(csv);
                        Config.Equip.DataJewelrytype.Initialize(os, Errors);
                        break;
                    case "equip.rank":
                        configNulls.Remove(csv);
                        Config.Equip.DataRank.Initialize(os, Errors);
                        break;
                    case "other.drop":
                        configNulls.Remove(csv);
                        Config.Other.DataDrop.Initialize(os, Errors);
                        break;
                    case "other.loot":
                        configNulls.Remove(csv);
                        Config.Other.DataLoot.Initialize(os, Errors);
                        break;
                    case "other.lootitem":
                        configNulls.Remove(csv);
                        Config.Other.DataLootitem.Initialize(os, Errors);
                        break;
                    case "other.monster":
                        configNulls.Remove(csv);
                        Config.Other.DataMonster.Initialize(os, Errors);
                        break;
                    case "other.signin":
                        configNulls.Remove(csv);
                        Config.Other.DataSignin.Initialize(os, Errors);
                        break;
                    case "task.completeconditiontype":
                        configNulls.Remove(csv);
                        Config.Task.DataCompleteconditiontype.Initialize(os, Errors);
                        break;
                    case "task.task":
                        configNulls.Remove(csv);
                        Config.Task.DataTask.Initialize(os, Errors);
                        break;
                    case "task.task2":
                        configNulls.Remove(csv);
                        Config.Task.DataTask2.Initialize(os, Errors);
                        break;
                    case "task.taskextraexp":
                        configNulls.Remove(csv);
                        Config.Task.DataTaskextraexp.Initialize(os, Errors);
                        break;
                    default:
                        Errors.ConfigDataAdd(csv);
                        break;
                }
            }
            foreach (var csv in configNulls)
                Errors.ConfigNull(csv);
            Config.Equip.DataJewelry.Resolve(Errors);
            Config.Equip.DataJewelryrandom.Resolve(Errors);
            Config.Other.DataLoot.Resolve(Errors);
            Config.Other.DataSignin.Resolve(Errors);
            Config.Task.DataTask.Resolve(Errors);
            Config.Task.DataTask2.Resolve(Errors);
        }

    }
}


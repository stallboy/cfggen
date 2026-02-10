using System.Collections.Generic;

namespace Config
{
    public static class Processor
    {
        public static readonly LoadErrors Errors = new LoadErrors();

        // 从 bytes 文件加载（新格式）
        public static void Process(Config.Stream os)
        {
            var configNulls = new List<string>
            {
                "ai.ai",
                "ai.ai_action",
                "ai.ai_condition",
                "equip.ability",
                "equip.equipconfig",
                "equip.jewelry",
                "equip.jewelryrandom",
                "equip.jewelrysuit",
                "equip.jewelrytype",
                "equip.rank",
                "other.drop",
                "other.keytest",
                "other.loot",
                "other.lootitem",
                "other.monster",
                "other.signin",
                "task.completeconditiontype",
                "task.task",
                "task.task2",
                "task.taskextraexp",
            };

            // 读取表数量
            int tableCount = os.ReadInt32();

            for (int i = 0; i < tableCount; i++)
            {
                // 读取表名
                string tableName = os.ReadString();
                // 读取表大小
                int tableSize = os.ReadInt32();

                // 根据表名分发到对应的 Initialize 方法
                switch(tableName)
                {
                    case "ai.ai":
                        configNulls.Remove(tableName);
                        Config.Ai.DataAi.Initialize(os, Errors);
                        break;
                    case "ai.ai_action":
                        configNulls.Remove(tableName);
                        Config.Ai.DataAi_action.Initialize(os, Errors);
                        break;
                    case "ai.ai_condition":
                        configNulls.Remove(tableName);
                        Config.Ai.DataAi_condition.Initialize(os, Errors);
                        break;
                    case "equip.ability":
                        configNulls.Remove(tableName);
                        Config.Equip.DataAbility.Initialize(os, Errors);
                        break;
                    case "equip.equipconfig":
                        configNulls.Remove(tableName);
                        Config.Equip.DataEquipconfig.Initialize(os, Errors);
                        break;
                    case "equip.jewelry":
                        configNulls.Remove(tableName);
                        Config.Equip.DataJewelry.Initialize(os, Errors);
                        break;
                    case "equip.jewelryrandom":
                        configNulls.Remove(tableName);
                        Config.Equip.DataJewelryrandom.Initialize(os, Errors);
                        break;
                    case "equip.jewelrysuit":
                        configNulls.Remove(tableName);
                        Config.Equip.DataJewelrysuit.Initialize(os, Errors);
                        break;
                    case "equip.jewelrytype":
                        configNulls.Remove(tableName);
                        Config.Equip.DataJewelrytype.Initialize(os, Errors);
                        break;
                    case "equip.rank":
                        configNulls.Remove(tableName);
                        Config.Equip.DataRank.Initialize(os, Errors);
                        break;
                    case "other.drop":
                        configNulls.Remove(tableName);
                        Config.Other.DataDrop.Initialize(os, Errors);
                        break;
                    case "other.keytest":
                        configNulls.Remove(tableName);
                        Config.Other.DataKeytest.Initialize(os, Errors);
                        break;
                    case "other.loot":
                        configNulls.Remove(tableName);
                        Config.Other.DataLoot.Initialize(os, Errors);
                        break;
                    case "other.lootitem":
                        configNulls.Remove(tableName);
                        Config.Other.DataLootitem.Initialize(os, Errors);
                        break;
                    case "other.monster":
                        configNulls.Remove(tableName);
                        Config.Other.DataMonster.Initialize(os, Errors);
                        break;
                    case "other.signin":
                        configNulls.Remove(tableName);
                        Config.Other.DataSignin.Initialize(os, Errors);
                        break;
                    case "task.completeconditiontype":
                        configNulls.Remove(tableName);
                        Config.Task.DataCompleteconditiontype.Initialize(os, Errors);
                        break;
                    case "task.task":
                        configNulls.Remove(tableName);
                        Config.Task.DataTask.Initialize(os, Errors);
                        break;
                    case "task.task2":
                        configNulls.Remove(tableName);
                        Config.Task.DataTask2.Initialize(os, Errors);
                        break;
                    case "task.taskextraexp":
                        configNulls.Remove(tableName);
                        Config.Task.DataTaskextraexp.Initialize(os, Errors);
                        break;
                    default:
                        // 未知表，跳过
                        os.SkipBytes(tableSize);
                        break;
                }
            }
            foreach (var t in configNulls)
                Errors.ConfigNull(t);
            // 解析外键引用
            Config.Equip.DataJewelry.Resolve(Errors);
            Config.Equip.DataJewelryrandom.Resolve(Errors);
            Config.Other.DataKeytest.Resolve(Errors);
            Config.Other.DataLoot.Resolve(Errors);
            Config.Other.DataMonster.Resolve(Errors);
            Config.Other.DataSignin.Resolve(Errors);
            Config.Task.DataTask.Resolve(Errors);
            Config.Task.DataTask2.Resolve(Errors);
        }
    }
}
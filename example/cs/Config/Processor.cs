using System.Collections.Generic;

namespace Config
{
    public static class Processor
    {
        // 从 bytes 文件加载（新格式）
        public static void Process(Config.Stream os, LoadErrors errors)
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
                        Config.Ai.DataAi.Initialize(os, errors);
                        break;
                    case "ai.ai_action":
                        configNulls.Remove(tableName);
                        Config.Ai.DataAi_action.Initialize(os, errors);
                        break;
                    case "ai.ai_condition":
                        configNulls.Remove(tableName);
                        Config.Ai.DataAi_condition.Initialize(os, errors);
                        break;
                    case "equip.ability":
                        configNulls.Remove(tableName);
                        Config.Equip.DataAbility.Initialize(os, errors);
                        break;
                    case "equip.equipconfig":
                        configNulls.Remove(tableName);
                        Config.Equip.DataEquipconfig.Initialize(os, errors);
                        break;
                    case "equip.jewelry":
                        configNulls.Remove(tableName);
                        Config.Equip.DataJewelry.Initialize(os, errors);
                        break;
                    case "equip.jewelryrandom":
                        configNulls.Remove(tableName);
                        Config.Equip.DataJewelryrandom.Initialize(os, errors);
                        break;
                    case "equip.jewelrysuit":
                        configNulls.Remove(tableName);
                        Config.Equip.DataJewelrysuit.Initialize(os, errors);
                        break;
                    case "equip.jewelrytype":
                        configNulls.Remove(tableName);
                        Config.Equip.DataJewelrytype.Initialize(os, errors);
                        break;
                    case "equip.rank":
                        configNulls.Remove(tableName);
                        Config.Equip.DataRank.Initialize(os, errors);
                        break;
                    case "other.drop":
                        configNulls.Remove(tableName);
                        Config.Other.DataDrop.Initialize(os, errors);
                        break;
                    case "other.keytest":
                        configNulls.Remove(tableName);
                        Config.Other.DataKeytest.Initialize(os, errors);
                        break;
                    case "other.loot":
                        configNulls.Remove(tableName);
                        Config.Other.DataLoot.Initialize(os, errors);
                        break;
                    case "other.lootitem":
                        configNulls.Remove(tableName);
                        Config.Other.DataLootitem.Initialize(os, errors);
                        break;
                    case "other.monster":
                        configNulls.Remove(tableName);
                        Config.Other.DataMonster.Initialize(os, errors);
                        break;
                    case "other.signin":
                        configNulls.Remove(tableName);
                        Config.Other.DataSignin.Initialize(os, errors);
                        break;
                    case "task.completeconditiontype":
                        configNulls.Remove(tableName);
                        Config.Task.DataCompleteconditiontype.Initialize(os, errors);
                        break;
                    case "task.task":
                        configNulls.Remove(tableName);
                        Config.Task.DataTask.Initialize(os, errors);
                        break;
                    case "task.task2":
                        configNulls.Remove(tableName);
                        Config.Task.DataTask2.Initialize(os, errors);
                        break;
                    case "task.taskextraexp":
                        configNulls.Remove(tableName);
                        Config.Task.DataTaskextraexp.Initialize(os, errors);
                        break;
                    default:
                        // 未知表，跳过
                        os.SkipBytes(tableSize);
                        break;
                }
            }
            foreach (var t in configNulls)
                errors.ConfigNull(t);
            // 解析外键引用
            Config.Equip.DataJewelry.Resolve(errors);
            Config.Equip.DataJewelryrandom.Resolve(errors);
            Config.Other.DataKeytest.Resolve(errors);
            Config.Other.DataLoot.Resolve(errors);
            Config.Other.DataMonster.Resolve(errors);
            Config.Other.DataSignin.Resolve(errors);
            Config.Task.DataTask.Resolve(errors);
            Config.Task.DataTask2.Resolve(errors);
        }
    }
}
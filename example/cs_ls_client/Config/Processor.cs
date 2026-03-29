using System.Collections.Generic;

namespace Config;

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
            "other.ArgCaptureMode",
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
            string tableName = os.ReadTableName();
            // 读取表大小
            int tableSize = os.ReadInt32();

            // 根据表名分发到对应的 Initialize 方法
            switch(tableName)
            {
                case "ai.ai":
                    configNulls.Remove(tableName);
                    Ai.DataAi.Initialize(os, errors);
                    break;
                case "ai.ai_action":
                    configNulls.Remove(tableName);
                    Ai.DataAi_action.Initialize(os, errors);
                    break;
                case "ai.ai_condition":
                    configNulls.Remove(tableName);
                    Ai.DataAi_condition.Initialize(os, errors);
                    break;
                case "equip.ability":
                    configNulls.Remove(tableName);
                    Equip.DataAbility.Initialize(os, errors);
                    break;
                case "equip.equipconfig":
                    configNulls.Remove(tableName);
                    Equip.DataEquipconfig.Initialize(os, errors);
                    break;
                case "equip.jewelry":
                    configNulls.Remove(tableName);
                    Equip.DataJewelry.Initialize(os, errors);
                    break;
                case "equip.jewelryrandom":
                    configNulls.Remove(tableName);
                    Equip.DataJewelryrandom.Initialize(os, errors);
                    break;
                case "equip.jewelrysuit":
                    configNulls.Remove(tableName);
                    Equip.DataJewelrysuit.Initialize(os, errors);
                    break;
                case "equip.jewelrytype":
                    configNulls.Remove(tableName);
                    Equip.DataJewelrytype.Initialize(os, errors);
                    break;
                case "equip.rank":
                    configNulls.Remove(tableName);
                    Equip.DataRank.Initialize(os, errors);
                    break;
                case "other.ArgCaptureMode":
                    configNulls.Remove(tableName);
                    Other.DataArgCaptureMode.Initialize(os, errors);
                    break;
                case "other.drop":
                    configNulls.Remove(tableName);
                    Other.DataDrop.Initialize(os, errors);
                    break;
                case "other.keytest":
                    configNulls.Remove(tableName);
                    Other.DataKeytest.Initialize(os, errors);
                    break;
                case "other.loot":
                    configNulls.Remove(tableName);
                    Other.DataLoot.Initialize(os, errors);
                    break;
                case "other.lootitem":
                    configNulls.Remove(tableName);
                    Other.DataLootitem.Initialize(os, errors);
                    break;
                case "other.monster":
                    configNulls.Remove(tableName);
                    Other.DataMonster.Initialize(os, errors);
                    break;
                case "other.signin":
                    configNulls.Remove(tableName);
                    Other.DataSignin.Initialize(os, errors);
                    break;
                case "task.completeconditiontype":
                    configNulls.Remove(tableName);
                    Task.DataCompleteconditiontype.Initialize(os, errors);
                    break;
                case "task.task":
                    configNulls.Remove(tableName);
                    Task.DataTask.Initialize(os, errors);
                    break;
                case "task.task2":
                    configNulls.Remove(tableName);
                    Task.DataTask2.Initialize(os, errors);
                    break;
                case "task.taskextraexp":
                    configNulls.Remove(tableName);
                    Task.DataTaskextraexp.Initialize(os, errors);
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
        Equip.DataJewelry.Resolve(errors);
        Equip.DataJewelryrandom.Resolve(errors);
        Other.DataKeytest.Resolve(errors);
        Other.DataLoot.Resolve(errors);
        Other.DataMonster.Resolve(errors);
        Other.DataSignin.Resolve(errors);
        Task.DataTask.Resolve(errors);
        Task.DataTask2.Resolve(errors);
    }
}

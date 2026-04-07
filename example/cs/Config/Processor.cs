
namespace Config;

public static class Processor
{
    // 从 bytes 文件加载（新格式）
    public static void Process(ConfigReader reader)
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
        int tableCount = reader.ReadInt32();

        for (int i = 0; i < tableCount; i++)
        {
            // 读取表名
            string tableName = reader.ReadTableName();
            // 读取表大小
            int tableSize = reader.ReadInt32();

            // 根据表名分发到对应的 Initialize 方法
            switch(tableName)
            {
                case "ai.ai":
                    configNulls.Remove(tableName);
                    Ai.DataAi.Initialize(reader);
                    break;
                case "ai.ai_action":
                    configNulls.Remove(tableName);
                    Ai.DataAi_action.Initialize(reader);
                    break;
                case "ai.ai_condition":
                    configNulls.Remove(tableName);
                    Ai.DataAi_condition.Initialize(reader);
                    break;
                case "equip.ability":
                    configNulls.Remove(tableName);
                    Equip.DataAbility.Initialize(reader);
                    break;
                case "equip.equipconfig":
                    configNulls.Remove(tableName);
                    Equip.DataEquipconfig.Initialize(reader);
                    break;
                case "equip.jewelry":
                    configNulls.Remove(tableName);
                    Equip.DataJewelry.Initialize(reader);
                    break;
                case "equip.jewelryrandom":
                    configNulls.Remove(tableName);
                    Equip.DataJewelryrandom.Initialize(reader);
                    break;
                case "equip.jewelrysuit":
                    configNulls.Remove(tableName);
                    Equip.DataJewelrysuit.Initialize(reader);
                    break;
                case "equip.jewelrytype":
                    configNulls.Remove(tableName);
                    Equip.DataJewelrytype.Initialize(reader);
                    break;
                case "equip.rank":
                    configNulls.Remove(tableName);
                    Equip.DataRank.Initialize(reader);
                    break;
                case "other.ArgCaptureMode":
                    configNulls.Remove(tableName);
                    Other.DataArgCaptureMode.Initialize(reader);
                    break;
                case "other.drop":
                    configNulls.Remove(tableName);
                    Other.DataDrop.Initialize(reader);
                    break;
                case "other.keytest":
                    configNulls.Remove(tableName);
                    Other.DataKeytest.Initialize(reader);
                    break;
                case "other.loot":
                    configNulls.Remove(tableName);
                    Other.DataLoot.Initialize(reader);
                    break;
                case "other.lootitem":
                    configNulls.Remove(tableName);
                    Other.DataLootitem.Initialize(reader);
                    break;
                case "other.monster":
                    configNulls.Remove(tableName);
                    Other.DataMonster.Initialize(reader);
                    break;
                case "other.signin":
                    configNulls.Remove(tableName);
                    Other.DataSignin.Initialize(reader);
                    break;
                case "task.completeconditiontype":
                    configNulls.Remove(tableName);
                    Task.DataCompleteconditiontype.Initialize(reader);
                    break;
                case "task.task":
                    configNulls.Remove(tableName);
                    Task.DataTask.Initialize(reader);
                    break;
                case "task.task2":
                    configNulls.Remove(tableName);
                    Task.DataTask2.Initialize(reader);
                    break;
                case "task.taskextraexp":
                    configNulls.Remove(tableName);
                    Task.DataTaskextraexp.Initialize(reader);
                    break;
                default:
                    // 未知表，跳过
                    reader.SkipBytes(tableSize);
                    break;
            }
        }
        foreach (var t in configNulls)
            reader.TableNotInData(t);
        // 解析外键引用
        Equip.DataJewelry.Resolve(reader);
        Equip.DataJewelryrandom.Resolve(reader);
        Other.DataKeytest.Resolve(reader);
        Other.DataLoot.Resolve(reader);
        Other.DataMonster.Resolve(reader);
        Other.DataSignin.Resolve(reader);
        Task.DataTask.Resolve(reader);
        Task.DataTask2.Resolve(reader);
    }
}

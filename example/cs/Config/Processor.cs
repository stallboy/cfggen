
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
                    Ai.DAi.Initialize(reader);
                    break;
                case "ai.ai_action":
                    configNulls.Remove(tableName);
                    Ai.DAi_action.Initialize(reader);
                    break;
                case "ai.ai_condition":
                    configNulls.Remove(tableName);
                    Ai.DAi_condition.Initialize(reader);
                    break;
                case "equip.ability":
                    configNulls.Remove(tableName);
                    Equip.DAbilityInfo.Initialize(reader);
                    break;
                case "equip.equipconfig":
                    configNulls.Remove(tableName);
                    Equip.DEquipconfig.Initialize(reader);
                    break;
                case "equip.jewelry":
                    configNulls.Remove(tableName);
                    Equip.DJewelry.Initialize(reader);
                    break;
                case "equip.jewelryrandom":
                    configNulls.Remove(tableName);
                    Equip.DJewelryrandom.Initialize(reader);
                    break;
                case "equip.jewelrysuit":
                    configNulls.Remove(tableName);
                    Equip.DJewelrysuit.Initialize(reader);
                    break;
                case "equip.jewelrytype":
                    configNulls.Remove(tableName);
                    Equip.DJewelrytypeInfo.Initialize(reader);
                    break;
                case "equip.rank":
                    configNulls.Remove(tableName);
                    Equip.DRankInfo.Initialize(reader);
                    break;
                case "other.ArgCaptureMode":
                    configNulls.Remove(tableName);
                    Other.DArgCaptureModeInfo.Initialize(reader);
                    break;
                case "other.drop":
                    configNulls.Remove(tableName);
                    Other.DDrop.Initialize(reader);
                    break;
                case "other.keytest":
                    configNulls.Remove(tableName);
                    Other.DKeytest.Initialize(reader);
                    break;
                case "other.loot":
                    configNulls.Remove(tableName);
                    Other.DLoot.Initialize(reader);
                    break;
                case "other.lootitem":
                    configNulls.Remove(tableName);
                    Other.DLootitem.Initialize(reader);
                    break;
                case "other.monster":
                    configNulls.Remove(tableName);
                    Other.DMonster.Initialize(reader);
                    break;
                case "other.signin":
                    configNulls.Remove(tableName);
                    Other.DSignin.Initialize(reader);
                    break;
                case "task.completeconditiontype":
                    configNulls.Remove(tableName);
                    Task.DCompleteconditiontypeInfo.Initialize(reader);
                    break;
                case "task.task":
                    configNulls.Remove(tableName);
                    Task.DTask.Initialize(reader);
                    break;
                case "task.task2":
                    configNulls.Remove(tableName);
                    Task.DTask2.Initialize(reader);
                    break;
                case "task.taskextraexp":
                    configNulls.Remove(tableName);
                    Task.DTaskextraexp.Initialize(reader);
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
        Equip.DJewelry.Resolve(reader);
        Equip.DJewelryrandom.Resolve(reader);
        Other.DKeytest.Resolve(reader);
        Other.DLoot.Resolve(reader);
        Other.DMonster.Resolve(reader);
        Other.DSignin.Resolve(reader);
        Task.DTask.Resolve(reader);
        Task.DTask2.Resolve(reader);
    }
}

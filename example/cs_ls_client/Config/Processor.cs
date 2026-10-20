using System;
using System.Collections.Generic;
namespace Config
{

public static class Processor
{
    private static readonly string Schema = ConfigPackage.SchemaKey(new ConfigReader(Convert.FromBase64String("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X0VudHJ5DAAAAAEAAgAAAAgAAABJbnN0YW5jZQkAAABJbnN0YW5jZTINAAAAZXF1aXAuamV3ZWxyeQoAAAABCgAAAAIAAABJRAIAAAAEAAAATmFtZQUAAAAIAAAASWNvbkZpbGUFAAAABwAAAEx2bFJhbmsHAAAACQAAAExldmVsUmFuawUAAABKVHlwZQUAAAAGAAAAU3VpdElEAgAAAAoAAABLZXlBYmlsaXR5AgAAAA8AAABLZXlBYmlsaXR5VmFsdWUCAAAACQAAAFNhbGVQcmljZQIAAAALAAAARGVzY3JpcHRpb24FAAAAEwAAAGVxdWlwLmpld2VscnlyYW5kb20KAAAAAQQAAAAHAAAATHZsUmFuawcAAAAJAAAATGV2ZWxSYW5rCwAAAEF0dGFja1JhbmdlBwAAAAUAAABSYW5nZQoAAABPdGhlclJhbmdlCAAAAAcAAAAFAAAAUmFuZ2UIAAAAVGVzdFBhY2sIAAAABwAAABIAAABlcXVpcC5UZXN0UGFja0JlYW4RAAAAZXF1aXAuamV3ZWxyeXN1aXQKAAAAAQoAAAAGAAAAU3VpdElEAgAAAAUAAABFbmFtZQUAAAAEAAAATmFtZQYAAAAIAAAAQWJpbGl0eTECAAAADQAAAEFiaWxpdHkxVmFsdWUCAAAACAAAAEFiaWxpdHkyAgAAAA0AAABBYmlsaXR5MlZhbHVlAgAAAAgAAABBYmlsaXR5MwIAAAANAAAAQWJpbGl0eTNWYWx1ZQIAAAAIAAAAU3VpdExpc3QIAAAAAgAAABcAAABlcXVpcC5qZXdlbHJ5c3VpdF9FbnRyeQwAAAABAQEAAAALAAAAU3BlY2lhbFN1aXQEAAAAEQAAAGVxdWlwLmpld2Vscnl0eXBlDAAAAAAABAAAAAQAAABKYWRlCAAAAEJyYWNlbGV0BQAAAE1hZ2ljBgAAAEJvdHRsZRgAAABlcXVpcC5qZXdlbHJ5dHlwZV9EZXRhaWwKAAAAAQEAAAAIAAAAVHlwZU5hbWUFAAAACgAAAGVxdWlwLnJhbmsMAAAAAAEGAAAABQAAAHdoaXRlAAAAAAUAAABncmVlbgEAAAAEAAAAYmx1ZQIAAAAGAAAAcHVycGxlAwAAAAYAAAB5ZWxsb3cEAAAAAwAAAHJlZAUAAAARAAAAZXF1aXAucmFua19EZXRhaWwKAAAAAQMAAAAGAAAAUmFua0lEAgAAAAgAAABSYW5rTmFtZQUAAAAMAAAAUmFua1Nob3dOYW1lBQAAABQAAABvdGhlci5BcmdDYXB0dXJlTW9kZQwAAAAAAAIAAAAIAAAAU25hcHNob3QHAAAARHluYW1pYxsAAABvdGhlci5BcmdDYXB0dXJlTW9kZV9EZXRhaWwKAAAAAQMAAAAEAAAAbmFtZQUAAAACAAAAaWQCAAAABwAAAGNvbW1lbnQFAAAACgAAAG90aGVyLmRyb3AKAAAAAQQAAAAGAAAAZHJvcGlkAgAAAAQAAABuYW1lBgAAAAUAAABpdGVtcwgAAAAHAAAADgAAAG90aGVyLkRyb3BJdGVtBwAAAHRlc3RtYXAJAAAAAgAAAAIAAAANAAAAb3RoZXIua2V5dGVzdAoAAAABBgAAAAMAAABpZDECAAAAAwAAAGlkMgMAAAADAAAAaWQzAgAAAAMAAABpZHMIAAAAAgAAAAgAAABlbnVtVGVzdAUAAAAIAAAAZW51bUxpc3QIAAAABQAAAAoAAABvdGhlci5sb290CgAAAAEEAAAABgAAAGxvb3RpZAIAAAAFAAAAZW5hbWUFAAAABAAAAG5hbWUGAAAACgAAAGNoYW5jZUxpc3QIAAAAAgAAAA4AAABvdGhlci5sb290aXRlbQoAAAABBQAAAAYAAABsb290aWQCAAAABgAAAGl0ZW1pZAIAAAAGAAAAY2hhbmNlAgAAAAgAAABjb3VudG1pbgIAAAAIAAAAY291bnRtYXgCAAAADQAAAG90aGVyLm1vbnN0ZXIKAAAAAQYAAAACAAAAaWQCAAAABwAAAHBvc0xpc3QIAAAABwAAAAgAAABQb3NpdGlvbgYAAABsb290SWQCAAAACgAAAGxvb3RJdGVtSWQCAAAACAAAAGVudW1NYXAxCQAAAAUAAAACAAAACAAAAGVudW1NYXAyCQAAAAIAAAAFAAAADAAAAG90aGVyLnNpZ25pbgoAAAABBQAAAAIAAABpZAIAAAANAAAAaXRlbTJjb3VudE1hcAkAAAACAAAAAgAAABMAAAB2aXBpdGVtMnZpcGNvdW50TWFwCQAAAAIAAAACAAAACAAAAHZpcGxldmVsAgAAAAgAAABJY29uRmlsZQUAAAAaAAAAdGFzay5jb21wbGV0ZWNvbmRpdGlvbnR5cGUMAAAAAAEHAAAACwAAAEtpbGxNb25zdGVyAQAAAAcAAABUYWxrTnBjAgAAAAsAAABDb2xsZWN0SXRlbQMAAAAMAAAAQ29uZGl0aW9uQW5kBAAAAAQAAABDaGF0BQAAAAwAAABUZXN0Tm9Db2x1bW4GAAAAAgAAAGFhBwAAACEAAAB0YXNrLmNvbXBsZXRlY29uZGl0aW9udHlwZV9EZXRhaWwKAAAAAQIAAAACAAAAaWQCAAAABAAAAG5hbWUFAAAACQAAAHRhc2sudGFzawoAAAABBgAAAAYAAAB0YXNraWQCAAAABAAAAG5hbWUIAAAABgAAAAgAAABuZXh0dGFzawIAAAARAAAAY29tcGxldGVjb25kaXRpb24HAAAAFgAAAHRhc2suY29tcGxldGVjb25kaXRpb24DAAAAZXhwAgAAAA8AAAB0ZXN0RGVmYXVsdEJlYW4HAAAAFAAAAHRhc2suVGVzdERlZmF1bHRCZWFuCgAAAHRhc2sudGFzazIKAAAAAQsAAAAGAAAAdGFza2lkAgAAAAQAAABuYW1lCAAAAAYAAAAIAAAAbmV4dHRhc2sCAAAAEQAAAGNvbXBsZXRlY29uZGl0aW9uBwAAABYAAAB0YXNrLmNvbXBsZXRlY29uZGl0aW9uAwAAAGV4cAIAAAAIAAAAdGVzdEJvb2wBAAAACgAAAHRlc3RTdHJpbmcFAAAACgAAAHRlc3RTdHJ1Y3QHAAAACAAAAFBvc2l0aW9uCAAAAHRlc3RMaXN0CAAAAAIAAAAOAAAAdGVzdExpc3RTdHJ1Y3QIAAAABwAAAAgAAABQb3NpdGlvbhEAAAB0ZXN0TGlzdEludGVyZmFjZQgAAAAHAAAADgAAAGFpLlRyaWdnZXJUaWNrEQAAAHRhc2sudGFza2V4dHJhZXhwCgAAAAEIAAAABgAAAHRhc2tpZAIAAAAIAAAAZXh0cmFleHACAAAABQAAAHRlc3QxBQAAAAUAAAB0ZXN0MgUAAAAGAAAAZmllbGRhBQAAAAYAAABmaWVsZGIFAAAABgAAAGZpZWxkYwUAAAAGAAAAZmllbGRkBQAAAA==")));
    private static readonly string[] Languages = { "zh_cn", "en", "tw" };
    public static ConfigMgr LoadPackagesAndSetMgr(params ConfigPackage[] packages)
    {
        var config = LoadPackages(packages);
        ConfigMgr.SetMgr(config);
        return config;
    }

    public static ConfigMgr LoadPackages(params ConfigPackage[] packages)
    {
        ConfigPackage.ValidateOrder(packages);
        var errors = new LoadErrors();
        foreach (var pack in packages) pack.ValidateSchema(Schema, errors);
        errors.CheckErrors();
        var config = new ConfigMgr();
        {
            var rows = ReadTable(packages, "ai.ai", Ai.DAi._create,
                Ai.DAi.PrimaryKey, config._ai_dai_Origins, errors);
            Ai.DAi.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "ai.ai_action", Ai.DAiAction._create,
                Ai.DAiAction.PrimaryKey, config._ai_dai_uaction_Origins, errors);
            Ai.DAiAction.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "ai.ai_condition", Ai.DAiCondition._create,
                Ai.DAiCondition.PrimaryKey, config._ai_dai_ucondition_Origins, errors);
            Ai.DAiCondition.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "equip.ability", Equip.DAbilityInfo._create,
                Equip.DAbilityInfo.PrimaryKey, config._equip_dability_Origins, errors);
            Equip.DAbilityInfo.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "equip.equipconfig", Equip.DEquipconfig._create,
                Equip.DEquipconfig.PrimaryKey, config._equip_dequipconfig_Origins, errors);
            Equip.DEquipconfig.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "equip.jewelry", Equip.DJewelry._create,
                Equip.DJewelry.PrimaryKey, config._equip_djewelry_Origins, errors);
            Equip.DJewelry.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "equip.jewelryrandom", Equip.DJewelryrandom._create,
                Equip.DJewelryrandom.PrimaryKey, config._equip_djewelryrandom_Origins, errors);
            Equip.DJewelryrandom.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "equip.jewelrysuit", Equip.DJewelrysuit._create,
                Equip.DJewelrysuit.PrimaryKey, config._equip_djewelrysuit_Origins, errors);
            Equip.DJewelrysuit.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "equip.jewelrytype", Equip.DJewelrytypeInfo._create,
                Equip.DJewelrytypeInfo.PrimaryKey, config._equip_djewelrytype_Origins, errors);
            Equip.DJewelrytypeInfo.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "equip.rank", Equip.DRankInfo._create,
                Equip.DRankInfo.PrimaryKey, config._equip_drank_Origins, errors);
            Equip.DRankInfo.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "other.ArgCaptureMode", Other.DArgCaptureModeInfo._create,
                Other.DArgCaptureModeInfo.PrimaryKey, config._other_dArgCaptureMode_Origins, errors);
            Other.DArgCaptureModeInfo.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "other.drop", Other.DDrop._create,
                Other.DDrop.PrimaryKey, config._other_ddrop_Origins, errors);
            Other.DDrop.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "other.keytest", Other.DKeytest._create,
                Other.DKeytest.PrimaryKey, config._other_dkeytest_Origins, errors);
            Other.DKeytest.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "other.loot", Other.DLoot._create,
                Other.DLoot.PrimaryKey, config._other_dloot_Origins, errors);
            Other.DLoot.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "other.lootitem", Other.DLootitem._create,
                Other.DLootitem.PrimaryKey, config._other_dlootitem_Origins, errors);
            Other.DLootitem.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "other.monster", Other.DMonster._create,
                Other.DMonster.PrimaryKey, config._other_dmonster_Origins, errors);
            Other.DMonster.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "other.signin", Other.DSignin._create,
                Other.DSignin.PrimaryKey, config._other_dsignin_Origins, errors);
            Other.DSignin.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "task.completeconditiontype", Task.DCompleteconditiontypeInfo._create,
                Task.DCompleteconditiontypeInfo.PrimaryKey, config._task_dcompleteconditiontype_Origins, errors);
            Task.DCompleteconditiontypeInfo.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "task.task", Task.DTask._create,
                Task.DTask.PrimaryKey, config._task_dtask_Origins, errors);
            Task.DTask.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "task.task2", Task.DTask2._create,
                Task.DTask2.PrimaryKey, config._task_dtask2_Origins, errors);
            Task.DTask2.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "task.taskextraexp", Task.DTaskextraexp._create,
                Task.DTaskextraexp.PrimaryKey, config._task_dtaskextraexp_Origins, errors);
            Task.DTaskextraexp.InitializeAll(rows, errors, config);
        }
        errors.CheckErrors();
        foreach (var row in Equip.DJewelry.All(config)) row._resolve(errors, config);
        foreach (var row in Equip.DJewelryrandom.All(config)) row._resolve(errors, config);
        foreach (var row in Other.DKeytest.All(config)) row._resolve(errors, config);
        foreach (var row in Other.DLoot.All(config)) row._resolve(errors, config);
        foreach (var row in Other.DMonster.All(config)) row._resolve(errors, config);
        foreach (var row in Other.DSignin.All(config)) row._resolve(errors, config);
        foreach (var row in Task.DTask.All(config)) row._resolve(errors, config);
        foreach (var row in Task.DTask2.All(config)) row._resolve(errors, config);
        errors.CheckErrors();
        foreach (var row in Ai.DAi.All(config)) Ready(row, config);
        foreach (var row in Ai.DAiAction.All(config)) Ready(row, config);
        foreach (var row in Ai.DAiCondition.All(config)) Ready(row, config);
        foreach (var row in Equip.DAbilityInfo.All(config)) Ready(row, config);
        foreach (var row in Equip.DEquipconfig.All(config)) Ready(row, config);
        foreach (var row in Equip.DJewelry.All(config)) Ready(row, config);
        foreach (var row in Equip.DJewelryrandom.All(config)) Ready(row, config);
        foreach (var row in Equip.DJewelrysuit.All(config)) Ready(row, config);
        foreach (var row in Equip.DJewelrytypeInfo.All(config)) Ready(row, config);
        foreach (var row in Equip.DRankInfo.All(config)) Ready(row, config);
        foreach (var row in Other.DArgCaptureModeInfo.All(config)) Ready(row, config);
        foreach (var row in Other.DDrop.All(config)) Ready(row, config);
        foreach (var row in Other.DKeytest.All(config)) Ready(row, config);
        foreach (var row in Other.DLoot.All(config)) Ready(row, config);
        foreach (var row in Other.DLootitem.All(config)) Ready(row, config);
        foreach (var row in Other.DMonster.All(config)) Ready(row, config);
        foreach (var row in Other.DSignin.All(config)) Ready(row, config);
        foreach (var row in Task.DCompleteconditiontypeInfo.All(config)) Ready(row, config);
        foreach (var row in Task.DTask.All(config)) Ready(row, config);
        foreach (var row in Task.DTask2.All(config)) Ready(row, config);
        foreach (var row in Task.DTaskextraexp.All(config)) Ready(row, config);
        config.MarkReady();
        return config;
    }

    private static void Ready<T>(T value, ConfigMgr config)
    {
        if (value is IConfigExtension extension) extension.OnConfigReady(config);
    }

    private static void Ready(Ai.DAi value, ConfigMgr config)
    {
        Ready(value.TrigTick, config);
        Ready<Ai.DAi>(value, config);
    }

    private static void Ready(Equip.DTestPackBean value, ConfigMgr config)
    {
        Ready(value.IRange, config);
        Ready<Equip.DTestPackBean>(value, config);
    }

    private static void Ready(Equip.DJewelry value, ConfigMgr config)
    {
        Ready(value.LvlRank, config);
        Ready<Equip.DJewelry>(value, config);
    }

    private static void Ready(Equip.DJewelryrandom value, ConfigMgr config)
    {
        Ready(value.LvlRank, config);
        Ready(value.AttackRange, config);
        foreach (var item in value.OtherRange) Ready(item, config);
        foreach (var item in value.TestPack) Ready(item, config);
        Ready<Equip.DJewelryrandom>(value, config);
    }

    private static void Ready(Other.DDrop value, ConfigMgr config)
    {
        foreach (var item in value.Items) Ready(item, config);
        Ready<Other.DDrop>(value, config);
    }

    private static void Ready(Other.DMonster value, ConfigMgr config)
    {
        foreach (var item in value.PosList) Ready(item, config);
        Ready<Other.DMonster>(value, config);
    }

    private static void Ready(Task.DTestDefaultBean value, ConfigMgr config)
    {
        Ready(value.TestSubBean, config);
        Ready<Task.DTestDefaultBean>(value, config);
    }

    private static void Ready(Task.Completecondition.DConditionAnd value, ConfigMgr config)
    {
        Ready(value.Cond1, config);
        Ready(value.Cond2, config);
        Ready<Task.Completecondition.DConditionAnd>(value, config);
    }

    private static void Ready(Task.DTask value, ConfigMgr config)
    {
        Ready(value.Completecondition, config);
        Ready(value.TestDefaultBean, config);
        Ready<Task.DTask>(value, config);
    }

    private static void Ready(Task.DTask2 value, ConfigMgr config)
    {
        Ready(value.Completecondition, config);
        Ready(value.TestStruct, config);
        foreach (var item in value.TestListStruct) Ready(item, config);
        foreach (var item in value.TestListInterface) Ready(item, config);
        Ready<Task.DTask2>(value, config);
    }

    private static void Ready(Task.DCompletecondition value, ConfigMgr config)
    {
        if (value is Task.Completecondition.DConditionAnd item) Ready(item, config);
        else Ready<Task.DCompletecondition>(value, config);
    }

    private static List<T> ReadTable<K, T>(ConfigPackage[] packages, string table,
        Func<ConfigReader, T> readRow, Func<T, K> primaryKey, Dictionary<K, List<string>> origins,
        LoadErrors errors) where K : notnull
    {
        var rows = new List<T>();
        var slots = new Dictionary<K, int>();
        string block = "." + table;
        foreach (var pack in packages) {
            if (!pack.HasBlock(block)) continue;
            string packName = pack.Name;
            var input = pack.Block(block, Languages); input.SetErrors(errors);
            int count = input.ReadCount();
            for (int j = 0; j < count; j++) {
                var row = readRow(input);
                K key = primaryKey(row);
                if (!origins.TryGetValue(key, out var chain)) { chain = new List<string>(); origins.Add(key, chain); }
                if (chain.Count != 0 && chain[chain.Count - 1] == packName) { errors.DuplicateRecord(table, packName, key); continue; }
                if (slots.TryGetValue(key, out var slot)) rows[slot] = row;
                else { slots.Add(key, rows.Count); rows.Add(row); }
                chain.Add(packName);
            }
            input.RequireEnd();
        }
        return rows;
    }
}
}

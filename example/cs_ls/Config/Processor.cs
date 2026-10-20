namespace Config;

public static class Processor
{
    private static readonly string Schema = ConfigPackage.SchemaKey(new ConfigReader(Convert.FromBase64String("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aWdfRW50cnkMAAAAAQACAAAACAAAAEluc3RhbmNlCQAAAEluc3RhbmNlMg0AAABlcXVpcC5qZXdlbHJ5CgAAAAEKAAAAAgAAAElEAgAAAAQAAABOYW1lBQAAAAgAAABJY29uRmlsZQUAAAAHAAAATHZsUmFuawcAAAAJAAAATGV2ZWxSYW5rBQAAAEpUeXBlBQAAAAYAAABTdWl0SUQCAAAACgAAAEtleUFiaWxpdHkCAAAADwAAAEtleUFiaWxpdHlWYWx1ZQIAAAAJAAAAU2FsZVByaWNlAgAAAAsAAABEZXNjcmlwdGlvbgUAAAAUAAAAZXF1aXAuamV3ZWxyeV9yYW5kb20KAAAAAQQAAAAHAAAATHZsUmFuawcAAAAJAAAATGV2ZWxSYW5rCwAAAEF0dGFja1JhbmdlBwAAAAUAAABSYW5nZQoAAABPdGhlclJhbmdlCAAAAAcAAAAFAAAAUmFuZ2UIAAAAVGVzdFBhY2sIAAAABwAAABIAAABlcXVpcC5UZXN0UGFja0JlYW4SAAAAZXF1aXAuamV3ZWxyeV9zdWl0CgAAAAEKAAAABgAAAFN1aXRJRAIAAAAFAAAARW5hbWUFAAAABAAAAE5hbWUGAAAACAAAAEFiaWxpdHkxAgAAAA0AAABBYmlsaXR5MVZhbHVlAgAAAAgAAABBYmlsaXR5MgIAAAANAAAAQWJpbGl0eTJWYWx1ZQIAAAAIAAAAQWJpbGl0eTMCAAAADQAAAEFiaWxpdHkzVmFsdWUCAAAACAAAAFN1aXRMaXN0CAAAAAIAAAAYAAAAZXF1aXAuamV3ZWxyeV9zdWl0X0VudHJ5DAAAAAEBAQAAAAsAAABTcGVjaWFsU3VpdAQAAAASAAAAZXF1aXAuamV3ZWxyeV90eXBlDAAAAAAABAAAAAQAAABKYWRlCAAAAEJyYWNlbGV0BQAAAE1hZ2ljBgAAAEJvdHRsZRkAAABlcXVpcC5qZXdlbHJ5X3R5cGVfRGV0YWlsCgAAAAEBAAAACAAAAFR5cGVOYW1lBQAAAAoAAABlcXVpcC5yYW5rDAAAAAABBgAAAAUAAAB3aGl0ZQAAAAAFAAAAZ3JlZW4BAAAABAAAAGJsdWUCAAAABgAAAHB1cnBsZQMAAAAGAAAAeWVsbG93BAAAAAMAAAByZWQFAAAAEQAAAGVxdWlwLnJhbmtfRGV0YWlsCgAAAAEDAAAABgAAAFJhbmtJRAIAAAAIAAAAUmFua05hbWUFAAAADAAAAFJhbmtTaG93TmFtZQUAAAAUAAAAb3RoZXIuQXJnQ2FwdHVyZU1vZGUMAAAAAAACAAAACAAAAFNuYXBzaG90BwAAAER5bmFtaWMbAAAAb3RoZXIuQXJnQ2FwdHVyZU1vZGVfRGV0YWlsCgAAAAEDAAAABAAAAG5hbWUFAAAAAgAAAGlkAgAAAAcAAABjb21tZW50BQAAAAoAAABvdGhlci5kcm9wCgAAAAEEAAAABgAAAGRyb3BpZAIAAAAEAAAAbmFtZQYAAAAFAAAAaXRlbXMIAAAABwAAAA4AAABvdGhlci5Ecm9wSXRlbQcAAAB0ZXN0bWFwCQAAAAIAAAACAAAADgAAAG90aGVyLmtleV90ZXN0CgAAAAEGAAAAAwAAAGlkMQIAAAADAAAAaWQyAwAAAAMAAABpZDMCAAAAAwAAAGlkcwgAAAACAAAACAAAAGVudW1UZXN0BQAAAAgAAABlbnVtTGlzdAgAAAAFAAAACgAAAG90aGVyLmxvb3QKAAAAAQQAAAAGAAAAbG9vdGlkAgAAAAUAAABlbmFtZQUAAAAEAAAAbmFtZQYAAAAKAAAAY2hhbmNlTGlzdAgAAAACAAAADwAAAG90aGVyLmxvb3RfaXRlbQoAAAABBQAAAAYAAABsb290aWQCAAAABgAAAGl0ZW1pZAIAAAAGAAAAY2hhbmNlAgAAAAgAAABjb3VudG1pbgIAAAAIAAAAY291bnRtYXgCAAAADQAAAG90aGVyLm1vbnN0ZXIKAAAAAQYAAAACAAAAaWQCAAAABwAAAHBvc0xpc3QIAAAABwAAAAgAAABQb3NpdGlvbgYAAABsb290SWQCAAAACgAAAGxvb3RJdGVtSWQCAAAACAAAAGVudW1NYXAxCQAAAAUAAAACAAAACAAAAGVudW1NYXAyCQAAAAIAAAAFAAAADQAAAG90aGVyLnNpZ25faW4KAAAAAQUAAAACAAAAaWQCAAAADQAAAGl0ZW0yY291bnRNYXAJAAAAAgAAAAIAAAATAAAAdmlwaXRlbTJ2aXBjb3VudE1hcAkAAAACAAAAAgAAAAgAAAB2aXBsZXZlbAIAAAAIAAAASWNvbkZpbGUFAAAAHAAAAHRhc2suY29tcGxldGVfY29uZGl0aW9uX3R5cGUMAAAAAAEHAAAACwAAAEtpbGxNb25zdGVyAQAAAAcAAABUYWxrTnBjAgAAAAsAAABDb2xsZWN0SXRlbQMAAAAMAAAAQ29uZGl0aW9uQW5kBAAAAAQAAABDaGF0BQAAAAwAAABUZXN0Tm9Db2x1bW4GAAAAAgAAAGFhBwAAACMAAAB0YXNrLmNvbXBsZXRlX2NvbmRpdGlvbl90eXBlX0RldGFpbAoAAAABAgAAAAIAAABpZAIAAAAEAAAAbmFtZQUAAAAJAAAAdGFzay50YXNrCgAAAAEGAAAABgAAAHRhc2tpZAIAAAAEAAAAbmFtZQgAAAAGAAAACAAAAG5leHR0YXNrAgAAABEAAABjb21wbGV0ZWNvbmRpdGlvbgcAAAAWAAAAdGFzay5jb21wbGV0ZWNvbmRpdGlvbgMAAABleHACAAAADwAAAHRlc3REZWZhdWx0QmVhbgcAAAAUAAAAdGFzay5UZXN0RGVmYXVsdEJlYW4KAAAAdGFzay50YXNrMgoAAAABCwAAAAYAAAB0YXNraWQCAAAABAAAAG5hbWUIAAAABgAAAAgAAABuZXh0dGFzawIAAAARAAAAY29tcGxldGVjb25kaXRpb24HAAAAFgAAAHRhc2suY29tcGxldGVjb25kaXRpb24DAAAAZXhwAgAAAAgAAAB0ZXN0Qm9vbAEAAAAKAAAAdGVzdFN0cmluZwUAAAAKAAAAdGVzdFN0cnVjdAcAAAAIAAAAUG9zaXRpb24IAAAAdGVzdExpc3QIAAAAAgAAAA4AAAB0ZXN0TGlzdFN0cnVjdAgAAAAHAAAACAAAAFBvc2l0aW9uEQAAAHRlc3RMaXN0SW50ZXJmYWNlCAAAAAcAAAAOAAAAYWkuVHJpZ2dlclRpY2sTAAAAdGFzay50YXNrX2V4dHJhX2V4cAoAAAABCAAAAAYAAAB0YXNraWQCAAAACAAAAGV4dHJhZXhwAgAAAAUAAAB0ZXN0MQUAAAAFAAAAdGVzdDIFAAAABgAAAGZpZWxkYQUAAAAGAAAAZmllbGRiBQAAAAYAAABmaWVsZGMFAAAABgAAAGZpZWxkZAUAAAA=")));
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
            var rows = ReadTable(packages, "equip.equip_config", Equip.DEquipConfig._create,
                Equip.DEquipConfig.PrimaryKey, config._equip_dequip_uconfig_Origins, errors);
            Equip.DEquipConfig.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "equip.jewelry", Equip.DJewelry._create,
                Equip.DJewelry.PrimaryKey, config._equip_djewelry_Origins, errors);
            Equip.DJewelry.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "equip.jewelry_random", Equip.DJewelryRandom._create,
                Equip.DJewelryRandom.PrimaryKey, config._equip_djewelry_urandom_Origins, errors);
            Equip.DJewelryRandom.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "equip.jewelry_suit", Equip.DJewelrySuit._create,
                Equip.DJewelrySuit.PrimaryKey, config._equip_djewelry_usuit_Origins, errors);
            Equip.DJewelrySuit.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "equip.jewelry_type", Equip.DJewelryTypeInfo._create,
                Equip.DJewelryTypeInfo.PrimaryKey, config._equip_djewelry_utype_Origins, errors);
            Equip.DJewelryTypeInfo.InitializeAll(rows, errors, config);
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
            var rows = ReadTable(packages, "other.key_test", Other.DKeyTest._create,
                Other.DKeyTest.PrimaryKey, config._other_dkey_utest_Origins, errors);
            Other.DKeyTest.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "other.loot", Other.DLoot._create,
                Other.DLoot.PrimaryKey, config._other_dloot_Origins, errors);
            Other.DLoot.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "other.loot_item", Other.DLootItem._create,
                Other.DLootItem.PrimaryKey, config._other_dloot_uitem_Origins, errors);
            Other.DLootItem.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "other.monster", Other.DMonster._create,
                Other.DMonster.PrimaryKey, config._other_dmonster_Origins, errors);
            Other.DMonster.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "other.sign_in", Other.DSignIn._create,
                Other.DSignIn.PrimaryKey, config._other_dsign_uin_Origins, errors);
            Other.DSignIn.InitializeAll(rows, errors, config);
        }
        {
            var rows = ReadTable(packages, "task.complete_condition_type", Task.DCompleteConditionTypeInfo._create,
                Task.DCompleteConditionTypeInfo.PrimaryKey, config._task_dcomplete_ucondition_utype_Origins, errors);
            Task.DCompleteConditionTypeInfo.InitializeAll(rows, errors, config);
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
            var rows = ReadTable(packages, "task.task_extra_exp", Task.DTaskExtraExp._create,
                Task.DTaskExtraExp.PrimaryKey, config._task_dtask_uextra_uexp_Origins, errors);
            Task.DTaskExtraExp.InitializeAll(rows, errors, config);
        }
        errors.CheckErrors();
        foreach (var row in Equip.DJewelry.All(config)) row._resolve(errors, config);
        foreach (var row in Equip.DJewelryRandom.All(config)) row._resolve(errors, config);
        foreach (var row in Other.DKeyTest.All(config)) row._resolve(errors, config);
        foreach (var row in Other.DLoot.All(config)) row._resolve(errors, config);
        foreach (var row in Other.DMonster.All(config)) row._resolve(errors, config);
        foreach (var row in Other.DSignIn.All(config)) row._resolve(errors, config);
        foreach (var row in Task.DTask.All(config)) row._resolve(errors, config);
        foreach (var row in Task.DTask2.All(config)) row._resolve(errors, config);
        errors.CheckErrors();
        foreach (var row in Ai.DAi.All(config)) Ready(row, config);
        foreach (var row in Ai.DAiAction.All(config)) Ready(row, config);
        foreach (var row in Ai.DAiCondition.All(config)) Ready(row, config);
        foreach (var row in Equip.DAbilityInfo.All(config)) Ready(row, config);
        foreach (var row in Equip.DEquipConfig.All(config)) Ready(row, config);
        foreach (var row in Equip.DJewelry.All(config)) Ready(row, config);
        foreach (var row in Equip.DJewelryRandom.All(config)) Ready(row, config);
        foreach (var row in Equip.DJewelrySuit.All(config)) Ready(row, config);
        foreach (var row in Equip.DJewelryTypeInfo.All(config)) Ready(row, config);
        foreach (var row in Equip.DRankInfo.All(config)) Ready(row, config);
        foreach (var row in Other.DArgCaptureModeInfo.All(config)) Ready(row, config);
        foreach (var row in Other.DDrop.All(config)) Ready(row, config);
        foreach (var row in Other.DKeyTest.All(config)) Ready(row, config);
        foreach (var row in Other.DLoot.All(config)) Ready(row, config);
        foreach (var row in Other.DLootItem.All(config)) Ready(row, config);
        foreach (var row in Other.DMonster.All(config)) Ready(row, config);
        foreach (var row in Other.DSignIn.All(config)) Ready(row, config);
        foreach (var row in Task.DCompleteConditionTypeInfo.All(config)) Ready(row, config);
        foreach (var row in Task.DTask.All(config)) Ready(row, config);
        foreach (var row in Task.DTask2.All(config)) Ready(row, config);
        foreach (var row in Task.DTaskExtraExp.All(config)) Ready(row, config);
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

    private static void Ready(Equip.DJewelryRandom value, ConfigMgr config)
    {
        Ready(value.LvlRank, config);
        Ready(value.AttackRange, config);
        foreach (var item in value.OtherRange) Ready(item, config);
        foreach (var item in value.TestPack) Ready(item, config);
        Ready<Equip.DJewelryRandom>(value, config);
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

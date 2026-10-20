namespace Config;

public static class Processor
{
    private static readonly string Schema = ConfigPackage.SchemaKey(new ConfigReader(Convert.FromBase64String("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aXAuamV3ZWxyeQoAAAABCgAAAAIAAABJRAIAAAAEAAAATmFtZQUAAAAIAAAASWNvbkZpbGUFAAAABwAAAEx2bFJhbmsHAAAACQAAAExldmVsUmFuawUAAABKVHlwZQUAAAAGAAAAU3VpdElEAgAAAAoAAABLZXlBYmlsaXR5AgAAAA8AAABLZXlBYmlsaXR5VmFsdWUCAAAACQAAAFNhbGVQcmljZQIAAAALAAAARGVzY3JpcHRpb24FAAAAFAAAAGVxdWlwLmpld2VscnlfcmFuZG9tCgAAAAEEAAAABwAAAEx2bFJhbmsHAAAACQAAAExldmVsUmFuawsAAABBdHRhY2tSYW5nZQcAAAAFAAAAUmFuZ2UKAAAAT3RoZXJSYW5nZQgAAAAHAAAABQAAAFJhbmdlCAAAAFRlc3RQYWNrCAAAAAcAAAASAAAAZXF1aXAuVGVzdFBhY2tCZWFuEgAAAGVxdWlwLmpld2Vscnlfc3VpdAoAAAABCgAAAAYAAABTdWl0SUQCAAAABQAAAEVuYW1lBQAAAAQAAABOYW1lBgAAAAgAAABBYmlsaXR5MQIAAAANAAAAQWJpbGl0eTFWYWx1ZQIAAAAIAAAAQWJpbGl0eTICAAAADQAAAEFiaWxpdHkyVmFsdWUCAAAACAAAAEFiaWxpdHkzAgAAAA0AAABBYmlsaXR5M1ZhbHVlAgAAAAgAAABTdWl0TGlzdAgAAAACAAAAGAAAAGVxdWlwLmpld2Vscnlfc3VpdF9FbnRyeQwAAAABAQEAAAALAAAAU3BlY2lhbFN1aXQEAAAAEgAAAGVxdWlwLmpld2VscnlfdHlwZQwAAAAAAAQAAAAEAAAASmFkZQgAAABCcmFjZWxldAUAAABNYWdpYwYAAABCb3R0bGUZAAAAZXF1aXAuamV3ZWxyeV90eXBlX0RldGFpbAoAAAABAQAAAAgAAABUeXBlTmFtZQUAAAAKAAAAZXF1aXAucmFuawwAAAAAAQYAAAAFAAAAd2hpdGUAAAAABQAAAGdyZWVuAQAAAAQAAABibHVlAgAAAAYAAABwdXJwbGUDAAAABgAAAHllbGxvdwQAAAADAAAAcmVkBQAAABEAAABlcXVpcC5yYW5rX0RldGFpbAoAAAABAwAAAAYAAABSYW5rSUQCAAAACAAAAFJhbmtOYW1lBQAAAAwAAABSYW5rU2hvd05hbWUFAAAAFAAAAG90aGVyLkFyZ0NhcHR1cmVNb2RlDAAAAAAAAgAAAAgAAABTbmFwc2hvdAcAAABEeW5hbWljGwAAAG90aGVyLkFyZ0NhcHR1cmVNb2RlX0RldGFpbAoAAAABAwAAAAQAAABuYW1lBQAAAAIAAABpZAIAAAAHAAAAY29tbWVudAUAAAAKAAAAb3RoZXIuZHJvcAoAAAABBAAAAAYAAABkcm9waWQCAAAABAAAAG5hbWUGAAAABQAAAGl0ZW1zCAAAAAcAAAAOAAAAb3RoZXIuRHJvcEl0ZW0HAAAAdGVzdG1hcAkAAAACAAAAAgAAAA4AAABvdGhlci5rZXlfdGVzdAoAAAABBgAAAAMAAABpZDECAAAAAwAAAGlkMgMAAAADAAAAaWQzAgAAAAMAAABpZHMIAAAAAgAAAAgAAABlbnVtVGVzdAUAAAAIAAAAZW51bUxpc3QIAAAABQAAAAoAAABvdGhlci5sb290CgAAAAEEAAAABgAAAGxvb3RpZAIAAAAFAAAAZW5hbWUFAAAABAAAAG5hbWUGAAAACgAAAGNoYW5jZUxpc3QIAAAAAgAAAA8AAABvdGhlci5sb290X2l0ZW0KAAAAAQUAAAAGAAAAbG9vdGlkAgAAAAYAAABpdGVtaWQCAAAABgAAAGNoYW5jZQIAAAAIAAAAY291bnRtaW4CAAAACAAAAGNvdW50bWF4AgAAAA0AAABvdGhlci5tb25zdGVyCgAAAAEGAAAAAgAAAGlkAgAAAAcAAABwb3NMaXN0CAAAAAcAAAAIAAAAUG9zaXRpb24GAAAAbG9vdElkAgAAAAoAAABsb290SXRlbUlkAgAAAAgAAABlbnVtTWFwMQkAAAAFAAAAAgAAAAgAAABlbnVtTWFwMgkAAAACAAAABQAAAA0AAABvdGhlci5zaWduX2luCgAAAAEFAAAAAgAAAGlkAgAAAA0AAABpdGVtMmNvdW50TWFwCQAAAAIAAAACAAAAEwAAAHZpcGl0ZW0ydmlwY291bnRNYXAJAAAAAgAAAAIAAAAIAAAAdmlwbGV2ZWwCAAAACAAAAEljb25GaWxlBQAAABwAAAB0YXNrLmNvbXBsZXRlX2NvbmRpdGlvbl90eXBlDAAAAAABBwAAAAsAAABLaWxsTW9uc3RlcgEAAAAHAAAAVGFsa05wYwIAAAALAAAAQ29sbGVjdEl0ZW0DAAAADAAAAENvbmRpdGlvbkFuZAQAAAAEAAAAQ2hhdAUAAAAMAAAAVGVzdE5vQ29sdW1uBgAAAAIAAABhYQcAAAAjAAAAdGFzay5jb21wbGV0ZV9jb25kaXRpb25fdHlwZV9EZXRhaWwKAAAAAQIAAAACAAAAaWQCAAAABAAAAG5hbWUFAAAACQAAAHRhc2sudGFzawoAAAABBgAAAAYAAAB0YXNraWQCAAAABAAAAG5hbWUIAAAABgAAAAgAAABuZXh0dGFzawIAAAARAAAAY29tcGxldGVjb25kaXRpb24HAAAAFgAAAHRhc2suY29tcGxldGVjb25kaXRpb24DAAAAZXhwAgAAAA8AAAB0ZXN0RGVmYXVsdEJlYW4HAAAAFAAAAHRhc2suVGVzdERlZmF1bHRCZWFuCgAAAHRhc2sudGFzazIKAAAAAQsAAAAGAAAAdGFza2lkAgAAAAQAAABuYW1lCAAAAAYAAAAIAAAAbmV4dHRhc2sCAAAAEQAAAGNvbXBsZXRlY29uZGl0aW9uBwAAABYAAAB0YXNrLmNvbXBsZXRlY29uZGl0aW9uAwAAAGV4cAIAAAAIAAAAdGVzdEJvb2wBAAAACgAAAHRlc3RTdHJpbmcFAAAACgAAAHRlc3RTdHJ1Y3QHAAAACAAAAFBvc2l0aW9uCAAAAHRlc3RMaXN0CAAAAAIAAAAOAAAAdGVzdExpc3RTdHJ1Y3QIAAAABwAAAAgAAABQb3NpdGlvbhEAAAB0ZXN0TGlzdEludGVyZmFjZQgAAAAHAAAADgAAAGFpLlRyaWdnZXJUaWNrEwAAAHRhc2sudGFza19leHRyYV9leHAKAAAAAQgAAAAGAAAAdGFza2lkAgAAAAgAAABleHRyYWV4cAIAAAAFAAAAdGVzdDEFAAAABQAAAHRlc3QyBQAAAAYAAABmaWVsZGEFAAAABgAAAGZpZWxkYgUAAAAGAAAAZmllbGRjBQAAAAYAAABmaWVsZGQFAAAA")));
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
            var input = pack.Block(block); input.SetErrors(errors);
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

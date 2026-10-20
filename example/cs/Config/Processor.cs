namespace Config;

public static class Processor
{
    private static readonly string Schema = ConfigPackage.SchemaKey(new ConfigReader(Convert.FromBase64String("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Lmpld2VscnkKAAAAAQoAAAACAAAASUQCAAAABAAAAE5hbWUFAAAACAAAAEljb25GaWxlBQAAAAcAAABMdmxSYW5rBwAAAAkAAABMZXZlbFJhbmsFAAAASlR5cGUFAAAABgAAAFN1aXRJRAIAAAAKAAAAS2V5QWJpbGl0eQIAAAAPAAAAS2V5QWJpbGl0eVZhbHVlAgAAAAkAAABTYWxlUHJpY2UCAAAACwAAAERlc2NyaXB0aW9uBQAAABMAAABlcXVpcC5qZXdlbHJ5cmFuZG9tCgAAAAEEAAAABwAAAEx2bFJhbmsHAAAACQAAAExldmVsUmFuawsAAABBdHRhY2tSYW5nZQcAAAAFAAAAUmFuZ2UKAAAAT3RoZXJSYW5nZQgAAAAHAAAABQAAAFJhbmdlCAAAAFRlc3RQYWNrCAAAAAcAAAASAAAAZXF1aXAuVGVzdFBhY2tCZWFuEQAAAGVxdWlwLmpld2VscnlzdWl0CgAAAAEKAAAABgAAAFN1aXRJRAIAAAAFAAAARW5hbWUFAAAABAAAAE5hbWUGAAAACAAAAEFiaWxpdHkxAgAAAA0AAABBYmlsaXR5MVZhbHVlAgAAAAgAAABBYmlsaXR5MgIAAAANAAAAQWJpbGl0eTJWYWx1ZQIAAAAIAAAAQWJpbGl0eTMCAAAADQAAAEFiaWxpdHkzVmFsdWUCAAAACAAAAFN1aXRMaXN0CAAAAAIAAAAXAAAAZXF1aXAuamV3ZWxyeXN1aXRfRW50cnkMAAAAAQEBAAAACwAAAFNwZWNpYWxTdWl0BAAAABEAAABlcXVpcC5qZXdlbHJ5dHlwZQwAAAAAAAQAAAAEAAAASmFkZQgAAABCcmFjZWxldAUAAABNYWdpYwYAAABCb3R0bGUYAAAAZXF1aXAuamV3ZWxyeXR5cGVfRGV0YWlsCgAAAAEBAAAACAAAAFR5cGVOYW1lBQAAAAoAAABlcXVpcC5yYW5rDAAAAAABBgAAAAUAAAB3aGl0ZQAAAAAFAAAAZ3JlZW4BAAAABAAAAGJsdWUCAAAABgAAAHB1cnBsZQMAAAAGAAAAeWVsbG93BAAAAAMAAAByZWQFAAAAEQAAAGVxdWlwLnJhbmtfRGV0YWlsCgAAAAEDAAAABgAAAFJhbmtJRAIAAAAIAAAAUmFua05hbWUFAAAADAAAAFJhbmtTaG93TmFtZQUAAAAUAAAAb3RoZXIuQXJnQ2FwdHVyZU1vZGUMAAAAAAACAAAACAAAAFNuYXBzaG90BwAAAER5bmFtaWMbAAAAb3RoZXIuQXJnQ2FwdHVyZU1vZGVfRGV0YWlsCgAAAAEDAAAABAAAAG5hbWUFAAAAAgAAAGlkAgAAAAcAAABjb21tZW50BQAAAAoAAABvdGhlci5kcm9wCgAAAAEEAAAABgAAAGRyb3BpZAIAAAAEAAAAbmFtZQYAAAAFAAAAaXRlbXMIAAAABwAAAA4AAABvdGhlci5Ecm9wSXRlbQcAAAB0ZXN0bWFwCQAAAAIAAAACAAAADQAAAG90aGVyLmtleXRlc3QKAAAAAQYAAAADAAAAaWQxAgAAAAMAAABpZDIDAAAAAwAAAGlkMwIAAAADAAAAaWRzCAAAAAIAAAAIAAAAZW51bVRlc3QFAAAACAAAAGVudW1MaXN0CAAAAAUAAAAKAAAAb3RoZXIubG9vdAoAAAABBAAAAAYAAABsb290aWQCAAAABQAAAGVuYW1lBQAAAAQAAABuYW1lBgAAAAoAAABjaGFuY2VMaXN0CAAAAAIAAAAOAAAAb3RoZXIubG9vdGl0ZW0KAAAAAQUAAAAGAAAAbG9vdGlkAgAAAAYAAABpdGVtaWQCAAAABgAAAGNoYW5jZQIAAAAIAAAAY291bnRtaW4CAAAACAAAAGNvdW50bWF4AgAAAA0AAABvdGhlci5tb25zdGVyCgAAAAEGAAAAAgAAAGlkAgAAAAcAAABwb3NMaXN0CAAAAAcAAAAIAAAAUG9zaXRpb24GAAAAbG9vdElkAgAAAAoAAABsb290SXRlbUlkAgAAAAgAAABlbnVtTWFwMQkAAAAFAAAAAgAAAAgAAABlbnVtTWFwMgkAAAACAAAABQAAAAwAAABvdGhlci5zaWduaW4KAAAAAQUAAAACAAAAaWQCAAAADQAAAGl0ZW0yY291bnRNYXAJAAAAAgAAAAIAAAATAAAAdmlwaXRlbTJ2aXBjb3VudE1hcAkAAAACAAAAAgAAAAgAAAB2aXBsZXZlbAIAAAAIAAAASWNvbkZpbGUFAAAAGgAAAHRhc2suY29tcGxldGVjb25kaXRpb250eXBlDAAAAAABBwAAAAsAAABLaWxsTW9uc3RlcgEAAAAHAAAAVGFsa05wYwIAAAALAAAAQ29sbGVjdEl0ZW0DAAAADAAAAENvbmRpdGlvbkFuZAQAAAAEAAAAQ2hhdAUAAAAMAAAAVGVzdE5vQ29sdW1uBgAAAAIAAABhYQcAAAAhAAAAdGFzay5jb21wbGV0ZWNvbmRpdGlvbnR5cGVfRGV0YWlsCgAAAAECAAAAAgAAAGlkAgAAAAQAAABuYW1lBQAAAAkAAAB0YXNrLnRhc2sKAAAAAQYAAAAGAAAAdGFza2lkAgAAAAQAAABuYW1lCAAAAAYAAAAIAAAAbmV4dHRhc2sCAAAAEQAAAGNvbXBsZXRlY29uZGl0aW9uBwAAABYAAAB0YXNrLmNvbXBsZXRlY29uZGl0aW9uAwAAAGV4cAIAAAAPAAAAdGVzdERlZmF1bHRCZWFuBwAAABQAAAB0YXNrLlRlc3REZWZhdWx0QmVhbgoAAAB0YXNrLnRhc2syCgAAAAELAAAABgAAAHRhc2tpZAIAAAAEAAAAbmFtZQgAAAAGAAAACAAAAG5leHR0YXNrAgAAABEAAABjb21wbGV0ZWNvbmRpdGlvbgcAAAAWAAAAdGFzay5jb21wbGV0ZWNvbmRpdGlvbgMAAABleHACAAAACAAAAHRlc3RCb29sAQAAAAoAAAB0ZXN0U3RyaW5nBQAAAAoAAAB0ZXN0U3RydWN0BwAAAAgAAABQb3NpdGlvbggAAAB0ZXN0TGlzdAgAAAACAAAADgAAAHRlc3RMaXN0U3RydWN0CAAAAAcAAAAIAAAAUG9zaXRpb24RAAAAdGVzdExpc3RJbnRlcmZhY2UIAAAABwAAAA4AAABhaS5UcmlnZ2VyVGljaxEAAAB0YXNrLnRhc2tleHRyYWV4cAoAAAABCAAAAAYAAAB0YXNraWQCAAAACAAAAGV4dHJhZXhwAgAAAAUAAAB0ZXN0MQUAAAAFAAAAdGVzdDIFAAAABgAAAGZpZWxkYQUAAAAGAAAAZmllbGRiBQAAAAYAAABmaWVsZGMFAAAABgAAAGZpZWxkZAUAAAA=")));
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

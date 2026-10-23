namespace Config;

public static class Processor
{
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
        var config = new ConfigMgr();
        var loaders = NewLoaders(packages, config, errors);
        foreach (var pack in packages) {
            foreach (string block in pack.BlockNames) {
                if (block.StartsWith(".", StringComparison.Ordinal) && !loaders.ContainsKey(block.Substring(1))) {
                    errors.UnknownTableBlock(pack.Name, block);
                }
            }
        }
        errors.CheckErrors();
        foreach (var load in loaders.Values) {
            load();
        }
        errors.CheckErrors();
        foreach (var row in global::Config.Equip.DJewelry.All(config)) {
            row._resolve(errors, config);
        }
        foreach (var row in global::Config.Equip.DJewelryRandom.All(config)) {
            row._resolve(errors, config);
        }
        foreach (var row in global::Config.Other.DKeyTest.All(config)) {
            row._resolve(errors, config);
        }
        foreach (var row in global::Config.Other.DLoot.All(config)) {
            row._resolve(errors, config);
        }
        foreach (var row in global::Config.Other.DMonster.All(config)) {
            row._resolve(errors, config);
        }
        foreach (var row in global::Config.Other.DSignIn.All(config)) {
            row._resolve(errors, config);
        }
        foreach (var row in global::Config.Task.DTask.All(config)) {
            row._resolve(errors, config);
        }
        foreach (var row in global::Config.Task.DTask2.All(config)) {
            row._resolve(errors, config);
        }
        errors.CheckErrors();
        foreach (var row in global::Config.Ai.DAi.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Ai.DAiAction.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Ai.DAiCondition.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Equip.DAbilityInfo.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Equip.DEquipConfig.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Equip.DJewelry.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Equip.DJewelryRandom.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Equip.DJewelrySuit.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Equip.DJewelryTypeInfo.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Equip.DRankInfo.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Other.DArgCaptureModeInfo.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Other.DDrop.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Other.DKeyTest.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Other.DLoot.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Other.DLootItem.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Other.DMonster.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Other.DSignIn.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Task.DCompleteConditionTypeInfo.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Task.DTask.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Task.DTask2.All(config)) {
            Ready(row, config);
        }
        foreach (var row in global::Config.Task.DTaskExtraExp.All(config)) {
            Ready(row, config);
        }
        config.MarkReady();
        return config;
    }

    private static void Ready<T>(T value, ConfigMgr config)
    {
        if (value is IConfigExtension extension) {
            extension.OnConfigReady(config);
        }
    }

    private static Dictionary<string, Action> NewLoaders(ConfigPackage[] packages, ConfigMgr config, LoadErrors errors)
    {
        return new Dictionary<string, Action>(StringComparer.Ordinal) {
            ["ai.ai"] = () => global::Config.Ai.DAi.InitializeAll(
                ReadTable(packages, "ai.ai", global::Config.Ai.DAi._create,
                    global::Config.Ai.DAi.PrimaryKey, config._ai_dai_Origins, errors), errors, config),
            ["ai.ai_action"] = () => global::Config.Ai.DAiAction.InitializeAll(
                ReadTable(packages, "ai.ai_action", global::Config.Ai.DAiAction._create,
                    global::Config.Ai.DAiAction.PrimaryKey, config._ai_dai_uaction_Origins, errors), errors, config),
            ["ai.ai_condition"] = () => global::Config.Ai.DAiCondition.InitializeAll(
                ReadTable(packages, "ai.ai_condition", global::Config.Ai.DAiCondition._create,
                    global::Config.Ai.DAiCondition.PrimaryKey, config._ai_dai_ucondition_Origins, errors), errors, config),
            ["equip.ability"] = () => global::Config.Equip.DAbilityInfo.InitializeAll(
                ReadTable(packages, "equip.ability", global::Config.Equip.DAbilityInfo._create,
                    global::Config.Equip.DAbilityInfo.PrimaryKey, config._equip_dability_Origins, errors), errors, config),
            ["equip.equip_config"] = () => global::Config.Equip.DEquipConfig.InitializeAll(
                ReadTable(packages, "equip.equip_config", global::Config.Equip.DEquipConfig._create,
                    global::Config.Equip.DEquipConfig.PrimaryKey, config._equip_dequip_uconfig_Origins, errors), errors, config),
            ["equip.jewelry"] = () => global::Config.Equip.DJewelry.InitializeAll(
                ReadTable(packages, "equip.jewelry", global::Config.Equip.DJewelry._create,
                    global::Config.Equip.DJewelry.PrimaryKey, config._equip_djewelry_Origins, errors), errors, config),
            ["equip.jewelry_random"] = () => global::Config.Equip.DJewelryRandom.InitializeAll(
                ReadTable(packages, "equip.jewelry_random", global::Config.Equip.DJewelryRandom._create,
                    global::Config.Equip.DJewelryRandom.PrimaryKey, config._equip_djewelry_urandom_Origins, errors), errors, config),
            ["equip.jewelry_suit"] = () => global::Config.Equip.DJewelrySuit.InitializeAll(
                ReadTable(packages, "equip.jewelry_suit", global::Config.Equip.DJewelrySuit._create,
                    global::Config.Equip.DJewelrySuit.PrimaryKey, config._equip_djewelry_usuit_Origins, errors), errors, config),
            ["equip.jewelry_type"] = () => global::Config.Equip.DJewelryTypeInfo.InitializeAll(
                ReadTable(packages, "equip.jewelry_type", global::Config.Equip.DJewelryTypeInfo._create,
                    global::Config.Equip.DJewelryTypeInfo.PrimaryKey, config._equip_djewelry_utype_Origins, errors), errors, config),
            ["equip.rank"] = () => global::Config.Equip.DRankInfo.InitializeAll(
                ReadTable(packages, "equip.rank", global::Config.Equip.DRankInfo._create,
                    global::Config.Equip.DRankInfo.PrimaryKey, config._equip_drank_Origins, errors), errors, config),
            ["other.ArgCaptureMode"] = () => global::Config.Other.DArgCaptureModeInfo.InitializeAll(
                ReadTable(packages, "other.ArgCaptureMode", global::Config.Other.DArgCaptureModeInfo._create,
                    global::Config.Other.DArgCaptureModeInfo.PrimaryKey, config._other_dArgCaptureMode_Origins, errors), errors, config),
            ["other.drop"] = () => global::Config.Other.DDrop.InitializeAll(
                ReadTable(packages, "other.drop", global::Config.Other.DDrop._create,
                    global::Config.Other.DDrop.PrimaryKey, config._other_ddrop_Origins, errors), errors, config),
            ["other.key_test"] = () => global::Config.Other.DKeyTest.InitializeAll(
                ReadTable(packages, "other.key_test", global::Config.Other.DKeyTest._create,
                    global::Config.Other.DKeyTest.PrimaryKey, config._other_dkey_utest_Origins, errors), errors, config),
            ["other.loot"] = () => global::Config.Other.DLoot.InitializeAll(
                ReadTable(packages, "other.loot", global::Config.Other.DLoot._create,
                    global::Config.Other.DLoot.PrimaryKey, config._other_dloot_Origins, errors), errors, config),
            ["other.loot_item"] = () => global::Config.Other.DLootItem.InitializeAll(
                ReadTable(packages, "other.loot_item", global::Config.Other.DLootItem._create,
                    global::Config.Other.DLootItem.PrimaryKey, config._other_dloot_uitem_Origins, errors), errors, config),
            ["other.monster"] = () => global::Config.Other.DMonster.InitializeAll(
                ReadTable(packages, "other.monster", global::Config.Other.DMonster._create,
                    global::Config.Other.DMonster.PrimaryKey, config._other_dmonster_Origins, errors), errors, config),
            ["other.sign_in"] = () => global::Config.Other.DSignIn.InitializeAll(
                ReadTable(packages, "other.sign_in", global::Config.Other.DSignIn._create,
                    global::Config.Other.DSignIn.PrimaryKey, config._other_dsign_uin_Origins, errors), errors, config),
            ["task.complete_condition_type"] = () => global::Config.Task.DCompleteConditionTypeInfo.InitializeAll(
                ReadTable(packages, "task.complete_condition_type", global::Config.Task.DCompleteConditionTypeInfo._create,
                    global::Config.Task.DCompleteConditionTypeInfo.PrimaryKey, config._task_dcomplete_ucondition_utype_Origins, errors), errors, config),
            ["task.task"] = () => global::Config.Task.DTask.InitializeAll(
                ReadTable(packages, "task.task", global::Config.Task.DTask._create,
                    global::Config.Task.DTask.PrimaryKey, config._task_dtask_Origins, errors), errors, config),
            ["task.task2"] = () => global::Config.Task.DTask2.InitializeAll(
                ReadTable(packages, "task.task2", global::Config.Task.DTask2._create,
                    global::Config.Task.DTask2.PrimaryKey, config._task_dtask2_Origins, errors), errors, config),
            ["task.task_extra_exp"] = () => global::Config.Task.DTaskExtraExp.InitializeAll(
                ReadTable(packages, "task.task_extra_exp", global::Config.Task.DTaskExtraExp._create,
                    global::Config.Task.DTaskExtraExp.PrimaryKey, config._task_dtask_uextra_uexp_Origins, errors), errors, config),
        };
    }


    private static void Ready(global::Config.Ai.DAi value, ConfigMgr config)
    {
        Ready(value.TrigTick, config);
        Ready<global::Config.Ai.DAi>(value, config);
    }

    private static void Ready(global::Config.Equip.DTestPackBean value, ConfigMgr config)
    {
        Ready(value.IRange, config);
        Ready<global::Config.Equip.DTestPackBean>(value, config);
    }

    private static void Ready(global::Config.Equip.DJewelry value, ConfigMgr config)
    {
        Ready(value.LvlRank, config);
        Ready<global::Config.Equip.DJewelry>(value, config);
    }

    private static void Ready(global::Config.Equip.DJewelryRandom value, ConfigMgr config)
    {
        Ready(value.LvlRank, config);
        Ready(value.AttackRange, config);
        foreach (var item in value.OtherRange) {
            Ready(item, config);
        }
        foreach (var item in value.TestPack) {
            Ready(item, config);
        }
        Ready<global::Config.Equip.DJewelryRandom>(value, config);
    }

    private static void Ready(global::Config.Other.DDrop value, ConfigMgr config)
    {
        foreach (var item in value.Items) {
            Ready(item, config);
        }
        Ready<global::Config.Other.DDrop>(value, config);
    }

    private static void Ready(global::Config.Other.DMonster value, ConfigMgr config)
    {
        foreach (var item in value.PosList) {
            Ready(item, config);
        }
        Ready<global::Config.Other.DMonster>(value, config);
    }

    private static void Ready(global::Config.Task.DTestDefaultBean value, ConfigMgr config)
    {
        Ready(value.TestSubBean, config);
        Ready<global::Config.Task.DTestDefaultBean>(value, config);
    }

    private static void Ready(global::Config.Task.Completecondition.DConditionAnd value, ConfigMgr config)
    {
        Ready(value.Cond1, config);
        Ready(value.Cond2, config);
        Ready<global::Config.Task.Completecondition.DConditionAnd>(value, config);
    }

    private static void Ready(global::Config.Task.DTask value, ConfigMgr config)
    {
        Ready(value.Completecondition, config);
        Ready(value.TestDefaultBean, config);
        Ready<global::Config.Task.DTask>(value, config);
    }

    private static void Ready(global::Config.Task.DTask2 value, ConfigMgr config)
    {
        Ready(value.Completecondition, config);
        Ready(value.TestStruct, config);
        foreach (var item in value.TestListStruct) {
            Ready(item, config);
        }
        foreach (var item in value.TestListInterface) {
            Ready(item, config);
        }
        Ready<global::Config.Task.DTask2>(value, config);
    }

    private static void Ready(global::Config.Task.DCompletecondition value, ConfigMgr config)
    {
        if (value is global::Config.Task.Completecondition.DConditionAnd item) {
            Ready(item, config);
        }
        else {
            Ready<global::Config.Task.DCompletecondition>(value, config);
        }
    }

    private static List<T> ReadTable<K, T>(ConfigPackage[] packages, string table,
        Func<ConfigReader, T> readRow, Func<T, K> primaryKey, Dictionary<K, List<string>> origins,
        LoadErrors errors) where K : notnull
    {
        var rows = new List<T>();
        var slots = new Dictionary<K, int>();
        string block = "." + table;
        foreach (var pack in packages) {
            if (!pack.HasBlock(block)) {
                continue;
            }
            bool isBase = ReferenceEquals(pack, packages[0]);
            string packName = pack.Name;
            var input = pack.Block(block); input.SetErrors(errors);
            int count = input.ReadCount();
            for (int j = 0; j < count; j++) {
                var row = readRow(input);
                K key = primaryKey(row);
                bool exists = slots.TryGetValue(key, out var slot);
                bool duplicate = exists;
                if (!isBase) {
                    if (!origins.TryGetValue(key, out var chain)) {
                        chain = new List<string>();
                        origins.Add(key, chain);
                    }
                    duplicate = chain.Count != 0 && chain[chain.Count - 1] == packName;
                    if (!duplicate) {
                        chain.Add(packName);
                    }
                }
                if (duplicate) {
                    errors.DuplicateRecord(table, packName, key);
                    continue;
                }
                if (exists) {
                    rows[slot] = row;
                } else {
                    slots.Add(key, rows.Count);
                    rows.Add(row);
                }
            }
            input.RequireEnd();
        }
        return rows;
    }
}

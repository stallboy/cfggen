package config;

import configgen.genjava.ConfigLoader;
import configgen.genjava.ConfigPackage;
import configgen.genjava.LoadValueErrs;
import java.util.*;

public class ConfigMgrLoader {
    public static ConfigMgr loadPackagesAndSetMgr(ConfigPackage... packages) {
        ConfigMgr config = loadPackages(packages);
        ConfigMgr.setMgr(config);
        return config;
    }

    public static ConfigMgr loadPackages(ConfigPackage... packages) {
        ConfigPackage.validateOrder(List.of(packages));
        var errors = new LoadValueErrs();
        ConfigMgr mgr = new ConfigMgr();
        var loaders = newLoaders(mgr, errors);
        var expected = ConfigCodeSchema.getPackageSchema();
        for (var pack : packages) {
            String packName = pack.name();
            if (!pack.validateSchema(expected, errors)) continue;
            for (String block : pack.blockNames()) {
                if (!block.startsWith(".")) continue;
                String table = block.substring(1);
                var loader = loaders.get(table);
                if (loader != null) loader.read(pack.block(block), packName);
            }
        }
        return finish(mgr, loaders, errors);
    }

    private static ConfigMgr finish(ConfigMgr mgr, Map<String, ConfigLoader<?, ?>> loaders, LoadValueErrs errors) {
        for (var entry : loaders.entrySet()) {
            mgr.setOrigins(entry.getKey(), entry.getValue().origins());
            entry.getValue().index();
        }
        errors.checkErrors();
        config.equip.EquipConfig_Entry.setAllRefs(mgr, errors);
        config.equip.JewelrySuit_Entry.setAllRefs(mgr, errors);
        config.equip.Rank.setAllRefs(mgr, errors);
        config.other.ArgCaptureMode.setAllRefs(mgr, errors);
        for (var loader : loaders.values()) loader.resolveAll();
        errors.checkErrors();
        mgr.markReady();
        return mgr;
    }

    private static Map<String, ConfigLoader<?, ?>> newLoaders(ConfigMgr mgr, LoadValueErrs errors) {
        var loaders = new LinkedHashMap<String, ConfigLoader<?, ?>>();
        loaders.put("ai.ai", new ConfigLoader<>("ai.ai", errors, config.ai.Ai::_create,
                config.ai.Ai::getID, mgr::_set_ai_ai_All, null));
        loaders.put("ai.ai_action", new ConfigLoader<>("ai.ai_action", errors, config.ai.AiAction::_create,
                config.ai.AiAction::getID, mgr::_set_ai_ai_action_All, null));
        loaders.put("ai.ai_condition", new ConfigLoader<>("ai.ai_condition", errors, config.ai.AiCondition::_create,
                config.ai.AiCondition::getID, mgr::_set_ai_ai_condition_All, null));
        loaders.put("equip.equip_config", new ConfigLoader<>("equip.equip_config", errors, config.equip.EquipConfig::_create,
                config.equip.EquipConfig::getEntry, mgr::_set_equip_equip_config_All, null));
        loaders.put("equip.jewelry", new ConfigLoader<>("equip.jewelry", errors, config.equip.Jewelry::_create,
                config.equip.Jewelry::getID, mgr::_set_equip_jewelry_All, row -> row._resolve(mgr, errors)));
        loaders.put("equip.jewelry_random", new ConfigLoader<>("equip.jewelry_random", errors, config.equip.JewelryRandom::_create,
                config.equip.JewelryRandom::getLvlRank, mgr::_set_equip_jewelry_random_All, row -> row._resolve(mgr, errors)));
        loaders.put("equip.jewelry_suit", new ConfigLoader<>("equip.jewelry_suit", errors, config.equip.JewelrySuit::_create,
                config.equip.JewelrySuit::getSuitID, mgr::_set_equip_jewelry_suit_All, null));
        loaders.put("equip.rank", new ConfigLoader<>("equip.rank", errors, config.equip.Rank_Detail::_create,
                config.equip.Rank_Detail::getRankID, rows -> config.equip.Rank_Detail._index(mgr, rows, errors), null));
        loaders.put("other.ArgCaptureMode", new ConfigLoader<>("other.ArgCaptureMode", errors, config.other.ArgCaptureMode_Detail::_create,
                config.other.ArgCaptureMode_Detail::getName, rows -> config.other.ArgCaptureMode_Detail._index(mgr, rows, errors), null));
        loaders.put("other.drop", new ConfigLoader<>("other.drop", errors, config.other.Drop::_create,
                config.other.Drop::getDropid, mgr::_set_other_drop_All, null));
        loaders.put("other.key_test", new ConfigLoader<>("other.key_test", errors, config.other.KeyTest::_create,
                row -> new config.other.KeyTest.Id1Id2Key(row.getId1(), row.getId2()), rows -> config.other.KeyTest._index(mgr, rows, errors), row -> row._resolve(mgr, errors)));
        loaders.put("other.loot", new ConfigLoader<>("other.loot", errors, config.other.Loot::_create,
                config.other.Loot::getLootid, mgr::_set_other_loot_All, row -> row._resolve(mgr, errors)));
        loaders.put("other.loot_item", new ConfigLoader<>("other.loot_item", errors, config.other.LootItem::_create,
                row -> new config.other.LootItem.LootidItemidKey(row.getLootid(), row.getItemid()), mgr::_set_other_loot_item_All, null));
        loaders.put("other.monster", new ConfigLoader<>("other.monster", errors, config.other.Monster::_create,
                config.other.Monster::getId, mgr::_set_other_monster_All, row -> row._resolve(mgr, errors)));
        loaders.put("other.sign_in", new ConfigLoader<>("other.sign_in", errors, config.other.SignIn::_create,
                config.other.SignIn::getId, mgr::_set_other_sign_in_All, row -> row._resolve(mgr, errors)));
        loaders.put("task.task", new ConfigLoader<>("task.task", errors, config.task.Task::_create,
                config.task.Task::getTaskid, mgr::_set_task_task_All, row -> row._resolve(mgr, errors)));
        loaders.put("task.task2", new ConfigLoader<>("task.task2", errors, config.task.Task2::_create,
                config.task.Task2::getTaskid, mgr::_set_task_task2_All, row -> row._resolve(mgr, errors)));
        loaders.put("task.task_extra_exp", new ConfigLoader<>("task.task_extra_exp", errors, config.task.TaskExtraExp::_create,
                config.task.TaskExtraExp::getTaskid, mgr::_set_task_task_extra_exp_All, null));
        return loaders;
    }
}

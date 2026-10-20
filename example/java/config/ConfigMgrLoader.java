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
        config.equip.Equipconfig_Entry.setAllRefs(mgr, errors);
        config.equip.Jewelrysuit_Entry.setAllRefs(mgr, errors);
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
        loaders.put("equip.equipconfig", new ConfigLoader<>("equip.equipconfig", errors, config.equip.Equipconfig::_create,
                config.equip.Equipconfig::getEntry, mgr::_set_equip_equipconfig_All, null));
        loaders.put("equip.jewelry", new ConfigLoader<>("equip.jewelry", errors, config.equip.Jewelry::_create,
                config.equip.Jewelry::getID, mgr::_set_equip_jewelry_All, row -> row._resolve(mgr, errors)));
        loaders.put("equip.jewelryrandom", new ConfigLoader<>("equip.jewelryrandom", errors, config.equip.Jewelryrandom::_create,
                config.equip.Jewelryrandom::getLvlRank, mgr::_set_equip_jewelryrandom_All, row -> row._resolve(mgr, errors)));
        loaders.put("equip.jewelrysuit", new ConfigLoader<>("equip.jewelrysuit", errors, config.equip.Jewelrysuit::_create,
                config.equip.Jewelrysuit::getSuitID, mgr::_set_equip_jewelrysuit_All, null));
        loaders.put("equip.rank", new ConfigLoader<>("equip.rank", errors, config.equip.Rank_Detail::_create,
                config.equip.Rank_Detail::getRankID, rows -> config.equip.Rank_Detail._index(mgr, rows, errors), null));
        loaders.put("other.ArgCaptureMode", new ConfigLoader<>("other.ArgCaptureMode", errors, config.other.ArgCaptureMode_Detail::_create,
                config.other.ArgCaptureMode_Detail::getName, rows -> config.other.ArgCaptureMode_Detail._index(mgr, rows, errors), null));
        loaders.put("other.drop", new ConfigLoader<>("other.drop", errors, config.other.Drop::_create,
                config.other.Drop::getDropid, mgr::_set_other_drop_All, null));
        loaders.put("other.keytest", new ConfigLoader<>("other.keytest", errors, config.other.Keytest::_create,
                row -> new config.other.Keytest.Id1Id2Key(row.getId1(), row.getId2()), rows -> config.other.Keytest._index(mgr, rows, errors), row -> row._resolve(mgr, errors)));
        loaders.put("other.loot", new ConfigLoader<>("other.loot", errors, config.other.Loot::_create,
                config.other.Loot::getLootid, mgr::_set_other_loot_All, row -> row._resolve(mgr, errors)));
        loaders.put("other.lootitem", new ConfigLoader<>("other.lootitem", errors, config.other.Lootitem::_create,
                row -> new config.other.Lootitem.LootidItemidKey(row.getLootid(), row.getItemid()), mgr::_set_other_lootitem_All, null));
        loaders.put("other.monster", new ConfigLoader<>("other.monster", errors, config.other.Monster::_create,
                config.other.Monster::getId, mgr::_set_other_monster_All, row -> row._resolve(mgr, errors)));
        loaders.put("other.signin", new ConfigLoader<>("other.signin", errors, config.other.Signin::_create,
                config.other.Signin::getId, mgr::_set_other_signin_All, row -> row._resolve(mgr, errors)));
        loaders.put("task.task", new ConfigLoader<>("task.task", errors, config.task.Task::_create,
                config.task.Task::getTaskid, mgr::_set_task_task_All, row -> row._resolve(mgr, errors)));
        loaders.put("task.task2", new ConfigLoader<>("task.task2", errors, config.task.Task2::_create,
                config.task.Task2::getTaskid, mgr::_set_task_task2_All, row -> row._resolve(mgr, errors)));
        loaders.put("task.taskextraexp", new ConfigLoader<>("task.taskextraexp", errors, config.task.Taskextraexp::_create,
                config.task.Taskextraexp::getTaskid, mgr::_set_task_taskextraexp_All, null));
        return loaders;
    }
}

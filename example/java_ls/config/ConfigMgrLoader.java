package config;

import configgen.genjava.ConfigLoader;
import configgen.genjava.ConfigPackage;
import configgen.genjava.LoadValueErrs;
import java.util.*;

public class ConfigMgrLoader {
    private static final String[] LANGUAGES = { "zh_cn", "en", "tw" };
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
            if (!pack.validateSchema(expected, errors)) {
                continue;
            }
            for (String block : pack.blockNames()) {
                if (!block.startsWith(".")) {
                    continue;
                }
                String table = block.substring(1);
                var loader = loaders.get(table);
                if (loader != null) {
                    loader.read(pack.block(block, LANGUAGES), packName, pack == packages[0]);
                }
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
        config.equip.CfgEquip_config_Entry.setAllRefs(mgr, errors);
        config.equip.CfgJewelry_suit_Entry.setAllRefs(mgr, errors);
        config.equip.CfgRank.setAllRefs(mgr, errors);
        config.other.CfgArgCaptureMode.setAllRefs(mgr, errors);
        for (var loader : loaders.values()) {
            loader.resolveAll();
        }
        errors.checkErrors();
        mgr.markReady();
        return mgr;
    }

    private static Map<String, ConfigLoader<?, ?>> newLoaders(ConfigMgr mgr, LoadValueErrs errors) {
        var loaders = new LinkedHashMap<String, ConfigLoader<?, ?>>();
        loaders.put("ai.ai", new ConfigLoader<>("ai.ai", errors, config.ai.CfgAi::_create,
                config.ai.CfgAi::getID, mgr::_set_ai_ai_All, null));
        loaders.put("ai.ai_action", new ConfigLoader<>("ai.ai_action", errors, config.ai.CfgAi_action::_create,
                config.ai.CfgAi_action::getID, mgr::_set_ai_ai_action_All, null));
        loaders.put("ai.ai_condition", new ConfigLoader<>("ai.ai_condition", errors, config.ai.CfgAi_condition::_create,
                config.ai.CfgAi_condition::getID, mgr::_set_ai_ai_condition_All, null));
        loaders.put("equip.equip_config", new ConfigLoader<>("equip.equip_config", errors, config.equip.CfgEquip_config::_create,
                config.equip.CfgEquip_config::getEntry, mgr::_set_equip_equip_config_All, null));
        loaders.put("equip.jewelry", new ConfigLoader<>("equip.jewelry", errors, config.equip.CfgJewelry::_create,
                config.equip.CfgJewelry::getID, mgr::_set_equip_jewelry_All, row -> row._resolve(mgr, errors)));
        loaders.put("equip.jewelry_random", new ConfigLoader<>("equip.jewelry_random", errors, config.equip.CfgJewelry_random::_create,
                config.equip.CfgJewelry_random::getLvlRank, mgr::_set_equip_jewelry_random_All, row -> row._resolve(mgr, errors)));
        loaders.put("equip.jewelry_suit", new ConfigLoader<>("equip.jewelry_suit", errors, config.equip.CfgJewelry_suit::_create,
                config.equip.CfgJewelry_suit::getSuitID, mgr::_set_equip_jewelry_suit_All, null));
        loaders.put("equip.rank", new ConfigLoader<>("equip.rank", errors, config.equip.CfgRank_Detail::_create,
                config.equip.CfgRank_Detail::getRankID, rows -> config.equip.CfgRank_Detail._index(mgr, rows, errors), null));
        loaders.put("other.ArgCaptureMode", new ConfigLoader<>("other.ArgCaptureMode", errors, config.other.CfgArgCaptureMode_Detail::_create,
                config.other.CfgArgCaptureMode_Detail::getName, rows -> config.other.CfgArgCaptureMode_Detail._index(mgr, rows, errors), null));
        loaders.put("other.drop", new ConfigLoader<>("other.drop", errors, config.other.CfgDrop::_create,
                config.other.CfgDrop::getDropid, mgr::_set_other_drop_All, null));
        loaders.put("other.key_test", new ConfigLoader<>("other.key_test", errors, config.other.CfgKey_test::_create,
                row -> new config.other.CfgKey_test.Id1Id2Key(row.getId1(), row.getId2()), rows -> config.other.CfgKey_test._index(mgr, rows, errors), row -> row._resolve(mgr, errors)));
        loaders.put("other.loot", new ConfigLoader<>("other.loot", errors, config.other.CfgLoot::_create,
                config.other.CfgLoot::getLootid, mgr::_set_other_loot_All, row -> row._resolve(mgr, errors)));
        loaders.put("other.loot_item", new ConfigLoader<>("other.loot_item", errors, config.other.CfgLoot_item::_create,
                row -> new config.other.CfgLoot_item.LootidItemidKey(row.getLootid(), row.getItemid()), mgr::_set_other_loot_item_All, null));
        loaders.put("other.monster", new ConfigLoader<>("other.monster", errors, config.other.CfgMonster::_create,
                config.other.CfgMonster::getId, mgr::_set_other_monster_All, row -> row._resolve(mgr, errors)));
        loaders.put("other.sign_in", new ConfigLoader<>("other.sign_in", errors, config.other.CfgSign_in::_create,
                config.other.CfgSign_in::getId, mgr::_set_other_sign_in_All, row -> row._resolve(mgr, errors)));
        loaders.put("task.task", new ConfigLoader<>("task.task", errors, config.task.CfgTask::_create,
                config.task.CfgTask::getTaskid, mgr::_set_task_task_All, row -> row._resolve(mgr, errors)));
        loaders.put("task.task2", new ConfigLoader<>("task.task2", errors, config.task.CfgTask2::_create,
                config.task.CfgTask2::getTaskid, mgr::_set_task_task2_All, row -> row._resolve(mgr, errors)));
        loaders.put("task.task_extra_exp", new ConfigLoader<>("task.task_extra_exp", errors, config.task.CfgTask_extra_exp::_create,
                config.task.CfgTask_extra_exp::getTaskid, mgr::_set_task_task_extra_exp_All, null));
        return loaders;
    }
}

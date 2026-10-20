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
            if (!pack.validateSchema(expected, errors)) continue;
            for (String block : pack.blockNames()) {
                if (!block.startsWith(".")) continue;
                String table = block.substring(1);
                var loader = loaders.get(table);
                if (loader != null) loader.read(pack.block(block, LANGUAGES), packName);
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
        config.equip.CfgEquipconfig_Entry.setAllRefs(mgr, errors);
        config.equip.CfgJewelrysuit_Entry.setAllRefs(mgr, errors);
        config.equip.CfgRank.setAllRefs(mgr, errors);
        config.other.CfgArgCaptureMode.setAllRefs(mgr, errors);
        for (var loader : loaders.values()) loader.resolveAll();
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
        loaders.put("equip.equipconfig", new ConfigLoader<>("equip.equipconfig", errors, config.equip.CfgEquipconfig::_create,
                config.equip.CfgEquipconfig::getEntry, mgr::_set_equip_equipconfig_All, null));
        loaders.put("equip.jewelry", new ConfigLoader<>("equip.jewelry", errors, config.equip.CfgJewelry::_create,
                config.equip.CfgJewelry::getID, mgr::_set_equip_jewelry_All, row -> row._resolve(mgr, errors)));
        loaders.put("equip.jewelryrandom", new ConfigLoader<>("equip.jewelryrandom", errors, config.equip.CfgJewelryrandom::_create,
                config.equip.CfgJewelryrandom::getLvlRank, mgr::_set_equip_jewelryrandom_All, row -> row._resolve(mgr, errors)));
        loaders.put("equip.jewelrysuit", new ConfigLoader<>("equip.jewelrysuit", errors, config.equip.CfgJewelrysuit::_create,
                config.equip.CfgJewelrysuit::getSuitID, mgr::_set_equip_jewelrysuit_All, null));
        loaders.put("equip.rank", new ConfigLoader<>("equip.rank", errors, config.equip.CfgRank_Detail::_create,
                config.equip.CfgRank_Detail::getRankID, rows -> config.equip.CfgRank_Detail._index(mgr, rows, errors), null));
        loaders.put("other.ArgCaptureMode", new ConfigLoader<>("other.ArgCaptureMode", errors, config.other.CfgArgCaptureMode_Detail::_create,
                config.other.CfgArgCaptureMode_Detail::getName, rows -> config.other.CfgArgCaptureMode_Detail._index(mgr, rows, errors), null));
        loaders.put("other.drop", new ConfigLoader<>("other.drop", errors, config.other.CfgDrop::_create,
                config.other.CfgDrop::getDropid, mgr::_set_other_drop_All, null));
        loaders.put("other.keytest", new ConfigLoader<>("other.keytest", errors, config.other.CfgKeytest::_create,
                row -> new config.other.CfgKeytest.Id1Id2Key(row.getId1(), row.getId2()), rows -> config.other.CfgKeytest._index(mgr, rows, errors), row -> row._resolve(mgr, errors)));
        loaders.put("other.loot", new ConfigLoader<>("other.loot", errors, config.other.CfgLoot::_create,
                config.other.CfgLoot::getLootid, mgr::_set_other_loot_All, row -> row._resolve(mgr, errors)));
        loaders.put("other.lootitem", new ConfigLoader<>("other.lootitem", errors, config.other.CfgLootitem::_create,
                row -> new config.other.CfgLootitem.LootidItemidKey(row.getLootid(), row.getItemid()), mgr::_set_other_lootitem_All, null));
        loaders.put("other.monster", new ConfigLoader<>("other.monster", errors, config.other.CfgMonster::_create,
                config.other.CfgMonster::getId, mgr::_set_other_monster_All, row -> row._resolve(mgr, errors)));
        loaders.put("other.signin", new ConfigLoader<>("other.signin", errors, config.other.CfgSignin::_create,
                config.other.CfgSignin::getId, mgr::_set_other_signin_All, row -> row._resolve(mgr, errors)));
        loaders.put("task.task", new ConfigLoader<>("task.task", errors, config.task.CfgTask::_create,
                config.task.CfgTask::getTaskid, mgr::_set_task_task_All, row -> row._resolve(mgr, errors)));
        loaders.put("task.task2", new ConfigLoader<>("task.task2", errors, config.task.CfgTask2::_create,
                config.task.CfgTask2::getTaskid, mgr::_set_task_task2_All, row -> row._resolve(mgr, errors)));
        loaders.put("task.taskextraexp", new ConfigLoader<>("task.taskextraexp", errors, config.task.CfgTaskextraexp::_create,
                config.task.CfgTaskextraexp::getTaskid, mgr::_set_task_taskextraexp_All, null));
        return loaders;
    }
}

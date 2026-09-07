package config;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ConfigMgrLoader {

    public static configgen.genjava.SchemaInterface loadSchema(configgen.genjava.ConfigInput input) {
        // 1. 读取 Schema（如果有）
        int schemaLength = input.readInt();
        if (schemaLength <= 0) {
            return null;
        } else {
            return (configgen.genjava.SchemaInterface) configgen.genjava.SchemaDeserializer.deserialize(input);
        }
    }

    public static ConfigMgr load(configgen.genjava.ConfigInput input) {
        ConfigMgr mgr = new ConfigMgr();
        return load(mgr, input);
    }

    public static ConfigMgr load(ConfigMgr mgr, configgen.genjava.ConfigInput input) {
        // 2. 初始化 StringPool 和 LangTextPool
        input.readStringPool();
        input.readLangTextPool();

        // 3. 读取表数据
        int c = input.readInt();
        if (c < 18) {
            throw new IllegalArgumentException();
        }

        Map<String, ConfigLoader> allConfigLoaders = getAllConfigLoaders();
        for (int i = 0; i < c; i++) {
            String tableName = input.readString();
            int tableSize = input.readInt();
            ConfigLoader configLoader = allConfigLoaders.get(tableName);
            if (configLoader != null) {
                configLoader.createAll(mgr, input);
            } else {
                input.skipBytes(tableSize);
            }
        }

        for (var configLoader : allConfigLoaders.values()) {
            configLoader.resolveAll(mgr);
        }

        return mgr;
    }

    public static void loadPartialAndSetMgr(configgen.genjava.ConfigInput input, ConfigMgr oldMgr, Set<String> tableNames) {
        ConfigMgr newMgr = new ConfigMgr();
        newMgr.copyFrom(oldMgr);

        input.readStringPool();
        input.readLangTextPool();

        int c = input.readInt();

        Map<String, ConfigLoader> allConfigLoaders = getAllConfigLoaders();
        for (int i = 0; i < c; i++) {
            String tableName = input.readString();
            int tableSize = input.readInt();
            if (tableNames.contains(tableName)) {
                ConfigLoader configLoader = allConfigLoaders.get(tableName);
                if (configLoader != null) {
                    configLoader.createAll(newMgr, input);
                } else {
                    input.skipBytes(tableSize);
                }
            } else {
                input.skipBytes(tableSize);
            }
        }

        for (var configLoader : allConfigLoaders.values()) {
            configLoader.resolveAll(newMgr);
        }

        ConfigMgr.setMgr(newMgr);
    }

    public static void applySetAllRefs(ConfigMgr mgr) {
        config.equip.CfgEquipconfig_Entry.setAllRefs(mgr);
        config.equip.CfgJewelrysuit_Entry.setAllRefs(mgr);
        config.equip.CfgRank.setAllRefs(mgr);
        config.other.CfgArgCaptureMode.setAllRefs(mgr);
    }

    public static Map<String, ConfigLoader> getAllConfigLoaders() {
        Map<String, ConfigLoader> allConfigLoaders = new LinkedHashMap<>();
        allConfigLoaders.put("ai.ai", new config.ai.CfgAi._ConfigLoader());
        allConfigLoaders.put("ai.ai_action", new config.ai.CfgAi_action._ConfigLoader());
        allConfigLoaders.put("ai.ai_condition", new config.ai.CfgAi_condition._ConfigLoader());
        allConfigLoaders.put("equip.equipconfig", new config.equip.CfgEquipconfig._ConfigLoader());
        allConfigLoaders.put("equip.jewelry", new config.equip.CfgJewelry._ConfigLoader());
        allConfigLoaders.put("equip.jewelryrandom", new config.equip.CfgJewelryrandom._ConfigLoader());
        allConfigLoaders.put("equip.jewelrysuit", new config.equip.CfgJewelrysuit._ConfigLoader());
        allConfigLoaders.put("equip.rank", new config.equip.CfgRank_Detail._ConfigLoader());
        allConfigLoaders.put("other.ArgCaptureMode", new config.other.CfgArgCaptureMode_Detail._ConfigLoader());
        allConfigLoaders.put("other.drop", new config.other.CfgDrop._ConfigLoader());
        allConfigLoaders.put("other.keytest", new config.other.CfgKeytest._ConfigLoader());
        allConfigLoaders.put("other.loot", new config.other.CfgLoot._ConfigLoader());
        allConfigLoaders.put("other.lootitem", new config.other.CfgLootitem._ConfigLoader());
        allConfigLoaders.put("other.monster", new config.other.CfgMonster._ConfigLoader());
        allConfigLoaders.put("other.signin", new config.other.CfgSignin._ConfigLoader());
        allConfigLoaders.put("task.task", new config.task.CfgTask._ConfigLoader());
        allConfigLoaders.put("task.task2", new config.task.CfgTask2._ConfigLoader());
        allConfigLoaders.put("task.taskextraexp", new config.task.CfgTaskextraexp._ConfigLoader());
        return allConfigLoaders;
    }
}

package config;

import java.util.LinkedHashMap;
import java.util.Map;

public class ConfigMgrLoader {

    public static ConfigMgr load(configgen.genjava.ConfigInput input) {
        ConfigMgr mgr = new ConfigMgr();
        return load(mgr, input);
    }

    public static ConfigMgr load(ConfigMgr mgr, configgen.genjava.ConfigInput input) {
        int c = input.readInt();
        if (c < 15) {
            throw new IllegalArgumentException();
        }

        Map<String, ConfigLoader> allConfigLoaders = getAllConfigLoaders();
        for (int i = 0; i < c; i++) {
            String tableName = input.readStr();
            int tableSize = input.readInt();
            ConfigLoader configLoader = allConfigLoaders.get(tableName);
            if (configLoader != null) {
                configLoader.createAll(mgr, input);
            } else {
                input.skipBytes(tableSize);
            }
        }

        for (ConfigLoader configLoader : allConfigLoaders.values()) {
            configLoader.resolveAll(mgr);
        }

        return mgr;
    }

    private static Map<String, ConfigLoader> getAllConfigLoaders() {
        Map<String, ConfigLoader> allConfigLoaders = new LinkedHashMap<>();
        allConfigLoaders.put("ai.ai", new config.ai.Ai._ConfigLoader());
        allConfigLoaders.put("ai.ai_action", new config.ai.Ai_action._ConfigLoader());
        allConfigLoaders.put("ai.ai_condition", new config.ai.Ai_condition._ConfigLoader());
        allConfigLoaders.put("equip.equipconfig", new config.equip.Equipconfig._ConfigLoader());
        allConfigLoaders.put("equip.jewelry", new config.equip.Jewelry._ConfigLoader());
        allConfigLoaders.put("equip.jewelryrandom", new config.equip.Jewelryrandom._ConfigLoader());
        allConfigLoaders.put("equip.jewelrysuit", new config.equip.Jewelrysuit._ConfigLoader());
        allConfigLoaders.put("equip.rank", new config.equip.Rank_Detail._ConfigLoader());
        allConfigLoaders.put("other.drop", new config.other.Drop._ConfigLoader());
        allConfigLoaders.put("other.loot", new config.other.Loot._ConfigLoader());
        allConfigLoaders.put("other.lootitem", new config.other.Lootitem._ConfigLoader());
        allConfigLoaders.put("other.monster", new config.other.Monster._ConfigLoader());
        allConfigLoaders.put("other.signin", new config.other.Signin._ConfigLoader());
        allConfigLoaders.put("task.task", new config.task.Task._ConfigLoader());
        allConfigLoaders.put("task.taskextraexp", new config.task.Taskextraexp._ConfigLoader());

        return allConfigLoaders;
    }
}

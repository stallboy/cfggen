package config.equip;

import configgen.genjava.LoadValueErrs;

// 来自：equip/equip_config.csv
public class EquipConfig_Entry {
    public static final EquipConfig_Entry INSTANCE = new EquipConfig_Entry(0, "Instance");
    public static final EquipConfig_Entry INSTANCE2 = new EquipConfig_Entry(1, "Instance2");

    private final String value;


    EquipConfig_Entry(int index, String value) {
        this.index = index;
        this.value = value;
    }

    private final int index;

    public config.equip.EquipConfig ref() {
        return ref(config.ConfigMgr.getMgr());
    }

    public config.equip.EquipConfig ref(config.ConfigMgr mgr) {
        return mgr._get_equip_equip_config_Refs(index);
    }

    public static void setAllRefs(config.ConfigMgr mgr, LoadValueErrs errors) {
        mgr._set_equip_equip_config_Refs(new config.equip.EquipConfig[] {
            mgr.getEquipEquipConfig(INSTANCE.value),
            mgr.getEquipEquipConfig(INSTANCE2.value),
        });
        for (var row : mgr._get_equip_equip_config_All().values()) {
            var key = row.getEntry();
            String symbol = row.getEntry();
            if (symbol.isEmpty()) {
                continue;
            }
            EquipConfig_Entry expected = switch (symbol) {
                case "Instance" -> INSTANCE;
                case "Instance2" -> INSTANCE2;
                default -> null;
            };
            if (expected == null) {
                errors.unknownSymbol("equip.equip_config", symbol, key, mgr.explain("equip.equip_config", key));
                continue;
            }
        }
        if (INSTANCE.ref(mgr) == null) {
            errors.missingSymbol("equip.equip_config", "Instance", INSTANCE.value);
        }
        if (INSTANCE2.ref(mgr) == null) {
            errors.missingSymbol("equip.equip_config", "Instance2", INSTANCE2.value);
        }
    }

}

package config.equip;

import org.jspecify.annotations.Nullable;

import configgen.genjava.LoadValueErrs;

// 来自：equip/equip_config.csv
public class EquipConfig_Entry {
    public static final EquipConfig_Entry INSTANCE = new EquipConfig_Entry(0, "Instance");
    public static final EquipConfig_Entry INSTANCE2 = new EquipConfig_Entry(1, "Instance2");

    private final String value;


    private EquipConfig_Entry(int index, String value) {
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
        var refs = new config.equip.EquipConfig[2];
        boolean complete = true;
        {
            var row = mgr.getEquipEquipConfig(INSTANCE.value);
            if (row == null) {
                errors.missingSymbol("equip.equip_config", "Instance", INSTANCE.value);
                complete = false;
            } else {
                refs[INSTANCE.index] = row;
            }
        }
        {
            var row = mgr.getEquipEquipConfig(INSTANCE2.value);
            if (row == null) {
                errors.missingSymbol("equip.equip_config", "Instance2", INSTANCE2.value);
                complete = false;
            } else {
                refs[INSTANCE2.index] = row;
            }
        }
        if (complete) {
            mgr._set_equip_equip_config_Refs(refs);
        }
    }

}

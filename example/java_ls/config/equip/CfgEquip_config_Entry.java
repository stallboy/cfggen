package config.equip;

import org.jspecify.annotations.Nullable;

import configgen.genjava.LoadValueErrs;

// 来自：equip/equip_config.csv
public class CfgEquip_config_Entry {
    public static final CfgEquip_config_Entry INSTANCE = new CfgEquip_config_Entry(0, "Instance");
    public static final CfgEquip_config_Entry INSTANCE2 = new CfgEquip_config_Entry(1, "Instance2");

    private final String value;


    private CfgEquip_config_Entry(int index, String value) {
        this.index = index;
        this.value = value;
    }

    private final int index;

    public config.equip.@Nullable CfgEquip_config ref() {
        return ref(config.ConfigMgr.getMgr());
    }

    public config.equip.@Nullable CfgEquip_config ref(config.ConfigMgr mgr) {
        return mgr._get_equip_equip_config_Refs(index);
    }

    public static void setAllRefs(config.ConfigMgr mgr, LoadValueErrs errors) {
        mgr._set_equip_equip_config_Refs(new config.equip.@Nullable CfgEquip_config[] {
            mgr.getEquipEquip_config(INSTANCE.value),
            mgr.getEquipEquip_config(INSTANCE2.value),
        });
        for (var entry : mgr._get_equip_equip_config_All().values()) {
            var row = java.util.Objects.requireNonNull(entry);
            var key = row.getEntry();
            String symbol = row.getEntry();
            if (symbol.isEmpty()) {
                continue;
            }
            CfgEquip_config_Entry expected = switch (symbol) {
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

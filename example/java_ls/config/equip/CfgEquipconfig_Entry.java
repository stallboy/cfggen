package config.equip;

import configgen.genjava.LoadValueErrs;

// 来自：equip/equipconfig.csv
public class CfgEquipconfig_Entry {
    public static final CfgEquipconfig_Entry INSTANCE = new CfgEquipconfig_Entry(0, "Instance");
    public static final CfgEquipconfig_Entry INSTANCE2 = new CfgEquipconfig_Entry(1, "Instance2");

    private final String value;


    CfgEquipconfig_Entry(int index, String value) {
        this.index = index;
        this.value = value;
    }

    private final int index;

    public config.equip.CfgEquipconfig ref() {
        return ref(config.ConfigMgr.getMgr());
    }

    public config.equip.CfgEquipconfig ref(config.ConfigMgr mgr) {
        return mgr._get_equip_equipconfig_Refs(index);
    }

    public static void setAllRefs(config.ConfigMgr mgr, LoadValueErrs errors) {
        mgr._set_equip_equipconfig_Refs(new config.equip.CfgEquipconfig[] {
            mgr.getEquipEquipconfig(INSTANCE.value),
            mgr.getEquipEquipconfig(INSTANCE2.value),
        });
        for (var row : mgr._get_equip_equipconfig_All().values()) {
            var key = row.getEntry();
            String symbol = row.getEntry();
            if (symbol.isEmpty()) continue;
            CfgEquipconfig_Entry expected = switch (symbol) {
                case "Instance" -> INSTANCE;
                case "Instance2" -> INSTANCE2;
                default -> null;
            };
            if (expected == null) {
                errors.unknownSymbol("equip.equipconfig", symbol, key, mgr.explain("equip.equipconfig", key));
                continue;
            }
        }
        if (INSTANCE.ref(mgr) == null) errors.missingSymbol("equip.equipconfig", "Instance", INSTANCE.value);
        if (INSTANCE2.ref(mgr) == null) errors.missingSymbol("equip.equipconfig", "Instance2", INSTANCE2.value);
    }

}

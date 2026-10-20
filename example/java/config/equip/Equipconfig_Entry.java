package config.equip;

import configgen.genjava.LoadValueErrs;

// 来自：equip/equipconfig.csv
public class Equipconfig_Entry {
    public static final Equipconfig_Entry INSTANCE = new Equipconfig_Entry(0, "Instance");
    public static final Equipconfig_Entry INSTANCE2 = new Equipconfig_Entry(1, "Instance2");

    private final String value;


    Equipconfig_Entry(int index, String value) {
        this.index = index;
        this.value = value;
    }

    private final int index;

    public config.equip.Equipconfig ref() {
        return ref(config.ConfigMgr.getMgr());
    }

    public config.equip.Equipconfig ref(config.ConfigMgr mgr) {
        return mgr._get_equip_equipconfig_Refs(index);
    }

    public static void setAllRefs(config.ConfigMgr mgr, LoadValueErrs errors) {
        mgr._set_equip_equipconfig_Refs(new config.equip.Equipconfig[] {
            mgr.getEquipEquipconfig(INSTANCE.value),
            mgr.getEquipEquipconfig(INSTANCE2.value),
        });
        for (var row : mgr._get_equip_equipconfig_All().values()) {
            var key = row.getEntry();
            String symbol = row.getEntry();
            if (symbol.isEmpty()) continue;
            Equipconfig_Entry expected = switch (symbol) {
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

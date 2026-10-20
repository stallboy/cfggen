package config.equip;

import configgen.genjava.LoadValueErrs;

// 来自：equip/jewelrysuit.csv
public class CfgJewelrysuit_Entry {
    public static final CfgJewelrysuit_Entry SPECIALSUIT = new CfgJewelrysuit_Entry(0, "SpecialSuit", 4);

    private final String name;
    private final int value;


    CfgJewelrysuit_Entry(int index, String name, int value) {
        this.index = index;
        this.name = name;
        this.value = value;
    }

    private final int index;

    public config.equip.CfgJewelrysuit ref() {
        return ref(config.ConfigMgr.getMgr());
    }

    public config.equip.CfgJewelrysuit ref(config.ConfigMgr mgr) {
        return mgr._get_equip_jewelrysuit_Refs(index);
    }

    public static void setAllRefs(config.ConfigMgr mgr, LoadValueErrs errors) {
        mgr._set_equip_jewelrysuit_Refs(new config.equip.CfgJewelrysuit[] {
            mgr.getEquipJewelrysuit(SPECIALSUIT.value),
        });
        for (var row : mgr._get_equip_jewelrysuit_All().values()) {
            var key = row.getSuitID();
            String symbol = row.getEname();
            if (symbol.isEmpty()) continue;
            CfgJewelrysuit_Entry expected = switch (symbol) {
                case "SpecialSuit" -> SPECIALSUIT;
                default -> null;
            };
            if (expected == null) {
                errors.unknownSymbol("equip.jewelrysuit", symbol, key, mgr.explain("equip.jewelrysuit", key));
                continue;
            }
            if (key != expected.value) errors.changedSymbolId("equip.jewelrysuit", symbol, expected.value, key, mgr.explain("equip.jewelrysuit", key));
        }
        if (SPECIALSUIT.ref(mgr) == null) errors.missingSymbol("equip.jewelrysuit", "SpecialSuit", SPECIALSUIT.value);
    }

}

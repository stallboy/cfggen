package config.equip;

import configgen.genjava.LoadValueErrs;

// 来自：equip/jewelry_suit.csv
public class CfgJewelry_suit_Entry {
    public static final CfgJewelry_suit_Entry SPECIALSUIT = new CfgJewelry_suit_Entry(0, "SpecialSuit", 4);

    private final String name;
    private final int value;


    CfgJewelry_suit_Entry(int index, String name, int value) {
        this.index = index;
        this.name = name;
        this.value = value;
    }

    private final int index;

    public config.equip.CfgJewelry_suit ref() {
        return ref(config.ConfigMgr.getMgr());
    }

    public config.equip.CfgJewelry_suit ref(config.ConfigMgr mgr) {
        return mgr._get_equip_jewelry_suit_Refs(index);
    }

    public static void setAllRefs(config.ConfigMgr mgr, LoadValueErrs errors) {
        mgr._set_equip_jewelry_suit_Refs(new config.equip.CfgJewelry_suit[] {
            mgr.getEquipJewelry_suit(SPECIALSUIT.value),
        });
        for (var row : mgr._get_equip_jewelry_suit_All().values()) {
            var key = row.getSuitID();
            String symbol = row.getEname();
            if (symbol.isEmpty()) {
                continue;
            }
            CfgJewelry_suit_Entry expected = switch (symbol) {
                case "SpecialSuit" -> SPECIALSUIT;
                default -> null;
            };
            if (expected == null) {
                errors.unknownSymbol("equip.jewelry_suit", symbol, key, mgr.explain("equip.jewelry_suit", key));
                continue;
            }
            if (key != expected.value) {
                errors.changedSymbolId("equip.jewelry_suit", symbol, expected.value, key, mgr.explain("equip.jewelry_suit", key));
            }
        }
        if (SPECIALSUIT.ref(mgr) == null) {
            errors.missingSymbol("equip.jewelry_suit", "SpecialSuit", SPECIALSUIT.value);
        }
    }

}

package config.equip;

import configgen.genjava.LoadValueErrs;

// 来自：equip/jewelry_suit.csv
public class JewelrySuit_Entry {
    public static final JewelrySuit_Entry SPECIAL_SUIT = new JewelrySuit_Entry(0, "SpecialSuit", 4);

    private final String name;
    private final int value;


    JewelrySuit_Entry(int index, String name, int value) {
        this.index = index;
        this.name = name;
        this.value = value;
    }

    private final int index;

    public config.equip.JewelrySuit ref() {
        return ref(config.ConfigMgr.getMgr());
    }

    public config.equip.JewelrySuit ref(config.ConfigMgr mgr) {
        return mgr._get_equip_jewelry_suit_Refs(index);
    }

    public static void setAllRefs(config.ConfigMgr mgr, LoadValueErrs errors) {
        mgr._set_equip_jewelry_suit_Refs(new config.equip.JewelrySuit[] {
            mgr.getEquipJewelrySuit(SPECIAL_SUIT.value),
        });
        for (var row : mgr._get_equip_jewelry_suit_All().values()) {
            var key = row.getSuitID();
            String symbol = row.getEname();
            if (symbol.isEmpty()) continue;
            JewelrySuit_Entry expected = switch (symbol) {
                case "SpecialSuit" -> SPECIAL_SUIT;
                default -> null;
            };
            if (expected == null) {
                errors.unknownSymbol("equip.jewelry_suit", symbol, key, mgr.explain("equip.jewelry_suit", key));
                continue;
            }
            if (key != expected.value) errors.changedSymbolId("equip.jewelry_suit", symbol, expected.value, key, mgr.explain("equip.jewelry_suit", key));
        }
        if (SPECIAL_SUIT.ref(mgr) == null) errors.missingSymbol("equip.jewelry_suit", "SpecialSuit", SPECIAL_SUIT.value);
    }

}

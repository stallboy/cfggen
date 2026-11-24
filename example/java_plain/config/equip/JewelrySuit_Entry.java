package config.equip;

import configgen.genjava.LoadValueErrs;

// 来自：equip/jewelry_suit.csv
public class JewelrySuit_Entry {
    public static final JewelrySuit_Entry SPECIAL_SUIT = new JewelrySuit_Entry(0, 4);

    private final int value;


    private JewelrySuit_Entry(int index, int value) {
        this.index = index;
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
        for (var row : mgr._get_equip_jewelry_suit_All().values()) {
            var key = row.getSuitID();
            String symbol = row.getEname();
            if (symbol.isEmpty()) {
                continue;
            }
            JewelrySuit_Entry expected = switch (symbol) {
                case "SpecialSuit" -> SPECIAL_SUIT;
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
        var refs = new config.equip.JewelrySuit[1];
        boolean complete = true;
        {
            var row = mgr.getEquipJewelrySuit(SPECIAL_SUIT.value);
            if (row == null) {
                errors.missingSymbol("equip.jewelry_suit", "SpecialSuit", SPECIAL_SUIT.value);
                complete = false;
            } else {
                refs[SPECIAL_SUIT.index] = row;
            }
        }
        if (complete) {
            mgr._set_equip_jewelry_suit_Refs(refs);
        }
    }

}

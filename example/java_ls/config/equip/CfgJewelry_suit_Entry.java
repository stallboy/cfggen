package config.equip;

import org.jspecify.annotations.Nullable;

import configgen.genjava.LoadValueErrs;

// 来自：equip/jewelry_suit.csv
public class CfgJewelry_suit_Entry {
    public static final CfgJewelry_suit_Entry SPECIALSUIT = new CfgJewelry_suit_Entry(0, 4);

    private final int value;


    private CfgJewelry_suit_Entry(int index, int value) {
        this.index = index;
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
        var refs = new config.equip.CfgJewelry_suit[1];
        boolean complete = true;
        {
            var row = mgr.getEquipJewelry_suit(SPECIALSUIT.value);
            if (row == null) {
                errors.missingSymbol("equip.jewelry_suit", "SpecialSuit", SPECIALSUIT.value);
                complete = false;
            } else {
                refs[SPECIALSUIT.index] = row;
            }
        }
        if (complete) {
            mgr._set_equip_jewelry_suit_Refs(refs);
        }
    }

}

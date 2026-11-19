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

    public config.equip.@Nullable CfgJewelry_suit ref() {
        return ref(config.ConfigMgr.getMgr());
    }

    public config.equip.@Nullable CfgJewelry_suit ref(config.ConfigMgr mgr) {
        return mgr._get_equip_jewelry_suit_Refs(index);
    }

    public static void setAllRefs(config.ConfigMgr mgr, LoadValueErrs errors) {
        mgr._set_equip_jewelry_suit_Refs(new config.equip.@Nullable CfgJewelry_suit[] {
            mgr.getEquipJewelry_suit(SPECIALSUIT.value),
        });
        for (var entry : mgr._get_equip_jewelry_suit_All().values()) {
            var row = java.util.Objects.requireNonNull(entry);
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

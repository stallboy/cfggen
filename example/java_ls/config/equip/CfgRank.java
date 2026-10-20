package config.equip;

import configgen.genjava.LoadValueErrs;

// 来自：equip/rank.csv
public enum CfgRank {
    WHITE("white", 0),
    GREEN("green", 1),
    BLUE("blue", 2),
    PURPLE("purple", 3),
    YELLOW("yellow", 4),
    RED("red", 5);

    private final String name;
    private final int value;


    CfgRank(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public static final java.util.Map<Integer, CfgRank> map;

    static {
        var symbols = new java.util.LinkedHashMap<Integer, CfgRank>();
        for(CfgRank e : CfgRank.values()) {
            symbols.put(e.value, e);
        }
        map = java.util.Collections.unmodifiableMap(symbols);
    }

    public static CfgRank get(int value) {
        return map.get(value);
    }

    /**
     * 稀有度
     */
    public int getRankID() {
        return value;
    }

    /**
     * 程序用名字
     */
    public String getRankName() {
        return name;
    }

    /**
     * 显示名称
     */
    public String getRankShowName() {
        return ref().getRankShowName();
    }

    public config.equip.CfgRank_Detail ref() {
        return ref(config.ConfigMgr.getMgr());
    }

    public config.equip.CfgRank_Detail ref(config.ConfigMgr mgr) {
        return mgr._get_equip_rank_Refs(ordinal());
    }

    public static void setAllRefs(config.ConfigMgr mgr, LoadValueErrs errors) {
        mgr._set_equip_rank_Refs(new config.equip.CfgRank_Detail[] {
            mgr.getEquipRank(WHITE.value),
            mgr.getEquipRank(GREEN.value),
            mgr.getEquipRank(BLUE.value),
            mgr.getEquipRank(PURPLE.value),
            mgr.getEquipRank(YELLOW.value),
            mgr.getEquipRank(RED.value),
        });
        for (var row : mgr._get_equip_rank_All()) {
            var key = row.getRankID();
            String symbol = row.getRankName();
            CfgRank expected = switch (symbol) {
                case "white" -> WHITE;
                case "green" -> GREEN;
                case "blue" -> BLUE;
                case "purple" -> PURPLE;
                case "yellow" -> YELLOW;
                case "red" -> RED;
                default -> null;
            };
            if (expected == null) {
                errors.unknownSymbol("equip.rank", symbol, key, mgr.explain("equip.rank", key));
                continue;
            }
            if (key != expected.value) errors.changedSymbolId("equip.rank", symbol, expected.value, key, mgr.explain("equip.rank", key));
        }
        for (CfgRank e : map.values()) {
            if (e.ref(mgr) == null) errors.missingSymbol("equip.rank", e.name, e.value);
        }
    }

}

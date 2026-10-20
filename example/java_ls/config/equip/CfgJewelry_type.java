package config.equip;

// 来自：equip/jewelry_type.csv
public enum CfgJewelry_type {
    JADE("Jade"),
    BRACELET("Bracelet"),
    MAGIC("Magic"),
    BOTTLE("Bottle");

    private final String value;


    CfgJewelry_type(String value) {
        this.value = value;
    }

    public static final java.util.Map<String, CfgJewelry_type> map;

    static {
        var symbols = new java.util.LinkedHashMap<String, CfgJewelry_type>();
        for(CfgJewelry_type e : CfgJewelry_type.values()) {
            symbols.put(e.value, e);
        }
        map = java.util.Collections.unmodifiableMap(symbols);
    }

    public static CfgJewelry_type get(String value) {
        return map.get(value);
    }

    /**
     * 程序用名字
     */
    public String getTypeName() {
        return value;
    }

}

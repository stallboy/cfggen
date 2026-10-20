package config.equip;

// 来自：equip/jewelrytype.csv
public enum CfgJewelrytype {
    JADE("Jade"),
    BRACELET("Bracelet"),
    MAGIC("Magic"),
    BOTTLE("Bottle");

    private final String value;


    CfgJewelrytype(String value) {
        this.value = value;
    }

    public static final java.util.Map<String, CfgJewelrytype> map;

    static {
        var symbols = new java.util.LinkedHashMap<String, CfgJewelrytype>();
        for(CfgJewelrytype e : CfgJewelrytype.values()) {
            symbols.put(e.value, e);
        }
        map = java.util.Collections.unmodifiableMap(symbols);
    }

    public static CfgJewelrytype get(String value) {
        return map.get(value);
    }

    /**
     * 程序用名字
     */
    public String getTypeName() {
        return value;
    }

}

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

    public static final java.util.Map<String, CfgJewelrytype> map = new java.util.HashMap<>();

    static {
        for(CfgJewelrytype e : CfgJewelrytype.values()) {
            map.put(e.value, e);
        }
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

package config.equip;

// 来自：equip/jewelrytype.csv
public enum Jewelrytype {
    JADE("Jade"),
    BRACELET("Bracelet"),
    MAGIC("Magic"),
    BOTTLE("Bottle");

    private final String value;


    Jewelrytype(String value) {
        this.value = value;
    }

    public static final java.util.Map<String, Jewelrytype> map;

    static {
        var symbols = new java.util.LinkedHashMap<String, Jewelrytype>();
        for(Jewelrytype e : Jewelrytype.values()) {
            symbols.put(e.value, e);
        }
        map = java.util.Collections.unmodifiableMap(symbols);
    }

    public static Jewelrytype get(String value) {
        return map.get(value);
    }

    /**
     * 程序用名字
     */
    public String getTypeName() {
        return value;
    }

}

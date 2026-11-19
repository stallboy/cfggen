package config.equip;

import org.jspecify.annotations.Nullable;

// 来自：equip/jewelry_type.csv
public enum JewelryType {
    JADE("Jade"),
    BRACELET("Bracelet"),
    MAGIC("Magic"),
    BOTTLE("Bottle");

    private final String value;


    JewelryType(String value) {
        this.value = value;
    }

    public static final java.util.Map<String, JewelryType> map;

    static {
        var symbols = new java.util.LinkedHashMap<String, JewelryType>();
        for(JewelryType e : JewelryType.values()) {
            symbols.put(e.value, e);
        }
        map = java.util.Collections.unmodifiableMap(symbols);
    }

    public static @Nullable JewelryType get(String value) {
        return map.get(value);
    }

    /**
     * 程序用名字
     */
    public String getTypeName() {
        return value;
    }

}

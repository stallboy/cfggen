package config.ai;

public enum Triggerticktype {
    CONSTVALUE("ConstValue"),
    BYLEVEL("ByLevel"),
    BYSERVERUPDAY("ByServerUpDay");

    private final String value;

    Triggerticktype(String value) {
        this.value = value;
    }

    private static final java.util.Map<String, Triggerticktype> map = new java.util.HashMap<>();

    static {
        for(Triggerticktype e : Triggerticktype.values()) {
            map.put(e.value, e);
        }
    }

    public static Triggerticktype get(String value) {
        return map.get(value);
    }

    /**
     * 枚举名称
     */
    public String getEnumName() {
        return value;
    }

}

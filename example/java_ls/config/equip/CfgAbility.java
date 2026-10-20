package config.equip;

// 来自：equip/ability.csv
public enum CfgAbility {
    ATTACK("attack", 1),
    DEFENCE("defence", 2),
    HP("hp", 3),
    CRITICAL("critical", 4),
    CRITICAL_RESIST("critical_resist", 5),
    BLOCK("block", 6),
    BREAK_ARMOR("break_armor", 7);

    private final String name;
    private final int value;


    CfgAbility(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public static final java.util.Map<Integer, CfgAbility> map;

    static {
        var symbols = new java.util.LinkedHashMap<Integer, CfgAbility>();
        for(CfgAbility e : CfgAbility.values()) {
            symbols.put(e.value, e);
        }
        map = java.util.Collections.unmodifiableMap(symbols);
    }

    public static CfgAbility get(int value) {
        return map.get(value);
    }

    /**
     * 属性类型
     */
    public int getId() {
        return value;
    }

    /**
     * 程序用名字
     */
    public String getName() {
        return name;
    }

}

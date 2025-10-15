package config.equip;

public enum Rank {
    WHITE("white", 1),
    GREEN("green", 2),
    BLUE("blue", 3),
    PURPLE("purple", 4),
    YELLOW("yellow", 5);

    private final String name;
    private final int value;
    private volatile config.equip.Rank_Detail ref;

    Rank(String name, int value) {
        this.name = name;
        this.value = value;
    }

    private static final java.util.Map<Integer, Rank> map = new java.util.HashMap<>();

    static {
        for(Rank e : Rank.values()) {
            map.put(e.value, e);
        }
    }

    public static Rank get(int value) {
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
        return ref.getRankShowName();
    }

    public config.equip.Rank_Detail ref() {
        return ref;
    }

    void setRef() {
        ref = config.equip.Rank_Detail.get(value);
        java.util.Objects.requireNonNull(ref);
    }

    public static void setAllRefs() {
        for(Rank e : Rank.values()) {
            e.setRef();
        }
    }
}

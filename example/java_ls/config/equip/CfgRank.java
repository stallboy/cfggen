package config.equip;

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
    private volatile config.equip.CfgRank_Detail ref;

    CfgRank(String name, int value) {
        this.name = name;
        this.value = value;
    }

    public static final java.util.Map<Integer, CfgRank> map = new java.util.HashMap<>();

    static {
        for(CfgRank e : CfgRank.values()) {
            map.put(e.value, e);
        }
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
        return ref.getRankShowName();
    }

    public config.equip.CfgRank_Detail ref() {
        return ref;
    }

    void setRef(config.ConfigMgr mgr) {
        ref = mgr.equip_rank_All[value];
        configgen.genjava.LoadValueErrs.requireNonNull(ref, "equip.rank.setRef", value);
    }

    public static void setAllRefs(config.ConfigMgr mgr) {
        for(CfgRank e : CfgRank.values()) {
            e.setRef(mgr);
        }
    }
}

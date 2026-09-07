package config;

public class CfgLevelRank {
    private int level;
    private int rank;
    private config.equip.CfgRank RefRank;

    private CfgLevelRank() {
    }

    public CfgLevelRank(int level, int rank) {
        this.level = level;
        this.rank = rank;
    }

    public static CfgLevelRank _create(configgen.genjava.ConfigInput input) {
        CfgLevelRank self = new CfgLevelRank();
        self.level = input.readInt();
        self.rank = input.readInt();
        return self;
    }

    /**
     * 等级
     */
    public int getLevel() {
        return level;
    }

    /**
     * 品质
     */
    public int getRank() {
        return rank;
    }

    public config.equip.CfgRank refRank() {
        return RefRank;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(level, rank);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CfgLevelRank))
            return false;
        CfgLevelRank o = (CfgLevelRank) other;
        return level == o.level && rank == o.rank;
    }

    @Override
    public String toString() {
        return "(" + level + "," + rank + ")";
    }

    public void _resolveDirect(config.ConfigMgr mgr) {
        RefRank = config.equip.CfgRank.get(rank);
        configgen.genjava.LoadValueErrs.requireNonNull(RefRank, "LevelRank.Rank -> equip.rank", rank);
    }

    public void _resolve(config.ConfigMgr mgr) {
        _resolveDirect(mgr);
    }

}

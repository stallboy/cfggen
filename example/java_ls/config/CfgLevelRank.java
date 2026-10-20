package config;

import configgen.genjava.LoadValueErrs;

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
        if (!(other instanceof CfgLevelRank o)) {
            return false;
        }
        return level == o.level && rank == o.rank;
    }

    @Override
    public String toString() {
        return "(" + level + "," + rank + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        this.RefRank = config.equip.CfgRank.get(this.rank);
        if (this.RefRank == null) {
            errors.missingReference("LevelRank", "Rank", "equip.rank", this.rank);
        }

    }

}

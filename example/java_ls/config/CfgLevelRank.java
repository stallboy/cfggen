package config;

import org.jspecify.annotations.Nullable;

import configgen.genjava.LoadValueErrs;

@SuppressWarnings("NotNullFieldNotInitialized")
public class CfgLevelRank {
    private final int level;
    private final int rank;
    private config.equip.CfgRank RefRank;

    public CfgLevelRank(int level, int rank) {
        this.level = level;
        this.rank = rank;
    }

    private CfgLevelRank(configgen.genjava.ConfigInput input) {
        this.level = input.readInt();
        this.rank = input.readInt();
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
    public boolean equals(@Nullable Object other) {
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
        var refRank = config.equip.CfgRank.get(this.rank);
        if (refRank == null) {
            errors.missingReference("LevelRank", "Rank", "equip.rank", this.rank);
        } else {
            this.RefRank = refRank;
        }

    }

    public static CfgLevelRank _create(configgen.genjava.ConfigInput input) {
        return new CfgLevelRank(input);
    }

}

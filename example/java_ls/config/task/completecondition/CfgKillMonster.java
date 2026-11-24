package config.task.completecondition;

import org.jspecify.annotations.Nullable;

import configgen.genjava.LoadValueErrs;

@SuppressWarnings("NotNullFieldNotInitialized")
public final class CfgKillMonster implements config.task.completecondition.CfgCompletecondition {
    private final int monsterid;
    private final int count;
    private config.other.CfgMonster RefMonsterid;

    public CfgKillMonster(int monsterid, int count) {
        this.monsterid = monsterid;
        this.count = count;
    }

    private CfgKillMonster(configgen.genjava.ConfigInput input) {
        this.monsterid = input.readInt();
        this.count = input.readInt();
    }

    @Override
    public config.task.CfgComplete_condition_type type() {
        return config.task.CfgComplete_condition_type.KILLMONSTER;
    }

    public int getMonsterid() {
        return monsterid;
    }

    public int getCount() {
        return count;
    }

    public config.other.CfgMonster refMonsterid() {
        return RefMonsterid;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(monsterid, count);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (!(other instanceof CfgKillMonster o)) {
            return false;
        }
        return monsterid == o.monsterid && count == o.count;
    }

    @Override
    public String toString() {
        return "CfgKillMonster(" + monsterid + "," + count + ")";
    }

    @Override
    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        var refMonsterid = mgr.getOtherMonster(this.monsterid);
        if (refMonsterid == null) {
            errors.missingReference("KillMonster", "monsterid", "other.monster", this.monsterid);
        } else {
            this.RefMonsterid = refMonsterid;
        }

    }

    public static CfgKillMonster _create(configgen.genjava.ConfigInput input) {
        return new CfgKillMonster(input);
    }

}

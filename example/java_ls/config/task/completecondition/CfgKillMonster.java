package config.task.completecondition;

import org.jspecify.annotations.Nullable;

import configgen.genjava.LoadValueErrs;

public final class CfgKillMonster implements config.task.completecondition.CfgCompletecondition {
    private final int monsterid;
    private final int count;
    private config.other.@Nullable CfgMonster RefMonsterid;

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

    public config.other.@Nullable CfgMonster refMonsterid() {
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
        this.RefMonsterid = mgr.getOtherMonster(this.monsterid);
        if (this.RefMonsterid == null) {
            errors.missingReference("KillMonster", "monsterid", "other.monster", this.monsterid);
        }

    }

    public static CfgKillMonster _create(configgen.genjava.ConfigInput input) {
        return new CfgKillMonster(input);
    }

}

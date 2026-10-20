package config.task.completecondition;

import configgen.genjava.LoadValueErrs;

public final class CfgKillMonster implements config.task.completecondition.CfgCompletecondition {
    @Override
    public config.task.CfgComplete_condition_type type() {
        return config.task.CfgComplete_condition_type.KILLMONSTER;
    }

    private int monsterid;
    private int count;
    private config.other.CfgMonster RefMonsterid;

    private CfgKillMonster() {
    }

    public CfgKillMonster(int monsterid, int count) {
        this.monsterid = monsterid;
        this.count = count;
    }

    public static CfgKillMonster _create(configgen.genjava.ConfigInput input) {
        CfgKillMonster self = new CfgKillMonster();
        self.monsterid = input.readInt();
        self.count = input.readInt();
        return self;
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
    public boolean equals(Object other) {
        if (!(other instanceof CfgKillMonster o))
            return false;
        return monsterid == o.monsterid && count == o.count;
    }

    @Override
    public String toString() {
        return "CfgKillMonster(" + monsterid + "," + count + ")";
    }

    @Override
    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        this.RefMonsterid = mgr.getOtherMonster(this.monsterid);
        if (this.RefMonsterid == null) errors.missingReference("KillMonster", "monsterid", "other.monster", this.monsterid);

    }

}

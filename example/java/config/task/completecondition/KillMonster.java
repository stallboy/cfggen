package config.task.completecondition;

import configgen.genjava.LoadValueErrs;

public final class KillMonster implements config.task.completecondition.Completecondition {
    @Override
    public config.task.CompleteConditionType type() {
        return config.task.CompleteConditionType.KILL_MONSTER;
    }

    private int monsterid;
    private int count;
    private config.other.Monster RefMonsterid;

    private KillMonster() {
    }

    public KillMonster(int monsterid, int count) {
        this.monsterid = monsterid;
        this.count = count;
    }

    public static KillMonster _create(configgen.genjava.ConfigInput input) {
        KillMonster self = new KillMonster();
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

    public config.other.Monster refMonsterid() {
        return RefMonsterid;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(monsterid, count);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof KillMonster o)) {
            return false;
        }
        return monsterid == o.monsterid && count == o.count;
    }

    @Override
    public String toString() {
        return "KillMonster(" + monsterid + "," + count + ")";
    }

    @Override
    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        this.RefMonsterid = mgr.getOtherMonster(this.monsterid);
        if (this.RefMonsterid == null) {
            errors.missingReference("KillMonster", "monsterid", "other.monster", this.monsterid);
        }

    }

}

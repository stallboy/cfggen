package config.task.completecondition;

import org.jspecify.annotations.Nullable;

import configgen.genjava.LoadValueErrs;

public final class KillMonster implements config.task.completecondition.Completecondition {
    private final int monsterid;
    private final int count;
    private config.other.@Nullable Monster RefMonsterid;

    public KillMonster(int monsterid, int count) {
        this.monsterid = monsterid;
        this.count = count;
    }

    private KillMonster(configgen.genjava.ConfigInput input) {
        this.monsterid = input.readInt();
        this.count = input.readInt();
    }

    @Override
    public config.task.CompleteConditionType type() {
        return config.task.CompleteConditionType.KILL_MONSTER;
    }

    public int getMonsterid() {
        return monsterid;
    }

    public int getCount() {
        return count;
    }

    public config.other.@Nullable Monster refMonsterid() {
        return RefMonsterid;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(monsterid, count);
    }

    @Override
    public boolean equals(@Nullable Object other) {
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

    public static KillMonster _create(configgen.genjava.ConfigInput input) {
        return new KillMonster(input);
    }

}

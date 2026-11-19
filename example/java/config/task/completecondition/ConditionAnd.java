package config.task.completecondition;

import org.jspecify.annotations.Nullable;

import configgen.genjava.LoadValueErrs;

public final class ConditionAnd implements config.task.completecondition.Completecondition {
    private final config.task.completecondition.Completecondition cond1;
    private final config.task.completecondition.Completecondition cond2;

    public ConditionAnd(config.task.completecondition.Completecondition cond1, config.task.completecondition.Completecondition cond2) {
        this.cond1 = cond1;
        this.cond2 = cond2;
    }

    private ConditionAnd(configgen.genjava.ConfigInput input) {
        this.cond1 = config.task.completecondition.Completecondition._create(input);
        this.cond2 = config.task.completecondition.Completecondition._create(input);
    }

    @Override
    public config.task.CompleteConditionType type() {
        return config.task.CompleteConditionType.CONDITION_AND;
    }

    public config.task.completecondition.Completecondition getCond1() {
        return cond1;
    }

    public config.task.completecondition.Completecondition getCond2() {
        return cond2;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(cond1, cond2);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (!(other instanceof ConditionAnd o)) {
            return false;
        }
        return cond1.equals(o.cond1) && cond2.equals(o.cond2);
    }

    @Override
    public String toString() {
        return "ConditionAnd(" + cond1 + "," + cond2 + ")";
    }

    @Override
    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        this.cond1._resolve(mgr, errors);
        this.cond2._resolve(mgr, errors);
    }

    public static ConditionAnd _create(configgen.genjava.ConfigInput input) {
        return new ConditionAnd(input);
    }

}

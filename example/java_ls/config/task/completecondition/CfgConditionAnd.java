package config.task.completecondition;

import org.jspecify.annotations.Nullable;

import configgen.genjava.LoadValueErrs;

public final class CfgConditionAnd implements config.task.completecondition.CfgCompletecondition {
    private final config.task.completecondition.CfgCompletecondition cond1;
    private final config.task.completecondition.CfgCompletecondition cond2;

    public CfgConditionAnd(config.task.completecondition.CfgCompletecondition cond1, config.task.completecondition.CfgCompletecondition cond2) {
        this.cond1 = cond1;
        this.cond2 = cond2;
    }

    private CfgConditionAnd(configgen.genjava.ConfigInput input) {
        this.cond1 = config.task.completecondition.CfgCompletecondition._create(input);
        this.cond2 = config.task.completecondition.CfgCompletecondition._create(input);
    }

    @Override
    public config.task.CfgComplete_condition_type type() {
        return config.task.CfgComplete_condition_type.CONDITIONAND;
    }

    public config.task.completecondition.CfgCompletecondition getCond1() {
        return cond1;
    }

    public config.task.completecondition.CfgCompletecondition getCond2() {
        return cond2;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(cond1, cond2);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (!(other instanceof CfgConditionAnd o)) {
            return false;
        }
        return cond1.equals(o.cond1) && cond2.equals(o.cond2);
    }

    @Override
    public String toString() {
        return "CfgConditionAnd(" + cond1 + "," + cond2 + ")";
    }

    @Override
    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        this.cond1._resolve(mgr, errors);
        this.cond2._resolve(mgr, errors);
    }

    public static CfgConditionAnd _create(configgen.genjava.ConfigInput input) {
        return new CfgConditionAnd(input);
    }

}

package config.task.completecondition;

public final class CfgConditionAnd implements config.task.completecondition.CfgCompletecondition {
    @Override
    public config.task.CfgCompleteconditiontype type() {
        return config.task.CfgCompleteconditiontype.CONDITIONAND;
    }

    private config.task.completecondition.CfgCompletecondition cond1;
    private config.task.completecondition.CfgCompletecondition cond2;

    private CfgConditionAnd() {
    }

    public CfgConditionAnd(config.task.completecondition.CfgCompletecondition cond1, config.task.completecondition.CfgCompletecondition cond2) {
        this.cond1 = cond1;
        this.cond2 = cond2;
    }

    public static CfgConditionAnd _create(configgen.genjava.ConfigInput input) {
        CfgConditionAnd self = new CfgConditionAnd();
        self.cond1 = config.task.completecondition.CfgCompletecondition._create(input);
        self.cond2 = config.task.completecondition.CfgCompletecondition._create(input);
        return self;
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
    public boolean equals(Object other) {
        if (!(other instanceof CfgConditionAnd))
            return false;
        CfgConditionAnd o = (CfgConditionAnd) other;
        return cond1.equals(o.cond1) && cond2.equals(o.cond2);
    }

    @Override
    public String toString() {
        return "CfgConditionAnd(" + cond1 + "," + cond2 + ")";
    }

    @Override
    public void _resolve(config.ConfigMgr mgr) {
        cond1._resolve(mgr);
        cond2._resolve(mgr);
    }

}

package config.task.completecondition;

public final class ConditionAnd implements config.task.completecondition.Completecondition {
    @Override
    public config.task.Completeconditiontype type() {
        return config.task.Completeconditiontype.CONDITIONAND;
    }

    private config.task.completecondition.Completecondition cond1;
    private config.task.completecondition.Completecondition cond2;

    private ConditionAnd() {
    }

    public ConditionAnd(config.task.completecondition.Completecondition cond1, config.task.completecondition.Completecondition cond2) {
        this.cond1 = cond1;
        this.cond2 = cond2;
    }

    public static ConditionAnd _create(configgen.genjava.ConfigInput input) {
        ConditionAnd self = new ConditionAnd();
        self.cond1 = config.task.completecondition.Completecondition._create(input);
        self.cond2 = config.task.completecondition.Completecondition._create(input);
        return self;
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
    public boolean equals(Object other) {
        if (!(other instanceof ConditionAnd))
            return false;
        ConditionAnd o = (ConditionAnd) other;
        return cond1.equals(o.cond1) && cond2.equals(o.cond2);
    }

    @Override
    public String toString() {
        return "ConditionAnd(" + cond1 + "," + cond2 + ")";
    }

    @Override
    public void _resolve(config.ConfigMgr mgr) {
        cond1._resolve(mgr);
        cond2._resolve(mgr);
    }

}

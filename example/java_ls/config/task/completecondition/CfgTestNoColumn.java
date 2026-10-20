package config.task.completecondition;

public final class CfgTestNoColumn implements config.task.completecondition.CfgCompletecondition {
    @Override
    public config.task.CfgComplete_condition_type type() {
        return config.task.CfgComplete_condition_type.TESTNOCOLUMN;
    }

    public CfgTestNoColumn() {
    }

    public static CfgTestNoColumn _create(configgen.genjava.ConfigInput input) {
        CfgTestNoColumn self = new CfgTestNoColumn();
        return self;
    }

    @Override
    public int hashCode() {
        return CfgTestNoColumn.class.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CfgTestNoColumn;
    }

    @Override
    public String toString() {
        return "CfgTestNoColumn";
    }

}

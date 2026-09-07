package config.task.completecondition;

public final class CfgAa implements config.task.completecondition.CfgCompletecondition {
    @Override
    public config.task.CfgCompleteconditiontype type() {
        return config.task.CfgCompleteconditiontype.AA;
    }


    public CfgAa() {
    }

    public static CfgAa _create(configgen.genjava.ConfigInput input) {
        CfgAa self = new CfgAa();
        return self;
    }

    @Override
    public int hashCode() {
        return CfgAa.class.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CfgAa;
    }

    @Override
    public String toString() {
        return "CfgAa";
    }

}

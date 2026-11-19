package config.task.completecondition;

import org.jspecify.annotations.Nullable;

public final class CfgAa implements config.task.completecondition.CfgCompletecondition {

    public CfgAa() {
    }

    private CfgAa(configgen.genjava.ConfigInput input) {
    }

    @Override
    public config.task.CfgComplete_condition_type type() {
        return config.task.CfgComplete_condition_type.AA;
    }

    @Override
    public int hashCode() {
        return CfgAa.class.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof CfgAa;
    }

    @Override
    public String toString() {
        return "CfgAa";
    }

    public static CfgAa _create(configgen.genjava.ConfigInput input) {
        return new CfgAa(input);
    }

}

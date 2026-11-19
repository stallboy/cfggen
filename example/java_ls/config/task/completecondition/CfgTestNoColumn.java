package config.task.completecondition;

import org.jspecify.annotations.Nullable;

public final class CfgTestNoColumn implements config.task.completecondition.CfgCompletecondition {

    public CfgTestNoColumn() {
    }

    private CfgTestNoColumn(configgen.genjava.ConfigInput input) {
    }

    @Override
    public config.task.CfgComplete_condition_type type() {
        return config.task.CfgComplete_condition_type.TESTNOCOLUMN;
    }

    @Override
    public int hashCode() {
        return CfgTestNoColumn.class.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof CfgTestNoColumn;
    }

    @Override
    public String toString() {
        return "CfgTestNoColumn";
    }

    public static CfgTestNoColumn _create(configgen.genjava.ConfigInput input) {
        return new CfgTestNoColumn(input);
    }

}

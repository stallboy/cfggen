package config.task.completecondition;

import org.jspecify.annotations.Nullable;

public final class Aa implements config.task.completecondition.Completecondition {

    public Aa() {
    }

    private Aa(configgen.genjava.ConfigInput input) {
    }

    @Override
    public config.task.CompleteConditionType type() {
        return config.task.CompleteConditionType.AA;
    }

    @Override
    public int hashCode() {
        return Aa.class.hashCode();
    }

    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof Aa;
    }

    @Override
    public String toString() {
        return "Aa";
    }

    public static Aa _create(configgen.genjava.ConfigInput input) {
        return new Aa(input);
    }

}

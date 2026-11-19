package config.ai.triggertick;

import org.jspecify.annotations.Nullable;

public final class CfgConstValue implements config.ai.triggertick.CfgTriggerTick {
    private final int value;

    public CfgConstValue(int value) {
        this.value = value;
    }

    private CfgConstValue(configgen.genjava.ConfigInput input) {
        this.value = input.readInt();
    }

    public int getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(value);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (!(other instanceof CfgConstValue o)) {
            return false;
        }
        return value == o.value;
    }

    @Override
    public String toString() {
        return "CfgConstValue(" + value + ")";
    }

    public static CfgConstValue _create(configgen.genjava.ConfigInput input) {
        return new CfgConstValue(input);
    }

}

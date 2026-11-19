package config.ai.triggertick;

import org.jspecify.annotations.Nullable;

public final class ConstValue implements config.ai.triggertick.TriggerTick {
    private final int value;

    public ConstValue(int value) {
        this.value = value;
    }

    private ConstValue(configgen.genjava.ConfigInput input) {
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
        if (!(other instanceof ConstValue o)) {
            return false;
        }
        return value == o.value;
    }

    @Override
    public String toString() {
        return "ConstValue(" + value + ")";
    }

    public static ConstValue _create(configgen.genjava.ConfigInput input) {
        return new ConstValue(input);
    }

}

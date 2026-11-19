package config.ai.triggertick;

import org.jspecify.annotations.Nullable;

public final class CfgByLevel implements config.ai.triggertick.CfgTriggerTick {
    private final int init;
    private final float coefficient;

    public CfgByLevel(int init, float coefficient) {
        this.init = init;
        this.coefficient = coefficient;
    }

    private CfgByLevel(configgen.genjava.ConfigInput input) {
        this.init = input.readInt();
        this.coefficient = input.readFloat();
    }

    public int getInit() {
        return init;
    }

    public float getCoefficient() {
        return coefficient;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(init, coefficient);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (!(other instanceof CfgByLevel o)) {
            return false;
        }
        return init == o.init && coefficient == o.coefficient;
    }

    @Override
    public String toString() {
        return "CfgByLevel(" + init + "," + coefficient + ")";
    }

    public static CfgByLevel _create(configgen.genjava.ConfigInput input) {
        return new CfgByLevel(input);
    }

}

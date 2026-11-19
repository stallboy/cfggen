package config.ai.triggertick;

import org.jspecify.annotations.Nullable;

public final class CfgByServerUpDay implements config.ai.triggertick.CfgTriggerTick {
    private final int init;
    private final float coefficient1;
    private final float coefficient2;

    public CfgByServerUpDay(int init, float coefficient1, float coefficient2) {
        this.init = init;
        this.coefficient1 = coefficient1;
        this.coefficient2 = coefficient2;
    }

    private CfgByServerUpDay(configgen.genjava.ConfigInput input) {
        this.init = input.readInt();
        this.coefficient1 = input.readFloat();
        this.coefficient2 = input.readFloat();
    }

    public int getInit() {
        return init;
    }

    public float getCoefficient1() {
        return coefficient1;
    }

    public float getCoefficient2() {
        return coefficient2;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(init, coefficient1, coefficient2);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (!(other instanceof CfgByServerUpDay o)) {
            return false;
        }
        return init == o.init && coefficient1 == o.coefficient1 && coefficient2 == o.coefficient2;
    }

    @Override
    public String toString() {
        return "CfgByServerUpDay(" + init + "," + coefficient1 + "," + coefficient2 + ")";
    }

    public static CfgByServerUpDay _create(configgen.genjava.ConfigInput input) {
        return new CfgByServerUpDay(input);
    }

}

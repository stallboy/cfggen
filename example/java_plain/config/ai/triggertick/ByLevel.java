package config.ai.triggertick;

public final class ByLevel implements config.ai.triggertick.TriggerTick {
    private final int init;
    private final float coefficient;

    public ByLevel(int init, float coefficient) {
        this.init = init;
        this.coefficient = coefficient;
    }

    private ByLevel(configgen.genjava.ConfigInput input) {
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
    public boolean equals(Object other) {
        if (!(other instanceof ByLevel o)) {
            return false;
        }
        return init == o.init && coefficient == o.coefficient;
    }

    @Override
    public String toString() {
        return "ByLevel(" + init + "," + coefficient + ")";
    }

    public static ByLevel _create(configgen.genjava.ConfigInput input) {
        return new ByLevel(input);
    }

}

package config.ai.triggertick;

public final class ByLevel implements config.ai.triggertick.TriggerTick {
    private int init;
    private float coefficient;

    private ByLevel() {
    }

    public ByLevel(int init, float coefficient) {
        this.init = init;
        this.coefficient = coefficient;
    }

    public static ByLevel _create(configgen.genjava.ConfigInput input) {
        ByLevel self = new ByLevel();
        self.init = input.readInt();
        self.coefficient = input.readFloat();
        return self;
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
        if (!(other instanceof ByLevel))
            return false;
        ByLevel o = (ByLevel) other;
        return init == o.init && coefficient == o.coefficient;
    }

    @Override
    public String toString() {
        return "ByLevel(" + init + "," + coefficient + ")";
    }

}

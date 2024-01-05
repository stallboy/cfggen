package config.ai.triggertick;

public final class ByServerUpDay implements config.ai.triggertick.TriggerTick {
    private int init;
    private float coefficient1;
    private float coefficient2;

    private ByServerUpDay() {
    }

    public ByServerUpDay(int init, float coefficient1, float coefficient2) {
        this.init = init;
        this.coefficient1 = coefficient1;
        this.coefficient2 = coefficient2;
    }

    public static ByServerUpDay _create(configgen.genjava.ConfigInput input) {
        ByServerUpDay self = new ByServerUpDay();
        self.init = input.readInt();
        self.coefficient1 = input.readFloat();
        self.coefficient2 = input.readFloat();
        return self;
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
    public boolean equals(Object other) {
        if (!(other instanceof ByServerUpDay))
            return false;
        ByServerUpDay o = (ByServerUpDay) other;
        return init == o.init && coefficient1 == o.coefficient1 && coefficient2 == o.coefficient2;
    }

    @Override
    public String toString() {
        return "ByServerUpDay(" + init + "," + coefficient1 + "," + coefficient2 + ")";
    }

}

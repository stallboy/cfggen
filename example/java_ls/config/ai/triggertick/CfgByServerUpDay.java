package config.ai.triggertick;

public final class CfgByServerUpDay implements config.ai.triggertick.CfgTriggerTick {
    private int init;
    private float coefficient1;
    private float coefficient2;

    private CfgByServerUpDay() {
    }

    public CfgByServerUpDay(int init, float coefficient1, float coefficient2) {
        this.init = init;
        this.coefficient1 = coefficient1;
        this.coefficient2 = coefficient2;
    }

    public static CfgByServerUpDay _create(configgen.genjava.ConfigInput input) {
        CfgByServerUpDay self = new CfgByServerUpDay();
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
        if (!(other instanceof CfgByServerUpDay))
            return false;
        CfgByServerUpDay o = (CfgByServerUpDay) other;
        return init == o.init && coefficient1 == o.coefficient1 && coefficient2 == o.coefficient2;
    }

    @Override
    public String toString() {
        return "CfgByServerUpDay(" + init + "," + coefficient1 + "," + coefficient2 + ")";
    }

}

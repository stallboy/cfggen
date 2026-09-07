package config.ai.triggertick;

public final class CfgByLevel implements config.ai.triggertick.CfgTriggerTick {
    private int init;
    private float coefficient;

    private CfgByLevel() {
    }

    public CfgByLevel(int init, float coefficient) {
        this.init = init;
        this.coefficient = coefficient;
    }

    public static CfgByLevel _create(configgen.genjava.ConfigInput input) {
        CfgByLevel self = new CfgByLevel();
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
        if (!(other instanceof CfgByLevel))
            return false;
        CfgByLevel o = (CfgByLevel) other;
        return init == o.init && coefficient == o.coefficient;
    }

    @Override
    public String toString() {
        return "CfgByLevel(" + init + "," + coefficient + ")";
    }

}

package config.ai.triggertick;

public final class CfgConstValue implements config.ai.triggertick.CfgTriggerTick {
    private int value;

    private CfgConstValue() {
    }

    public CfgConstValue(int value) {
        this.value = value;
    }

    public static CfgConstValue _create(configgen.genjava.ConfigInput input) {
        CfgConstValue self = new CfgConstValue();
        self.value = input.readInt();
        return self;
    }

    public int getValue() {
        return value;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(value);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CfgConstValue))
            return false;
        CfgConstValue o = (CfgConstValue) other;
        return value == o.value;
    }

    @Override
    public String toString() {
        return "CfgConstValue(" + value + ")";
    }

}

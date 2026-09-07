package config;

public class CfgRange {
    private int min;
    private int max;

    private CfgRange() {
    }

    public CfgRange(int min, int max) {
        this.min = min;
        this.max = max;
    }

    public static CfgRange _create(configgen.genjava.ConfigInput input) {
        CfgRange self = new CfgRange();
        self.min = input.readInt();
        self.max = input.readInt();
        return self;
    }

    /**
     * 最小
     */
    public int getMin() {
        return min;
    }

    /**
     * 最大
     */
    public int getMax() {
        return max;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(min, max);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CfgRange))
            return false;
        CfgRange o = (CfgRange) other;
        return min == o.min && max == o.max;
    }

    @Override
    public String toString() {
        return "(" + min + "," + max + ")";
    }

}

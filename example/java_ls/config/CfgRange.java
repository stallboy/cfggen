package config;

import org.jspecify.annotations.Nullable;

public class CfgRange {
    private final int min;
    private final int max;

    public CfgRange(int min, int max) {
        this.min = min;
        this.max = max;
    }

    private CfgRange(configgen.genjava.ConfigInput input) {
        this.min = input.readInt();
        this.max = input.readInt();
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
    public boolean equals(@Nullable Object other) {
        if (!(other instanceof CfgRange o)) {
            return false;
        }
        return min == o.min && max == o.max;
    }

    @Override
    public String toString() {
        return "(" + min + "," + max + ")";
    }

    public static CfgRange _create(configgen.genjava.ConfigInput input) {
        return new CfgRange(input);
    }

}

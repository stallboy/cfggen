package config.equip;

import org.jspecify.annotations.Nullable;

public class CfgTestPackBean {
    private final String name;
    private final config.CfgRange iRange;

    public CfgTestPackBean(String name, config.CfgRange iRange) {
        this.name = name;
        this.iRange = iRange;
    }

    private CfgTestPackBean(configgen.genjava.ConfigInput input) {
        this.name = input.readStringInPool();
        this.iRange = config.CfgRange._create(input);
    }

    public String getName() {
        return name;
    }

    public config.CfgRange getIRange() {
        return iRange;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(name, iRange);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (!(other instanceof CfgTestPackBean o)) {
            return false;
        }
        return name.equals(o.name) && iRange.equals(o.iRange);
    }

    @Override
    public String toString() {
        return "(" + name + "," + iRange + ")";
    }

    public static CfgTestPackBean _create(configgen.genjava.ConfigInput input) {
        return new CfgTestPackBean(input);
    }

}

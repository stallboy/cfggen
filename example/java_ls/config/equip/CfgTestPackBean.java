package config.equip;

public class CfgTestPackBean {
    private String name;
    private config.CfgRange iRange;

    private CfgTestPackBean() {
    }

    public CfgTestPackBean(String name, config.CfgRange iRange) {
        this.name = name;
        this.iRange = iRange;
    }

    public static CfgTestPackBean _create(configgen.genjava.ConfigInput input) {
        CfgTestPackBean self = new CfgTestPackBean();
        self.name = input.readStringInPool();
        self.iRange = config.CfgRange._create(input);
        return self;
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
    public boolean equals(Object other) {
        if (!(other instanceof CfgTestPackBean))
            return false;
        CfgTestPackBean o = (CfgTestPackBean) other;
        return name.equals(o.name) && iRange.equals(o.iRange);
    }

    @Override
    public String toString() {
        return "(" + name + "," + iRange + ")";
    }

}

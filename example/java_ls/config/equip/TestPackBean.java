package config.equip;

public class TestPackBean {
    private String name;
    private config.Range iRange;

    private TestPackBean() {
    }

    public TestPackBean(String name, config.Range iRange) {
        this.name = name;
        this.iRange = iRange;
    }

    public static TestPackBean _create(configgen.genjava.ConfigInput input) {
        TestPackBean self = new TestPackBean();
        self.name = input.readStringInPool();
        self.iRange = config.Range._create(input);
        return self;
    }

    public String getName() {
        return name;
    }

    public config.Range getIRange() {
        return iRange;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(name, iRange);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TestPackBean))
            return false;
        TestPackBean o = (TestPackBean) other;
        return name.equals(o.name) && iRange.equals(o.iRange);
    }

    @Override
    public String toString() {
        return "(" + name + "," + iRange + ")";
    }

}

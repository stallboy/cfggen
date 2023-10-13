package config.equip;

public class TestPackBean {
    private String name;
    private config.Range range;

    private TestPackBean() {
    }

    public TestPackBean(String name, config.Range range) {
        this.name = name;
        this.range = range;
    }

    public static TestPackBean _create(configgen.genjava.ConfigInput input) {
        TestPackBean self = new TestPackBean();
        self.name = input.readStr();
        self.range = config.Range._create(input);
        return self;
    }

    public String getName() {
        return name;
    }

    public config.Range getRange() {
        return range;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(name, range);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof TestPackBean))
            return false;
        TestPackBean o = (TestPackBean) other;
        return name.equals(o.name) && range.equals(o.range);
    }

    @Override
    public String toString() {
        return "(" + name + "," + range + ")";
    }

}

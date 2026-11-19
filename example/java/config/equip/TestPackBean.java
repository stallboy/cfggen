package config.equip;

import org.jspecify.annotations.Nullable;

public class TestPackBean {
    private final String name;
    private final config.Range iRange;

    public TestPackBean(String name, config.Range iRange) {
        this.name = name;
        this.iRange = iRange;
    }

    private TestPackBean(configgen.genjava.ConfigInput input) {
        this.name = input.readStringInPool();
        this.iRange = config.Range._create(input);
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
    public boolean equals(@Nullable Object other) {
        if (!(other instanceof TestPackBean o)) {
            return false;
        }
        return name.equals(o.name) && iRange.equals(o.iRange);
    }

    @Override
    public String toString() {
        return "(" + name + "," + iRange + ")";
    }

    public static TestPackBean _create(configgen.genjava.ConfigInput input) {
        return new TestPackBean(input);
    }

}

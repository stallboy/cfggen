package config.task.completecondition;

public final class TestNoColumn implements config.task.completecondition.Completecondition {
    @Override
    public config.task.Completeconditiontype type() {
        return config.task.Completeconditiontype.TESTNOCOLUMN;
    }


    public TestNoColumn() {
    }

    public static TestNoColumn _create(configgen.genjava.ConfigInput input) {
        TestNoColumn self = new TestNoColumn();
        return self;
    }

    @Override
    public int hashCode() {
        return TestNoColumn.class.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof TestNoColumn;
    }

    @Override
    public String toString() {
        return "TestNoColumn";
    }

}

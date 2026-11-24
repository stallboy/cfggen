package config.task.completecondition;

public final class TestNoColumn implements config.task.completecondition.Completecondition {

    public TestNoColumn() {
    }

    private TestNoColumn(configgen.genjava.ConfigInput input) {
    }

    @Override
    public config.task.CompleteConditionType type() {
        return config.task.CompleteConditionType.TEST_NO_COLUMN;
    }

    @Override
    public int hashCode() {
        return TestNoColumn.class.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TestNoColumn;
    }

    @Override
    public String toString() {
        return "TestNoColumn";
    }

    public static TestNoColumn _create(configgen.genjava.ConfigInput input) {
        return new TestNoColumn(input);
    }

}

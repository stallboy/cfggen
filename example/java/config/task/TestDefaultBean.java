package config.task;

import org.jspecify.annotations.Nullable;

public class TestDefaultBean {
    private final int testInt;
    private final boolean testBool;
    private final String testString;
    private final config.Position testSubBean;
    private final java.util.List<Integer> testList;
    private final java.util.List<Integer> testList2;
    private final java.util.Map<Integer, String> testMap;

    public TestDefaultBean(int testInt, boolean testBool, String testString, config.Position testSubBean, java.util.List<Integer> testList, java.util.List<Integer> testList2, java.util.Map<Integer, String> testMap) {
        this.testInt = testInt;
        this.testBool = testBool;
        this.testString = testString;
        this.testSubBean = testSubBean;
        this.testList = testList;
        this.testList2 = testList2;
        this.testMap = testMap;
    }

    private TestDefaultBean(configgen.genjava.ConfigInput input) {
        this.testInt = input.readInt();
        this.testBool = input.readBool();
        this.testString = input.readStringInPool();
        this.testSubBean = config.Position._create(input);
        {
            int c = input.readCount(0);
            if (c == 0) {
                this.testList = java.util.Collections.emptyList();
            } else {
                var items = new java.util.ArrayList<Integer>(c);
                for (; c > 0; c--) {
                    items.add(input.readInt());
                }
                this.testList = java.util.Collections.unmodifiableList(items);
            }
        }
        {
            int c = input.readCount(0);
            if (c == 0) {
                this.testList2 = java.util.Collections.emptyList();
            } else {
                var items = new java.util.ArrayList<Integer>(c);
                for (; c > 0; c--) {
                    items.add(input.readInt());
                }
                this.testList2 = java.util.Collections.unmodifiableList(items);
            }
        }
        {
            int c = input.readCount(0);
            if (c == 0) {
                this.testMap = java.util.Collections.emptyMap();
            } else {
                var entries = new java.util.LinkedHashMap<Integer, String>(c);
                for (; c > 0; c--) {
                    var key = input.readInt();
                    var value = input.readStringInPool();
                    if (entries.putIfAbsent(key, value) != null) {
                        input.errors().duplicateKey("task.TestDefaultBean.testMap", key);
                    }
                }
                this.testMap = java.util.Collections.unmodifiableMap(entries);
            }
        }
    }

    public int getTestInt() {
        return testInt;
    }

    public boolean getTestBool() {
        return testBool;
    }

    public String getTestString() {
        return testString;
    }

    public config.Position getTestSubBean() {
        return testSubBean;
    }

    public java.util.List<Integer> getTestList() {
        return testList;
    }

    public java.util.List<Integer> getTestList2() {
        return testList2;
    }

    public java.util.Map<Integer, String> getTestMap() {
        return testMap;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(testInt, testBool, testString, testSubBean, testList, testList2, testMap);
    }

    @Override
    public boolean equals(@Nullable Object other) {
        if (!(other instanceof TestDefaultBean o)) {
            return false;
        }
        return testInt == o.testInt && testBool == o.testBool && testString.equals(o.testString) && testSubBean.equals(o.testSubBean) && testList.equals(o.testList) && testList2.equals(o.testList2) && testMap.equals(o.testMap);
    }

    @Override
    public String toString() {
        return "(" + testInt + "," + testBool + "," + testString + "," + testSubBean + "," + testList + "," + testList2 + "," + testMap + ")";
    }

    public static TestDefaultBean _create(configgen.genjava.ConfigInput input) {
        return new TestDefaultBean(input);
    }

}

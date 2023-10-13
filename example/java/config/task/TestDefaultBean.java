package config.task;

public class TestDefaultBean {
    private int testInt;
    private boolean testBool;
    private String testString;
    private config.Position testSubBean;
    private java.util.List<Integer> testList;
    private java.util.List<Integer> testList2;
    private java.util.Map<Integer, String> testMap;

    private TestDefaultBean() {
    }

    public TestDefaultBean(int testInt, boolean testBool, String testString, config.Position testSubBean, java.util.List<Integer> testList, java.util.List<Integer> testList2, java.util.Map<Integer, String> testMap) {
        this.testInt = testInt;
        this.testBool = testBool;
        this.testString = testString;
        this.testSubBean = testSubBean;
        this.testList = testList;
        this.testList2 = testList2;
        this.testMap = testMap;
    }

    public static TestDefaultBean _create(configgen.genjava.ConfigInput input) {
        TestDefaultBean self = new TestDefaultBean();
        self.testInt = input.readInt();
        self.testBool = input.readBool();
        self.testString = input.readStr();
        self.testSubBean = config.Position._create(input);
        self.testList = new java.util.ArrayList<>();
        for (int c = input.readInt(); c > 0; c--) {
            self.testList.add(input.readInt());
        }
        self.testList2 = new java.util.ArrayList<>();
        for (int c = input.readInt(); c > 0; c--) {
            self.testList2.add(input.readInt());
        }
        self.testMap = new java.util.LinkedHashMap<>();
        for (int c = input.readInt(); c > 0; c--) {
            self.testMap.put(input.readInt(), input.readStr());
        }
        return self;
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
    public boolean equals(Object other) {
        if (!(other instanceof TestDefaultBean))
            return false;
        TestDefaultBean o = (TestDefaultBean) other;
        return testInt == o.testInt && testBool == o.testBool && testString.equals(o.testString) && testSubBean.equals(o.testSubBean) && testList.equals(o.testList) && testList2.equals(o.testList2) && testMap.equals(o.testMap);
    }

    @Override
    public String toString() {
        return "(" + testInt + "," + testBool + "," + testString + "," + testSubBean + "," + testList + "," + testList2 + "," + testMap + ")";
    }

}

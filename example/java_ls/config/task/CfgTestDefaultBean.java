package config.task;

public class CfgTestDefaultBean {
    private int testInt;
    private boolean testBool;
    private String testString;
    private config.CfgPosition testSubBean;
    private java.util.List<Integer> testList;
    private java.util.List<Integer> testList2;
    private java.util.Map<Integer, String> testMap;

    private CfgTestDefaultBean() {
    }

    public CfgTestDefaultBean(int testInt, boolean testBool, String testString, config.CfgPosition testSubBean, java.util.List<Integer> testList, java.util.List<Integer> testList2, java.util.Map<Integer, String> testMap) {
        this.testInt = testInt;
        this.testBool = testBool;
        this.testString = testString;
        this.testSubBean = testSubBean;
        this.testList = testList;
        this.testList2 = testList2;
        this.testMap = testMap;
    }

    public static CfgTestDefaultBean _create(configgen.genjava.ConfigInput input) {
        CfgTestDefaultBean self = new CfgTestDefaultBean();
        self.testInt = input.readInt();
        self.testBool = input.readBool();
        self.testString = input.readStringInPool();
        self.testSubBean = config.CfgPosition._create(input);
        {
            int c = input.readInt();
            if (c == 0) {
                self.testList = java.util.Collections.emptyList();
            } else {
                self.testList = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.testList.add(input.readInt());
                }
            }
        }
        {
            int c = input.readInt();
            if (c == 0) {
                self.testList2 = java.util.Collections.emptyList();
            } else {
                self.testList2 = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.testList2.add(input.readInt());
                }
            }
        }
        {
            int c = input.readInt();
            if (c == 0) {
                self.testMap = java.util.Collections.emptyMap();
            } else {
                self.testMap = new java.util.LinkedHashMap<>(c);
                for (; c > 0; c--) {
                    self.testMap.put(input.readInt(), input.readStringInPool());
                }
            }
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

    public config.CfgPosition getTestSubBean() {
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
        if (!(other instanceof CfgTestDefaultBean))
            return false;
        CfgTestDefaultBean o = (CfgTestDefaultBean) other;
        return testInt == o.testInt && testBool == o.testBool && testString.equals(o.testString) && testSubBean.equals(o.testSubBean) && testList.equals(o.testList) && testList2.equals(o.testList2) && testMap.equals(o.testMap);
    }

    @Override
    public String toString() {
        return "(" + testInt + "," + testBool + "," + testString + "," + testSubBean + "," + testList + "," + testList2 + "," + testMap + ")";
    }

}

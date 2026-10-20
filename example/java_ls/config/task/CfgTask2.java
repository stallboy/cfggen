package config.task;

import configgen.genjava.LoadValueErrs;

public class CfgTask2 {
    private int taskid;
    private java.util.List<config.Text> name;
    private int nexttask;
    private config.task.completecondition.CfgCompletecondition completecondition;
    private int exp;
    private boolean testBool;
    private String testString;
    private config.CfgPosition testStruct;
    private java.util.List<Integer> testList;
    private java.util.List<config.CfgPosition> testListStruct;
    private java.util.List<config.ai.triggertick.CfgTriggerTick> testListInterface;
    private config.task.CfgTaskextraexp NullableRefTaskid;
    private config.task.CfgTask NullableRefNexttask;

    private CfgTask2() {
    }

    public static CfgTask2 _create(configgen.genjava.ConfigInput input) {
        CfgTask2 self = new CfgTask2();
        self.taskid = input.readInt();
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.name = java.util.Collections.emptyList();
            } else {
                self.name = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.name.add(config.Text._create(input));
                }
                self.name = java.util.Collections.unmodifiableList(self.name);
            }
        }
        self.nexttask = input.readInt();
        self.completecondition = config.task.completecondition.CfgCompletecondition._create(input);
        self.exp = input.readInt();
        self.testBool = input.readBool();
        self.testString = input.readStringInPool();
        self.testStruct = config.CfgPosition._create(input);
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.testList = java.util.Collections.emptyList();
            } else {
                self.testList = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.testList.add(input.readInt());
                }
                self.testList = java.util.Collections.unmodifiableList(self.testList);
            }
        }
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.testListStruct = java.util.Collections.emptyList();
            } else {
                self.testListStruct = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.testListStruct.add(config.CfgPosition._create(input));
                }
                self.testListStruct = java.util.Collections.unmodifiableList(self.testListStruct);
            }
        }
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.testListInterface = java.util.Collections.emptyList();
            } else {
                self.testListInterface = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.testListInterface.add(config.ai.triggertick.CfgTriggerTick._create(input));
                }
                self.testListInterface = java.util.Collections.unmodifiableList(self.testListInterface);
            }
        }
        return self;
    }

    /**
     * 任务完成条件类型（id的范围为1-100）
     */
    public int getTaskid() {
        return taskid;
    }

    public java.util.List<config.Text> getName() {
        return name;
    }

    public int getNexttask() {
        return nexttask;
    }

    public config.task.completecondition.CfgCompletecondition getCompletecondition() {
        return completecondition;
    }

    public int getExp() {
        return exp;
    }

    public boolean getTestBool() {
        return testBool;
    }

    public String getTestString() {
        return testString;
    }

    public config.CfgPosition getTestStruct() {
        return testStruct;
    }

    public java.util.List<Integer> getTestList() {
        return testList;
    }

    public java.util.List<config.CfgPosition> getTestListStruct() {
        return testListStruct;
    }

    public java.util.List<config.ai.triggertick.CfgTriggerTick> getTestListInterface() {
        return testListInterface;
    }

    public config.task.CfgTaskextraexp nullableRefTaskid() {
        return NullableRefTaskid;
    }

    public config.task.CfgTask nullableRefNexttask() {
        return NullableRefNexttask;
    }

    @Override
    public String toString() {
        return "(" + taskid + "," + name + "," + nexttask + "," + completecondition + "," + exp + "," + testBool + "," + testString + "," + testStruct + "," + testList + "," + testListStruct + "," + testListInterface + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        this.completecondition._resolve(mgr, errors);
        this.NullableRefTaskid = mgr.getTaskTaskextraexp(this.taskid);
        this.NullableRefNexttask = mgr.getTaskTask(this.nexttask);

    }

    public static CfgTask2 get(int taskid) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getTaskTask2(taskid);
    }

    public static java.util.Collection<CfgTask2> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allTaskTask2();
    }


}

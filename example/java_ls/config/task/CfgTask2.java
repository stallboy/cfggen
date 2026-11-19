package config.task;

import org.jspecify.annotations.Nullable;

import configgen.genjava.LoadValueErrs;

public class CfgTask2 {
    private final int taskid;
    private final java.util.List<config.Text> name;
    private final int nexttask;
    private final config.task.completecondition.CfgCompletecondition completecondition;
    private final int exp;
    private final boolean testBool;
    private final String testString;
    private final config.CfgPosition testStruct;
    private final java.util.List<Integer> testList;
    private final java.util.List<config.CfgPosition> testListStruct;
    private final java.util.List<config.ai.triggertick.CfgTriggerTick> testListInterface;
    private config.task.@Nullable CfgTask_extra_exp NullableRefTaskid;
    private config.task.@Nullable CfgTask NullableRefNexttask;

    private CfgTask2(configgen.genjava.ConfigInput input) {
        this.taskid = input.readInt();
        {
            int c = input.readCount(0);
            if (c == 0) {
                this.name = java.util.Collections.emptyList();
            } else {
                var items = new java.util.ArrayList<config.Text>(c);
                for (; c > 0; c--) {
                    items.add(config.Text._create(input));
                }
                this.name = java.util.Collections.unmodifiableList(items);
            }
        }
        this.nexttask = input.readInt();
        this.completecondition = config.task.completecondition.CfgCompletecondition._create(input);
        this.exp = input.readInt();
        this.testBool = input.readBool();
        this.testString = input.readStringInPool();
        this.testStruct = config.CfgPosition._create(input);
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
                this.testListStruct = java.util.Collections.emptyList();
            } else {
                var items = new java.util.ArrayList<config.CfgPosition>(c);
                for (; c > 0; c--) {
                    items.add(config.CfgPosition._create(input));
                }
                this.testListStruct = java.util.Collections.unmodifiableList(items);
            }
        }
        {
            int c = input.readCount(0);
            if (c == 0) {
                this.testListInterface = java.util.Collections.emptyList();
            } else {
                var items = new java.util.ArrayList<config.ai.triggertick.CfgTriggerTick>(c);
                for (; c > 0; c--) {
                    items.add(config.ai.triggertick.CfgTriggerTick._create(input));
                }
                this.testListInterface = java.util.Collections.unmodifiableList(items);
            }
        }
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

    public config.task.@Nullable CfgTask_extra_exp nullableRefTaskid() {
        return NullableRefTaskid;
    }

    public config.task.@Nullable CfgTask nullableRefNexttask() {
        return NullableRefNexttask;
    }

    @Override
    public String toString() {
        return "(" + taskid + "," + name + "," + nexttask + "," + completecondition + "," + exp + "," + testBool + "," + testString + "," + testStruct + "," + testList + "," + testListStruct + "," + testListInterface + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        this.completecondition._resolve(mgr, errors);
        this.NullableRefTaskid = mgr.getTaskTask_extra_exp(this.taskid);
        this.NullableRefNexttask = mgr.getTaskTask(this.nexttask);

    }

    public static CfgTask2 _create(configgen.genjava.ConfigInput input) {
        return new CfgTask2(input);
    }

    public static @Nullable CfgTask2 get(int taskid) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getTaskTask2(taskid);
    }

    public static java.util.Collection<CfgTask2> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allTaskTask2();
    }


}

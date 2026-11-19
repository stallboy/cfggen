package config.task;

import org.jspecify.annotations.Nullable;

import configgen.genjava.LoadValueErrs;

public class Task2 {
    private final int taskid;
    private final java.util.List<String> name;
    private final int nexttask;
    private final config.task.completecondition.Completecondition completecondition;
    private final int exp;
    private final boolean testBool;
    private final String testString;
    private final config.Position testStruct;
    private final java.util.List<Integer> testList;
    private final java.util.List<config.Position> testListStruct;
    private final java.util.List<config.ai.triggertick.TriggerTick> testListInterface;
    private config.task.@Nullable TaskExtraExp NullableRefTaskid;
    private config.task.@Nullable Task NullableRefNexttask;

    private Task2(configgen.genjava.ConfigInput input) {
        this.taskid = input.readInt();
        {
            int c = input.readCount(0);
            if (c == 0) {
                this.name = java.util.Collections.emptyList();
            } else {
                var items = new java.util.ArrayList<String>(c);
                for (; c > 0; c--) {
                    items.add(input.readTextInPool());
                }
                this.name = java.util.Collections.unmodifiableList(items);
            }
        }
        this.nexttask = input.readInt();
        this.completecondition = config.task.completecondition.Completecondition._create(input);
        this.exp = input.readInt();
        this.testBool = input.readBool();
        this.testString = input.readStringInPool();
        this.testStruct = config.Position._create(input);
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
                var items = new java.util.ArrayList<config.Position>(c);
                for (; c > 0; c--) {
                    items.add(config.Position._create(input));
                }
                this.testListStruct = java.util.Collections.unmodifiableList(items);
            }
        }
        {
            int c = input.readCount(0);
            if (c == 0) {
                this.testListInterface = java.util.Collections.emptyList();
            } else {
                var items = new java.util.ArrayList<config.ai.triggertick.TriggerTick>(c);
                for (; c > 0; c--) {
                    items.add(config.ai.triggertick.TriggerTick._create(input));
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

    public java.util.List<String> getName() {
        return name;
    }

    public int getNexttask() {
        return nexttask;
    }

    public config.task.completecondition.Completecondition getCompletecondition() {
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

    public config.Position getTestStruct() {
        return testStruct;
    }

    public java.util.List<Integer> getTestList() {
        return testList;
    }

    public java.util.List<config.Position> getTestListStruct() {
        return testListStruct;
    }

    public java.util.List<config.ai.triggertick.TriggerTick> getTestListInterface() {
        return testListInterface;
    }

    public config.task.@Nullable TaskExtraExp nullableRefTaskid() {
        return NullableRefTaskid;
    }

    public config.task.@Nullable Task nullableRefNexttask() {
        return NullableRefNexttask;
    }

    @Override
    public String toString() {
        return "(" + taskid + "," + name + "," + nexttask + "," + completecondition + "," + exp + "," + testBool + "," + testString + "," + testStruct + "," + testList + "," + testListStruct + "," + testListInterface + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        this.completecondition._resolve(mgr, errors);
        this.NullableRefTaskid = mgr.getTaskTaskExtraExp(this.taskid);
        this.NullableRefNexttask = mgr.getTaskTask(this.nexttask);

    }

    public static Task2 _create(configgen.genjava.ConfigInput input) {
        return new Task2(input);
    }

    public static @Nullable Task2 get(int taskid) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getTaskTask2(taskid);
    }

    public static java.util.Collection<Task2> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allTaskTask2();
    }


}

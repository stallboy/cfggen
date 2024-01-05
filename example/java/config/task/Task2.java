package config.task;

public class Task2 {
    private int taskid;
    private java.util.List<String> name;
    private int nexttask;
    private config.task.completecondition.Completecondition completecondition;
    private int exp;
    private boolean testBool;
    private String testString;
    private config.Position testStruct;
    private java.util.List<Integer> testList;
    private java.util.List<config.Position> testListStruct;
    private java.util.List<config.ai.triggertick.TriggerTick> testListInterface;
    private config.task.Taskextraexp NullableRefTaskid;
    private config.task.Task NullableRefNexttask;

    private Task2() {
    }

    public static Task2 _create(configgen.genjava.ConfigInput input) {
        Task2 self = new Task2();
        self.taskid = input.readInt();
        self.name = new java.util.ArrayList<>();
        for (int c = input.readInt(); c > 0; c--) {
            self.name.add(input.readStr());
        }
        self.nexttask = input.readInt();
        self.completecondition = config.task.completecondition.Completecondition._create(input);
        self.exp = input.readInt();
        self.testBool = input.readBool();
        self.testString = input.readStr();
        self.testStruct = config.Position._create(input);
        self.testList = new java.util.ArrayList<>();
        for (int c = input.readInt(); c > 0; c--) {
            self.testList.add(input.readInt());
        }
        self.testListStruct = new java.util.ArrayList<>();
        for (int c = input.readInt(); c > 0; c--) {
            self.testListStruct.add(config.Position._create(input));
        }
        self.testListInterface = new java.util.ArrayList<>();
        for (int c = input.readInt(); c > 0; c--) {
            self.testListInterface.add(config.ai.triggertick.TriggerTick._create(input));
        }
        return self;
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

    public config.task.Taskextraexp nullableRefTaskid() {
        return NullableRefTaskid;
    }

    public config.task.Task nullableRefNexttask() {
        return NullableRefNexttask;
    }

    @Override
    public String toString() {
        return "(" + taskid + "," + name + "," + nexttask + "," + completecondition + "," + exp + "," + testBool + "," + testString + "," + testStruct + "," + testList + "," + testListStruct + "," + testListInterface + ")";
    }

    public void _resolve(config.ConfigMgr mgr) {
        completecondition._resolve(mgr);
        NullableRefTaskid = mgr.task_taskextraexp_All.get(taskid);
        NullableRefNexttask = mgr.task_task_All.get(nexttask);
    }

    public static Task2 get(int taskid) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getTaskTask2(taskid);
    }

    public static java.util.Collection<Task2> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allTaskTask2();
    }

    public static class _ConfigLoader implements config.ConfigLoader {

        @Override
        public void createAll(config.ConfigMgr mgr, configgen.genjava.ConfigInput input) {
            for (int c = input.readInt(); c > 0; c--) {
                Task2 self = Task2._create(input);
                mgr.task_task2_All.put(self.taskid, self);
            }
        }

        @Override
        public void resolveAll(config.ConfigMgr mgr) {
            for (Task2 e : mgr.task_task2_All.values()) {
                e._resolve(mgr);
            }
        }

    }

}

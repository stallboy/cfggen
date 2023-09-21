package config.task;

public class Task {
    private int taskid;
    private config.task.Taskextraexp NullableRefTaskid;
    private java.util.List<String> name;
    private int nexttask;
    private config.task.Task NullableRefNexttask;
    private config.task.Completecondition completecondition;
    private int exp;
    private config.task.TestDefaultBean testDefaultBean;

    private Task() {
    }

    public static Task _create(configgen.genjava.ConfigInput input) {
        Task self = new Task();
        self.taskid = input.readInt();
        self.name = new java.util.ArrayList<>();
        for (int c = input.readInt(); c > 0; c--) {
            self.name.add(input.readStr());
        }
        self.nexttask = input.readInt();
        self.completecondition = config.task.Completecondition._create(input);
        self.exp = input.readInt();
        self.testDefaultBean = config.task.TestDefaultBean._create(input);
        return self;
    }

    /**
     * 任务完成条件类型（id的范围为1-100）
     */
    public int getTaskid() {
        return taskid;
    }

    public config.task.Taskextraexp nullableRefTaskid() {
        return NullableRefTaskid;
    }

    /**
     * 程序用名字
     */
    public java.util.List<String> getName() {
        return name;
    }

    public int getNexttask() {
        return nexttask;
    }

    public config.task.Task nullableRefNexttask() {
        return NullableRefNexttask;
    }

    public config.task.Completecondition getCompletecondition() {
        return completecondition;
    }

    public int getExp() {
        return exp;
    }

    /**
     * 测试
     */
    public config.task.TestDefaultBean getTestDefaultBean() {
        return testDefaultBean;
    }

    @Override
    public String toString() {
        return "(" + taskid + "," + name + "," + nexttask + "," + completecondition + "," + exp + "," + testDefaultBean + ")";
    }

    public void _resolve(config.ConfigMgr mgr) {
        NullableRefTaskid = mgr.task_taskextraexp_All.get(taskid);
        NullableRefNexttask = mgr.task_task_All.get(nexttask);
        completecondition._resolve(mgr);
    }

    public static Task get(int taskid) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.task_task_All.get(taskid);
    }

    public static java.util.Collection<Task> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.task_task_All.values();
    }

    public static class _ConfigLoader implements config.ConfigLoader {

        @Override
        public void createAll(config.ConfigMgr mgr, configgen.genjava.ConfigInput input) {
            for (int c = input.readInt(); c > 0; c--) {
                Task self = Task._create(input);
                mgr.task_task_All.put(self.taskid, self);
            }
        }

        @Override
        public void resolveAll(config.ConfigMgr mgr) {
            for (Task e : mgr.task_task_All.values()) {
                e._resolve(mgr);
            }
        }

    }

}

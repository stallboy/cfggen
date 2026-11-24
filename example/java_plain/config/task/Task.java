package config.task;

import configgen.genjava.LoadValueErrs;

// 来自：task/task_任务.csv
public class Task {
    private final int taskid;
    private final java.util.List<String> name;
    private final int nexttask;
    private final config.task.completecondition.Completecondition completecondition;
    private final int exp;
    private final config.task.TestDefaultBean testDefaultBean;
    private config.task.TaskExtraExp NullableRefTaskid;
    private config.task.Task NullableRefNexttask;

    private Task(configgen.genjava.ConfigInput input) {
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
        this.testDefaultBean = config.task.TestDefaultBean._create(input);
    }

    /**
     * 任务完成条件类型（id的范围为1-100）
     */
    public int getTaskid() {
        return taskid;
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

    public config.task.completecondition.Completecondition getCompletecondition() {
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

    public config.task.TaskExtraExp nullableRefTaskid() {
        return NullableRefTaskid;
    }

    public config.task.Task nullableRefNexttask() {
        return NullableRefNexttask;
    }

    @Override
    public String toString() {
        return "(" + taskid + "," + name + "," + nexttask + "," + completecondition + "," + exp + "," + testDefaultBean + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        this.completecondition._resolve(mgr, errors);
        this.NullableRefTaskid = mgr.getTaskTaskExtraExp(this.taskid);
        this.NullableRefNexttask = mgr.getTaskTask(this.nexttask);

    }

    public static Task _create(configgen.genjava.ConfigInput input) {
        return new Task(input);
    }

    public static Task get(int taskid) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getTaskTask(taskid);
    }

    public static java.util.Collection<Task> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allTaskTask();
    }


}

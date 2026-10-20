package config.task;

import configgen.genjava.LoadValueErrs;

// 来自：task/task_任务.csv
public class CfgTask {
    private int taskid;
    private java.util.List<config.Text> name;
    private int nexttask;
    private config.task.completecondition.CfgCompletecondition completecondition;
    private int exp;
    private config.task.CfgTestDefaultBean testDefaultBean;
    private config.task.CfgTaskextraexp NullableRefTaskid;
    private config.task.CfgTask NullableRefNexttask;

    private CfgTask() {
    }

    public static CfgTask _create(configgen.genjava.ConfigInput input) {
        CfgTask self = new CfgTask();
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
        self.testDefaultBean = config.task.CfgTestDefaultBean._create(input);
        return self;
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

    /**
     * 测试
     */
    public config.task.CfgTestDefaultBean getTestDefaultBean() {
        return testDefaultBean;
    }

    public config.task.CfgTaskextraexp nullableRefTaskid() {
        return NullableRefTaskid;
    }

    public config.task.CfgTask nullableRefNexttask() {
        return NullableRefNexttask;
    }

    @Override
    public String toString() {
        return "(" + taskid + "," + name + "," + nexttask + "," + completecondition + "," + exp + "," + testDefaultBean + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        this.completecondition._resolve(mgr, errors);
        this.NullableRefTaskid = mgr.getTaskTaskextraexp(this.taskid);
        this.NullableRefNexttask = mgr.getTaskTask(this.nexttask);

    }

    public static CfgTask get(int taskid) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getTaskTask(taskid);
    }

    public static java.util.Collection<CfgTask> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allTaskTask();
    }


}

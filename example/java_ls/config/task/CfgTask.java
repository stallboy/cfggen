package config.task;

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
            int c = input.readInt();
            if (c == 0) {
                self.name = java.util.Collections.emptyList();
            } else {
                self.name = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.name.add(config.Text._create(input));
                }
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

    public void _resolveDirect(config.ConfigMgr mgr) {
        NullableRefTaskid = mgr.task_taskextraexp_All.get(taskid);
        NullableRefNexttask = mgr.task_task_All.get(nexttask);
    }

    public void _resolve(config.ConfigMgr mgr) {
        completecondition._resolve(mgr);
        _resolveDirect(mgr);
    }

    public static CfgTask get(int taskid) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getTaskTask(taskid);
    }

    public static java.util.Collection<CfgTask> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allTaskTask();
    }
    public static class _ConfigLoader implements config.ConfigLoader {

        @Override
        public void createAll(config.ConfigMgr mgr, configgen.genjava.ConfigInput input) {
            int c = input.readInt();
            mgr.task_task_All = new java.util.LinkedHashMap<>(c);
            for (; c > 0; c--) {
                CfgTask self = CfgTask._create(input);
                mgr.task_task_All.put(self.taskid, self);
            }
        }

        @Override
        public void resolveAll(config.ConfigMgr mgr) {
            for (CfgTask e : mgr.task_task_All.values()) {
                e._resolve(mgr);
            }
        }

    }

}

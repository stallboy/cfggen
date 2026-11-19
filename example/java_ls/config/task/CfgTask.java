package config.task;

import org.jspecify.annotations.Nullable;

import configgen.genjava.LoadValueErrs;

// 来自：task/task_任务.csv
public class CfgTask {
    private final int taskid;
    private final java.util.List<config.Text> name;
    private final int nexttask;
    private final config.task.completecondition.CfgCompletecondition completecondition;
    private final int exp;
    private final config.task.CfgTestDefaultBean testDefaultBean;
    private config.task.@Nullable CfgTask_extra_exp NullableRefTaskid;
    private config.task.@Nullable CfgTask NullableRefNexttask;

    private CfgTask(configgen.genjava.ConfigInput input) {
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
        this.testDefaultBean = config.task.CfgTestDefaultBean._create(input);
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

    public config.task.@Nullable CfgTask_extra_exp nullableRefTaskid() {
        return NullableRefTaskid;
    }

    public config.task.@Nullable CfgTask nullableRefNexttask() {
        return NullableRefNexttask;
    }

    @Override
    public String toString() {
        return "(" + taskid + "," + name + "," + nexttask + "," + completecondition + "," + exp + "," + testDefaultBean + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        this.completecondition._resolve(mgr, errors);
        this.NullableRefTaskid = mgr.getTaskTask_extra_exp(this.taskid);
        this.NullableRefNexttask = mgr.getTaskTask(this.nexttask);

    }

    public static CfgTask _create(configgen.genjava.ConfigInput input) {
        return new CfgTask(input);
    }

    public static @Nullable CfgTask get(int taskid) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getTaskTask(taskid);
    }

    public static java.util.Collection<CfgTask> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allTaskTask();
    }


}

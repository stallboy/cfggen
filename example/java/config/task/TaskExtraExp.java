package config.task;

import org.jspecify.annotations.Nullable;

// 来自：task/task_extra_exp.csv
public class TaskExtraExp {
    private final int taskid;
    private final int extraexp;
    private final String test1;
    private final String test2;
    private final String fielda;
    private final String fieldb;
    private final String fieldc;

    private TaskExtraExp(configgen.genjava.ConfigInput input) {
        this.taskid = input.readInt();
        this.extraexp = input.readInt();
        this.test1 = input.readStringInPool();
        this.test2 = input.readStringInPool();
        this.fielda = input.readStringInPool();
        this.fieldb = input.readStringInPool();
        this.fieldc = input.readStringInPool();
    }

    /**
     * 任务完成条件类型（id的范围为1-100）
     */
    public int getTaskid() {
        return taskid;
    }

    /**
     * 额外奖励经验
     */
    public int getExtraexp() {
        return extraexp;
    }

    public String getTest1() {
        return test1;
    }

    public String getTest2() {
        return test2;
    }

    public String getFielda() {
        return fielda;
    }

    public String getFieldb() {
        return fieldb;
    }

    public String getFieldc() {
        return fieldc;
    }

    @Override
    public String toString() {
        return "(" + taskid + "," + extraexp + "," + test1 + "," + test2 + "," + fielda + "," + fieldb + "," + fieldc + ")";
    }

    public static TaskExtraExp _create(configgen.genjava.ConfigInput input) {
        return new TaskExtraExp(input);
    }

    public static @Nullable TaskExtraExp get(int taskid) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getTaskTaskExtraExp(taskid);
    }

    public static java.util.Collection<TaskExtraExp> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allTaskTaskExtraExp();
    }


}

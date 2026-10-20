package config.task;

// 来自：task/task_extra_exp.csv
public class CfgTask_extra_exp {
    private int taskid;
    private int extraexp;
    private String test1;
    private String test2;
    private String fielda;
    private String fieldb;
    private String fieldc;
    private CfgTask_extra_exp() {
    }

    public static CfgTask_extra_exp _create(configgen.genjava.ConfigInput input) {
        CfgTask_extra_exp self = new CfgTask_extra_exp();
        self.taskid = input.readInt();
        self.extraexp = input.readInt();
        self.test1 = input.readStringInPool();
        self.test2 = input.readStringInPool();
        self.fielda = input.readStringInPool();
        self.fieldb = input.readStringInPool();
        self.fieldc = input.readStringInPool();
        return self;
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

    public static CfgTask_extra_exp get(int taskid) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getTaskTask_extra_exp(taskid);
    }

    public static java.util.Collection<CfgTask_extra_exp> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allTaskTask_extra_exp();
    }


}

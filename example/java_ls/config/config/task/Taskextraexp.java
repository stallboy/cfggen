package config.task;

public class Taskextraexp {
    private int taskid;
    private int extraexp;
    private String test1;
    private String test2;
    private String fielda;
    private String fieldb;
    private String fieldc;
    private String fieldd;

    private Taskextraexp() {
    }

    public static Taskextraexp _create(configgen.genjava.ConfigInput input) {
        Taskextraexp self = new Taskextraexp();
        self.taskid = input.readInt();
        self.extraexp = input.readInt();
        self.test1 = input.readStringInPool();
        self.test2 = input.readStringInPool();
        self.fielda = input.readStringInPool();
        self.fieldb = input.readStringInPool();
        self.fieldc = input.readStringInPool();
        self.fieldd = input.readStringInPool();
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

    public String getFieldd() {
        return fieldd;
    }

    @Override
    public String toString() {
        return "(" + taskid + "," + extraexp + "," + test1 + "," + test2 + "," + fielda + "," + fieldb + "," + fieldc + "," + fieldd + ")";
    }

    public static Taskextraexp get(int taskid) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getTaskTaskextraexp(taskid);
    }

    public static java.util.Collection<Taskextraexp> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allTaskTaskextraexp();
    }

    public static class _ConfigLoader implements config.ConfigLoader {

        @Override
        public void createAll(config.ConfigMgr mgr, configgen.genjava.ConfigInput input) {
            for (int c = input.readInt(); c > 0; c--) {
                Taskextraexp self = Taskextraexp._create(input);
                mgr.task_taskextraexp_All.put(self.taskid, self);
            }
        }

        @Override
        public void resolveAll(config.ConfigMgr mgr) {
            // no resolve
        }

    }

}

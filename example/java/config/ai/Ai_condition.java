package config.ai;

public class Ai_condition {
    private int iD;
    private String desc;
    private int formulaID;
    private java.util.List<Integer> argIList;
    private java.util.List<Integer> argSList;

    private Ai_condition() {
    }

    public static Ai_condition _create(configgen.genjava.ConfigInput input) {
        Ai_condition self = new Ai_condition();
        self.iD = input.readInt();
        self.desc = input.readStr();
        self.formulaID = input.readInt();
        self.argIList = new java.util.ArrayList<>();
        for (int c = input.readInt(); c > 0; c--) {
            self.argIList.add(input.readInt());
        }
        self.argSList = new java.util.ArrayList<>();
        for (int c = input.readInt(); c > 0; c--) {
            self.argSList.add(input.readInt());
        }
        return self;
    }

    /**
     * ID
     */
    public int getID() {
        return iD;
    }

    /**
     * 描述
     */
    public String getDesc() {
        return desc;
    }

    /**
     * 公式
     */
    public int getFormulaID() {
        return formulaID;
    }

    /**
     * 参数(int)1
     */
    public java.util.List<Integer> getArgIList() {
        return argIList;
    }

    /**
     * 参数(string)1
     */
    public java.util.List<Integer> getArgSList() {
        return argSList;
    }

    @Override
    public String toString() {
        return "(" + iD + "," + desc + "," + formulaID + "," + argIList + "," + argSList + ")";
    }

    public static Ai_condition get(int iD) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.ai_ai_condition_All.get(iD);
    }

    public static java.util.Collection<Ai_condition> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.ai_ai_condition_All.values();
    }

    public static class _ConfigLoader implements config.ConfigLoader {

        @Override
        public void createAll(config.ConfigMgr mgr, configgen.genjava.ConfigInput input) {
            for (int c = input.readInt(); c > 0; c--) {
                Ai_condition self = Ai_condition._create(input);
                mgr.ai_ai_condition_All.put(self.iD, self);
            }
        }

        @Override
        public void resolveAll(config.ConfigMgr mgr) {
            // no resolve
        }

    }

}

package config.ai;

public class Ai_action {
    private int iD;
    private String desc;
    private int formulaID;
    private java.util.List<Integer> argIList;
    private java.util.List<Integer> argSList;

    private Ai_action() {
    }

    public static Ai_action _create(configgen.genjava.ConfigInput input) {
        Ai_action self = new Ai_action();
        self.iD = input.readInt();
        self.desc = input.readStringInPool();
        self.formulaID = input.readInt();
        {
            int c = input.readInt();
            if (c == 0) {
                self.argIList = java.util.Collections.emptyList();
            } else {
                self.argIList = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.argIList.add(input.readInt());
                }
            }
        }
        {
            int c = input.readInt();
            if (c == 0) {
                self.argSList = java.util.Collections.emptyList();
            } else {
                self.argSList = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.argSList.add(input.readInt());
                }
            }
        }
        return self;
    }

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

    public static Ai_action get(int iD) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getAiAi_action(iD);
    }

    public static java.util.Collection<Ai_action> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allAiAi_action();
    }

    public static class _ConfigLoader implements config.ConfigLoader {

        @Override
        public void createAll(config.ConfigMgr mgr, configgen.genjava.ConfigInput input) {
            for (int c = input.readInt(); c > 0; c--) {
                Ai_action self = Ai_action._create(input);
                mgr.ai_ai_action_All.put(self.iD, self);
            }
        }

        @Override
        public void resolveAll(config.ConfigMgr mgr) {
            // no resolve
        }

    }

}

package config.ai;

// 来自：ai_行为/ai行为.xlsx[AI_ACTION_中文测试], ai_行为/ai行为.xlsx[AI_ACTION_1_继续测试]
public class AiAction {
    private int iD;
    private String desc;
    private int formulaID;
    private java.util.List<Integer> argIList;
    private java.util.List<Integer> argSList;

    private AiAction() {
    }

    public static AiAction _create(configgen.genjava.ConfigInput input) {
        AiAction self = new AiAction();
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

    public static AiAction get(int iD) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getAiAiAction(iD);
    }

    public static java.util.Collection<AiAction> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allAiAiAction();
    }
    public static class _ConfigLoader implements config.ConfigLoader {

        @Override
        public void createAll(config.ConfigMgr mgr, configgen.genjava.ConfigInput input) {
            int c = input.readInt();
            mgr.ai_ai_action_All = new java.util.LinkedHashMap<>(c);
            for (; c > 0; c--) {
                AiAction self = AiAction._create(input);
                mgr.ai_ai_action_All.put(self.iD, self);
            }
        }

        @Override
        public void resolveAll(config.ConfigMgr mgr) {
            // no resolve
        }

    }

}

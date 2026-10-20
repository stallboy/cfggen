package config.ai;

// 来自：ai_行为/ai行为.xlsx[AI_ACTION_中文测试], ai_行为/ai行为.xlsx[AI_ACTION_1_继续测试]
public class CfgAi_action {
    private int iD;
    private String desc;
    private int formulaID;
    private java.util.List<Integer> argIList;
    private java.util.List<Integer> argSList;
    private CfgAi_action() {
    }

    public static CfgAi_action _create(configgen.genjava.ConfigInput input) {
        CfgAi_action self = new CfgAi_action();
        self.iD = input.readInt();
        self.desc = input.readStringInPool();
        self.formulaID = input.readInt();
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.argIList = java.util.Collections.emptyList();
            } else {
                self.argIList = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.argIList.add(input.readInt());
                }
                self.argIList = java.util.Collections.unmodifiableList(self.argIList);
            }
        }
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.argSList = java.util.Collections.emptyList();
            } else {
                self.argSList = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.argSList.add(input.readInt());
                }
                self.argSList = java.util.Collections.unmodifiableList(self.argSList);
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

    public static CfgAi_action get(int iD) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getAiAi_action(iD);
    }

    public static java.util.Collection<CfgAi_action> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allAiAi_action();
    }


}

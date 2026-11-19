package config.ai;

import org.jspecify.annotations.Nullable;

// 来自：ai_行为/ai行为.xlsx[AI_ACTION_中文测试], ai_行为/ai行为.xlsx[AI_ACTION_1_继续测试]
public class CfgAi_action {
    private final int iD;
    private final String desc;
    private final int formulaID;
    private final java.util.List<Integer> argIList;
    private final java.util.List<Integer> argSList;

    private CfgAi_action(configgen.genjava.ConfigInput input) {
        this.iD = input.readInt();
        this.desc = input.readStringInPool();
        this.formulaID = input.readInt();
        {
            int c = input.readCount(0);
            if (c == 0) {
                this.argIList = java.util.Collections.emptyList();
            } else {
                var items = new java.util.ArrayList<Integer>(c);
                for (; c > 0; c--) {
                    items.add(input.readInt());
                }
                this.argIList = java.util.Collections.unmodifiableList(items);
            }
        }
        {
            int c = input.readCount(0);
            if (c == 0) {
                this.argSList = java.util.Collections.emptyList();
            } else {
                var items = new java.util.ArrayList<Integer>(c);
                for (; c > 0; c--) {
                    items.add(input.readInt());
                }
                this.argSList = java.util.Collections.unmodifiableList(items);
            }
        }
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

    public static CfgAi_action _create(configgen.genjava.ConfigInput input) {
        return new CfgAi_action(input);
    }

    public static @Nullable CfgAi_action get(int iD) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getAiAi_action(iD);
    }

    public static java.util.Collection<CfgAi_action> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allAiAi_action();
    }


}

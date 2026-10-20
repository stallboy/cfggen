package config.ai;

// 来自：ai_行为/ai行为.xlsx[AI中文会被忽略只要a-z开头]
public class CfgAi {
    private int iD;
    private String desc;
    private String condID;
    private config.ai.triggertick.CfgTriggerTick trigTick;
    private int trigOdds;
    private java.util.List<Integer> actionID;
    private boolean deathRemove;
    private CfgAi() {
    }

    public static CfgAi _create(configgen.genjava.ConfigInput input) {
        CfgAi self = new CfgAi();
        self.iD = input.readInt();
        self.desc = input.readStringInPool();
        self.condID = input.readStringInPool();
        self.trigTick = config.ai.triggertick.CfgTriggerTick._create(input);
        self.trigOdds = input.readInt();
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.actionID = java.util.Collections.emptyList();
            } else {
                self.actionID = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.actionID.add(input.readInt());
                }
                self.actionID = java.util.Collections.unmodifiableList(self.actionID);
            }
        }
        self.deathRemove = input.readBool();
        return self;
    }

    public int getID() {
        return iD;
    }

    /**
     * 描述----这里测试下多行效果--再来一行
     */
    public String getDesc() {
        return desc;
    }

    /**
     * 触发公式
     */
    public String getCondID() {
        return condID;
    }

    /**
     * 触发间隔(帧)
     */
    public config.ai.triggertick.CfgTriggerTick getTrigTick() {
        return trigTick;
    }

    /**
     * 触发几率
     */
    public int getTrigOdds() {
        return trigOdds;
    }

    /**
     * 触发行为
     */
    public java.util.List<Integer> getActionID() {
        return actionID;
    }

    /**
     * 死亡移除
     */
    public boolean getDeathRemove() {
        return deathRemove;
    }

    @Override
    public String toString() {
        return "(" + iD + "," + desc + "," + condID + "," + trigTick + "," + trigOdds + "," + actionID + "," + deathRemove + ")";
    }

    public static CfgAi get(int iD) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getAiAi(iD);
    }

    public static java.util.Collection<CfgAi> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allAiAi();
    }


}

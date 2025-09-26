package config.ai;

public class Ai {
    private int iD;
    private String desc;
    private String condID;
    private config.ai.triggertick.TriggerTick trigTick;
    private int trigOdds;
    private java.util.List<Integer> actionID;
    private boolean deathRemove;
    private Ai() {
    }

    Ai(AiBuilder b) {
        this.iD = b.iD;
        this.desc = b.desc;
        this.condID = b.condID;
        this.trigTick = b.trigTick;
        this.trigOdds = b.trigOdds;
        this.actionID = b.actionID;
        this.deathRemove = b.deathRemove;
    }

    public static Ai _create(configgen.genjava.ConfigInput input) {
        Ai self = new Ai();
        self.iD = input.readInt();
        self.desc = input.readStr();
        self.condID = input.readStr();
        self.trigTick = config.ai.triggertick.TriggerTick._create(input);
        self.trigOdds = input.readInt();
        {
            int c = input.readInt();
            if (c == 0) {
                self.actionID = java.util.Collections.emptyList();
            } else {
                self.actionID = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.actionID.add(input.readInt());
                }
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
    public config.ai.triggertick.TriggerTick getTrigTick() {
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

    public static Ai get(int iD) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getAiAi(iD);
    }

    public static java.util.Collection<Ai> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allAiAi();
    }

    public static class _ConfigLoader implements config.ConfigLoader {

        @Override
        public void createAll(config.ConfigMgr mgr, configgen.genjava.ConfigInput input) {
            for (int c = input.readInt(); c > 0; c--) {
                Ai self = Ai._create(input);
                mgr.ai_ai_All.put(self.iD, self);
            }
        }

        @Override
        public void resolveAll(config.ConfigMgr mgr) {
            // no resolve
        }

    }

}

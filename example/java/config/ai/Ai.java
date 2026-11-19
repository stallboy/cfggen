package config.ai;

import org.jspecify.annotations.Nullable;

// 来自：ai_行为/ai行为.xlsx[AI中文会被忽略只要a-z开头]
public class Ai {
    private final int iD;
    private final String desc;
    private final String condID;
    private final config.ai.triggertick.TriggerTick trigTick;
    private final int trigOdds;
    private final java.util.List<Integer> actionID;
    private final boolean deathRemove;

    Ai(AiBuilder b) {
        this.iD = b.iD;
        this.desc = b.desc;
        this.condID = b.condID;
        this.trigTick = b.trigTick;
        this.trigOdds = b.trigOdds;
        this.actionID = b.actionID;
        this.deathRemove = b.deathRemove;
    }

    private Ai(configgen.genjava.ConfigInput input) {
        this.iD = input.readInt();
        this.desc = input.readStringInPool();
        this.condID = input.readStringInPool();
        this.trigTick = config.ai.triggertick.TriggerTick._create(input);
        this.trigOdds = input.readInt();
        {
            int c = input.readCount(0);
            if (c == 0) {
                this.actionID = java.util.Collections.emptyList();
            } else {
                var items = new java.util.ArrayList<Integer>(c);
                for (; c > 0; c--) {
                    items.add(input.readInt());
                }
                this.actionID = java.util.Collections.unmodifiableList(items);
            }
        }
        this.deathRemove = input.readBool();
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

    public static Ai _create(configgen.genjava.ConfigInput input) {
        return new Ai(input);
    }

    public static @Nullable Ai get(int iD) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getAiAi(iD);
    }

    public static java.util.Collection<Ai> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allAiAi();
    }


}

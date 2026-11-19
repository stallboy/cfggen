package config.ai;

public class AiBuilder {
    public int iD;
    public String desc = "";
    public String condID = "";
    public config.ai.triggertick.TriggerTick trigTick = new config.ai.triggertick.ConstValue(0);
    public int trigOdds;
    public java.util.List<Integer> actionID = new java.util.ArrayList<>();
    public boolean deathRemove;

    public Ai build() {
        return new Ai(this);
    }
}

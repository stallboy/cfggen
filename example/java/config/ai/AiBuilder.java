package config.ai;

public class AiBuilder {
    public int iD;
    public String desc;
    public String condID;
    public config.ai.triggertick.TriggerTick trigTick;
    public int trigOdds;
    public java.util.List<Integer> actionID;
    public boolean deathRemove;

    public Ai build() {
        java.util.Objects.requireNonNull(desc);
        java.util.Objects.requireNonNull(condID);
        java.util.Objects.requireNonNull(trigTick);
        java.util.Objects.requireNonNull(actionID);
        return new Ai(this);
    }

}

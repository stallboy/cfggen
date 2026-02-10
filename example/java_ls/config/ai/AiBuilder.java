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
        if (desc == null) {
            desc = "";
        }
        if (condID == null) {
            condID = "";
        }
        java.util.Objects.requireNonNull(trigTick);
        if (actionID == null) {
            actionID = new java.util.ArrayList<>();
        }
        return new Ai(this);
    }

}

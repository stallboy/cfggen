package config.ai;

public class AiBuilder {
    public int iD;
    public String desc;
    public String condID;
    public int trigTick;
    public int trigOdds;
    public String actionID;
    public boolean deathRemove;

    public Ai build() {
        java.util.Objects.requireNonNull(desc);
        java.util.Objects.requireNonNull(condID);
        java.util.Objects.requireNonNull(actionID);
        return new Ai(this);
    }

}

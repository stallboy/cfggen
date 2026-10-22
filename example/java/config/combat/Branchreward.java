package config.combat;

public class Branchreward {
    private config.combat.PhaseKey key;
    private config.combat.Reward reward;
    private Branchreward() {
    }

    public static Branchreward _create(configgen.genjava.ConfigInput input) {
        Branchreward self = new Branchreward();
        self.key = config.combat.PhaseKey._create(input);
        self.reward = config.combat.Reward._create(input);
        return self;
    }

    public config.combat.PhaseKey getKey() {
        return key;
    }

    public config.combat.Reward getReward() {
        return reward;
    }

    @Override
    public String toString() {
        return "(" + key + "," + reward + ")";
    }

    public static Branchreward get(config.combat.PhaseKey key) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getCombatBranchreward(key);
    }

    public static java.util.Collection<Branchreward> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allCombatBranchreward();
    }


}

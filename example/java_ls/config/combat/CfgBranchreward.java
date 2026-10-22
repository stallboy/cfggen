package config.combat;

public class CfgBranchreward {
    private config.combat.CfgPhaseKey key;
    private config.combat.CfgReward reward;
    private CfgBranchreward() {
    }

    public static CfgBranchreward _create(configgen.genjava.ConfigInput input) {
        CfgBranchreward self = new CfgBranchreward();
        self.key = config.combat.CfgPhaseKey._create(input);
        self.reward = config.combat.CfgReward._create(input);
        return self;
    }

    public config.combat.CfgPhaseKey getKey() {
        return key;
    }

    public config.combat.CfgReward getReward() {
        return reward;
    }

    @Override
    public String toString() {
        return "(" + key + "," + reward + ")";
    }

    public static CfgBranchreward get(config.combat.CfgPhaseKey key) {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.getCombatBranchreward(key);
    }

    public static java.util.Collection<CfgBranchreward> all() {
        config.ConfigMgr mgr = config.ConfigMgr.getMgr();
        return mgr.allCombatBranchreward();
    }


}

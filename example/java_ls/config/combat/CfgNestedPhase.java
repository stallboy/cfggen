package config.combat;

import configgen.genjava.LoadValueErrs;

public class CfgNestedPhase {
    private String key;
    private config.combat.command.CfgCommand action;
    private CfgNestedPhase() {
    }

    public CfgNestedPhase(String key, config.combat.command.CfgCommand action) {
        this.key = key;
        this.action = action;
    }

    public static CfgNestedPhase _create(configgen.genjava.ConfigInput input) {
        CfgNestedPhase self = new CfgNestedPhase();
        self.key = input.readStringInPool();
        self.action = config.combat.command.CfgCommand._create(input);
        return self;
    }

    public String getKey() {
        return key;
    }

    public config.combat.command.CfgCommand getAction() {
        return action;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(key, action);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CfgNestedPhase o)) {
            return false;
        }
        return key.equals(o.key) && action.equals(o.action);
    }

    @Override
    public String toString() {
        return "(" + key + "," + action + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        this.action._resolve(mgr, errors);
    }

}

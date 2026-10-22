package config.combat;

import configgen.genjava.LoadValueErrs;

public class NestedPhase {
    private String key;
    private config.combat.command.Command action;
    private NestedPhase() {
    }

    public NestedPhase(String key, config.combat.command.Command action) {
        this.key = key;
        this.action = action;
    }

    public static NestedPhase _create(configgen.genjava.ConfigInput input) {
        NestedPhase self = new NestedPhase();
        self.key = input.readStringInPool();
        self.action = config.combat.command.Command._create(input);
        return self;
    }

    public String getKey() {
        return key;
    }

    public config.combat.command.Command getAction() {
        return action;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(key, action);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof NestedPhase o)) {
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

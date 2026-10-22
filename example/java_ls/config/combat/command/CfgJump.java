package config.combat.command;

import configgen.genjava.LoadValueErrs;

public final class CfgJump implements config.combat.command.CfgCommand {
    private String key;
    private config.combat.CfgNestedPhase NullableRefKey;

    public void _bindNullableRefKey(config.combat.CfgNestedPhase value) {
        this.NullableRefKey = value;
    }
    private CfgJump() {
    }

    public CfgJump(String key) {
        this.key = key;
    }

    public static CfgJump _create(configgen.genjava.ConfigInput input) {
        CfgJump self = new CfgJump();
        self.key = input.readStringInPool();
        return self;
    }

    public String getKey() {
        return key;
    }

    public config.combat.CfgNestedPhase nullableRefKey() {
        return NullableRefKey;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(key);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CfgJump o)) {
            return false;
        }
        return key.equals(o.key);
    }

    @Override
    public String toString() {
        return "CfgJump(" + key + ")";
    }

    @Override
    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {

    }

}

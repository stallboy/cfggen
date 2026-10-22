package config.combat.command;

import configgen.genjava.LoadValueErrs;

public final class Jump implements config.combat.command.Command {
    private String key;
    private config.combat.NestedPhase NullableRefKey;

    public void _bindNullableRefKey(config.combat.NestedPhase value) {
        this.NullableRefKey = value;
    }
    private Jump() {
    }

    public Jump(String key) {
        this.key = key;
    }

    public static Jump _create(configgen.genjava.ConfigInput input) {
        Jump self = new Jump();
        self.key = input.readStringInPool();
        return self;
    }

    public String getKey() {
        return key;
    }

    public config.combat.NestedPhase nullableRefKey() {
        return NullableRefKey;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(key);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Jump o)) {
            return false;
        }
        return key.equals(o.key);
    }

    @Override
    public String toString() {
        return "Jump(" + key + ")";
    }

    @Override
    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {

    }

}

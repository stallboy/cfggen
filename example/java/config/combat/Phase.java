package config.combat;

import configgen.genjava.LoadValueErrs;

public class Phase {
    private String key;
    private String next;
    private config.combat.Phase NullableRefNext;
    private config.combat.effect.Effect RefExit;

    public void _bindNullableRefNext(config.combat.Phase value) {
        this.NullableRefNext = value;
    }
    public void _bindRefExit(config.combat.effect.Effect value) {
        this.RefExit = value;
    }
    private Phase() {
    }

    public Phase(String key, String next) {
        this.key = key;
        this.next = next;
    }

    public static Phase _create(configgen.genjava.ConfigInput input) {
        Phase self = new Phase();
        self.key = input.readStringInPool();
        self.next = input.readStringInPool();
        return self;
    }

    public String getKey() {
        return key;
    }

    public String getNext() {
        return next;
    }

    public config.combat.Phase nullableRefNext() {
        return NullableRefNext;
    }

    public config.combat.effect.Effect refExit() {
        return RefExit;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(key, next);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof Phase o)) {
            return false;
        }
        return key.equals(o.key) && next.equals(o.next);
    }

    @Override
    public String toString() {
        return "(" + key + "," + next + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {

    }

}

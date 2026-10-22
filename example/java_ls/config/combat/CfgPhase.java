package config.combat;

import configgen.genjava.LoadValueErrs;

public class CfgPhase {
    private String key;
    private String next;
    private config.combat.CfgPhase NullableRefNext;
    private config.combat.effect.CfgEffect RefExit;

    public void _bindNullableRefNext(config.combat.CfgPhase value) {
        this.NullableRefNext = value;
    }
    public void _bindRefExit(config.combat.effect.CfgEffect value) {
        this.RefExit = value;
    }
    private CfgPhase() {
    }

    public CfgPhase(String key, String next) {
        this.key = key;
        this.next = next;
    }

    public static CfgPhase _create(configgen.genjava.ConfigInput input) {
        CfgPhase self = new CfgPhase();
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

    public config.combat.CfgPhase nullableRefNext() {
        return NullableRefNext;
    }

    public config.combat.effect.CfgEffect refExit() {
        return RefExit;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(key, next);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CfgPhase o)) {
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

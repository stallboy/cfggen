package config.combat;

import configgen.genjava.LoadValueErrs;

public class CfgPhaseUse {
    private String phaseKey;
    private config.combat.CfgPhase RefTarget;
    private config.combat.CfgPhase RefByCode;
    private config.combat.CfgOrigin RefOrigin;

    public void _bindRefTarget(config.combat.CfgPhase value) {
        this.RefTarget = value;
    }
    public void _bindRefByCode(config.combat.CfgPhase value) {
        this.RefByCode = value;
    }
    public void _bindRefOrigin(config.combat.CfgOrigin value) {
        this.RefOrigin = value;
    }
    private CfgPhaseUse() {
    }

    public CfgPhaseUse(String phaseKey) {
        this.phaseKey = phaseKey;
    }

    public static CfgPhaseUse _create(configgen.genjava.ConfigInput input) {
        CfgPhaseUse self = new CfgPhaseUse();
        self.phaseKey = input.readStringInPool();
        return self;
    }

    public String getPhaseKey() {
        return phaseKey;
    }

    public config.combat.CfgPhase refTarget() {
        return RefTarget;
    }

    public config.combat.CfgPhase refByCode() {
        return RefByCode;
    }

    public config.combat.CfgOrigin refOrigin() {
        return RefOrigin;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(phaseKey);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CfgPhaseUse o)) {
            return false;
        }
        return phaseKey.equals(o.phaseKey);
    }

    @Override
    public String toString() {
        return "(" + phaseKey + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {

    }

}

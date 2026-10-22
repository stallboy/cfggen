package config.combat;

import configgen.genjava.LoadValueErrs;

public class PhaseUse {
    private String phaseKey;
    private config.combat.Phase RefTarget;
    private config.combat.Phase RefByCode;
    private config.combat.Origin RefOrigin;

    public void _bindRefTarget(config.combat.Phase value) {
        this.RefTarget = value;
    }
    public void _bindRefByCode(config.combat.Phase value) {
        this.RefByCode = value;
    }
    public void _bindRefOrigin(config.combat.Origin value) {
        this.RefOrigin = value;
    }
    private PhaseUse() {
    }

    public PhaseUse(String phaseKey) {
        this.phaseKey = phaseKey;
    }

    public static PhaseUse _create(configgen.genjava.ConfigInput input) {
        PhaseUse self = new PhaseUse();
        self.phaseKey = input.readStringInPool();
        return self;
    }

    public String getPhaseKey() {
        return phaseKey;
    }

    public config.combat.Phase refTarget() {
        return RefTarget;
    }

    public config.combat.Phase refByCode() {
        return RefByCode;
    }

    public config.combat.Origin refOrigin() {
        return RefOrigin;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(phaseKey);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof PhaseUse o)) {
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

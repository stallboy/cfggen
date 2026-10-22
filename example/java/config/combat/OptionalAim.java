package config.combat;

import configgen.genjava.LoadValueErrs;

public class OptionalAim {
    private int abilityId;
    private String machineKey;
    private String phaseKey;
    private config.combat.Phase NullableRefTarget;

    public void _bindNullableRefTarget(config.combat.Phase value) {
        this.NullableRefTarget = value;
    }
    private OptionalAim() {
    }

    public OptionalAim(int abilityId, String machineKey, String phaseKey) {
        this.abilityId = abilityId;
        this.machineKey = machineKey;
        this.phaseKey = phaseKey;
    }

    public static OptionalAim _create(configgen.genjava.ConfigInput input) {
        OptionalAim self = new OptionalAim();
        self.abilityId = input.readInt();
        self.machineKey = input.readStringInPool();
        self.phaseKey = input.readStringInPool();
        return self;
    }

    public int getAbilityId() {
        return abilityId;
    }

    public String getMachineKey() {
        return machineKey;
    }

    public String getPhaseKey() {
        return phaseKey;
    }

    public config.combat.Phase nullableRefTarget() {
        return NullableRefTarget;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(abilityId, machineKey, phaseKey);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof OptionalAim o)) {
            return false;
        }
        return abilityId == o.abilityId && machineKey.equals(o.machineKey) && phaseKey.equals(o.phaseKey);
    }

    @Override
    public String toString() {
        return "(" + abilityId + "," + machineKey + "," + phaseKey + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {

    }

}

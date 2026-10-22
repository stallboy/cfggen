package config.combat;

import configgen.genjava.LoadValueErrs;

public class CfgMachineUse {
    private String machineKey;
    private java.util.List<config.combat.CfgPhaseUse> targets;
    private CfgMachineUse() {
    }

    public CfgMachineUse(String machineKey, java.util.List<config.combat.CfgPhaseUse> targets) {
        this.machineKey = machineKey;
        this.targets = targets;
    }

    public static CfgMachineUse _create(configgen.genjava.ConfigInput input) {
        CfgMachineUse self = new CfgMachineUse();
        self.machineKey = input.readStringInPool();
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.targets = java.util.Collections.emptyList();
            } else {
                self.targets = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.targets.add(config.combat.CfgPhaseUse._create(input));
                }
                self.targets = java.util.Collections.unmodifiableList(self.targets);
            }
        }
        return self;
    }

    public String getMachineKey() {
        return machineKey;
    }

    public java.util.List<config.combat.CfgPhaseUse> getTargets() {
        return targets;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(machineKey, targets);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CfgMachineUse o)) {
            return false;
        }
        return machineKey.equals(o.machineKey) && targets.equals(o.targets);
    }

    @Override
    public String toString() {
        return "(" + machineKey + "," + targets + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        for (config.combat.CfgPhaseUse e : this.targets) {
            e._resolve(mgr, errors);
        }
    }

}

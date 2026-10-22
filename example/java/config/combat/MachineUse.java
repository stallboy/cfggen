package config.combat;

import configgen.genjava.LoadValueErrs;

public class MachineUse {
    private String machineKey;
    private java.util.List<config.combat.PhaseUse> targets;
    private MachineUse() {
    }

    public MachineUse(String machineKey, java.util.List<config.combat.PhaseUse> targets) {
        this.machineKey = machineKey;
        this.targets = targets;
    }

    public static MachineUse _create(configgen.genjava.ConfigInput input) {
        MachineUse self = new MachineUse();
        self.machineKey = input.readStringInPool();
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.targets = java.util.Collections.emptyList();
            } else {
                self.targets = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.targets.add(config.combat.PhaseUse._create(input));
                }
                self.targets = java.util.Collections.unmodifiableList(self.targets);
            }
        }
        return self;
    }

    public String getMachineKey() {
        return machineKey;
    }

    public java.util.List<config.combat.PhaseUse> getTargets() {
        return targets;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(machineKey, targets);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MachineUse o)) {
            return false;
        }
        return machineKey.equals(o.machineKey) && targets.equals(o.targets);
    }

    @Override
    public String toString() {
        return "(" + machineKey + "," + targets + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        for (config.combat.PhaseUse e : this.targets) {
            e._resolve(mgr, errors);
        }
    }

}

package config.combat;

import configgen.genjava.LoadValueErrs;

public class CfgAbilityUse {
    private config.combat.CfgBinding binding;
    private java.util.List<config.combat.CfgMachineUse> machines;
    private config.combat.CfgAbility RefAbility;

    public void _bindRefAbility(config.combat.CfgAbility value) {
        this.RefAbility = value;
    }
    private CfgAbilityUse() {
    }

    public CfgAbilityUse(config.combat.CfgBinding binding, java.util.List<config.combat.CfgMachineUse> machines) {
        this.binding = binding;
        this.machines = machines;
    }

    public static CfgAbilityUse _create(configgen.genjava.ConfigInput input) {
        CfgAbilityUse self = new CfgAbilityUse();
        self.binding = config.combat.CfgBinding._create(input);
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.machines = java.util.Collections.emptyList();
            } else {
                self.machines = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.machines.add(config.combat.CfgMachineUse._create(input));
                }
                self.machines = java.util.Collections.unmodifiableList(self.machines);
            }
        }
        return self;
    }

    public config.combat.CfgBinding getBinding() {
        return binding;
    }

    public java.util.List<config.combat.CfgMachineUse> getMachines() {
        return machines;
    }

    public config.combat.CfgAbility refAbility() {
        return RefAbility;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(binding, machines);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CfgAbilityUse o)) {
            return false;
        }
        return binding.equals(o.binding) && machines.equals(o.machines);
    }

    @Override
    public String toString() {
        return "(" + binding + "," + machines + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        for (config.combat.CfgMachineUse e : this.machines) {
            e._resolve(mgr, errors);
        }

    }

}

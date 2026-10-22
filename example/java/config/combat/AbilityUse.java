package config.combat;

import configgen.genjava.LoadValueErrs;

public class AbilityUse {
    private config.combat.Binding binding;
    private java.util.List<config.combat.MachineUse> machines;
    private config.combat.Ability RefAbility;

    public void _bindRefAbility(config.combat.Ability value) {
        this.RefAbility = value;
    }
    private AbilityUse() {
    }

    public AbilityUse(config.combat.Binding binding, java.util.List<config.combat.MachineUse> machines) {
        this.binding = binding;
        this.machines = machines;
    }

    public static AbilityUse _create(configgen.genjava.ConfigInput input) {
        AbilityUse self = new AbilityUse();
        self.binding = config.combat.Binding._create(input);
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.machines = java.util.Collections.emptyList();
            } else {
                self.machines = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.machines.add(config.combat.MachineUse._create(input));
                }
                self.machines = java.util.Collections.unmodifiableList(self.machines);
            }
        }
        return self;
    }

    public config.combat.Binding getBinding() {
        return binding;
    }

    public java.util.List<config.combat.MachineUse> getMachines() {
        return machines;
    }

    public config.combat.Ability refAbility() {
        return RefAbility;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(binding, machines);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AbilityUse o)) {
            return false;
        }
        return binding.equals(o.binding) && machines.equals(o.machines);
    }

    @Override
    public String toString() {
        return "(" + binding + "," + machines + ")";
    }

    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        for (config.combat.MachineUse e : this.machines) {
            e._resolve(mgr, errors);
        }

    }

}

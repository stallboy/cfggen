package config.combat.command;

import configgen.genjava.LoadValueErrs;

public final class CfgStateMachine implements config.combat.command.CfgCommand {
    private java.util.List<config.combat.CfgNestedPhase> phases;
    private CfgStateMachine() {
    }

    public CfgStateMachine(java.util.List<config.combat.CfgNestedPhase> phases) {
        this.phases = phases;
    }

    public static CfgStateMachine _create(configgen.genjava.ConfigInput input) {
        CfgStateMachine self = new CfgStateMachine();
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.phases = java.util.Collections.emptyList();
            } else {
                self.phases = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.phases.add(config.combat.CfgNestedPhase._create(input));
                }
                self.phases = java.util.Collections.unmodifiableList(self.phases);
            }
        }
        return self;
    }

    public java.util.List<config.combat.CfgNestedPhase> getPhases() {
        return phases;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(phases);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof CfgStateMachine o)) {
            return false;
        }
        return phases.equals(o.phases);
    }

    @Override
    public String toString() {
        return "CfgStateMachine(" + phases + ")";
    }

    @Override
    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        for (config.combat.CfgNestedPhase e : this.phases) {
            e._resolve(mgr, errors);
        }
    }

}

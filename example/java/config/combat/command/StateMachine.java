package config.combat.command;

import configgen.genjava.LoadValueErrs;

public final class StateMachine implements config.combat.command.Command {
    private java.util.List<config.combat.NestedPhase> phases;
    private StateMachine() {
    }

    public StateMachine(java.util.List<config.combat.NestedPhase> phases) {
        this.phases = phases;
    }

    public static StateMachine _create(configgen.genjava.ConfigInput input) {
        StateMachine self = new StateMachine();
        {
            int c = input.readCount(0);
            if (c == 0) {
                self.phases = java.util.Collections.emptyList();
            } else {
                self.phases = new java.util.ArrayList<>(c);
                for (; c > 0; c--) {
                    self.phases.add(config.combat.NestedPhase._create(input));
                }
                self.phases = java.util.Collections.unmodifiableList(self.phases);
            }
        }
        return self;
    }

    public java.util.List<config.combat.NestedPhase> getPhases() {
        return phases;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(phases);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof StateMachine o)) {
            return false;
        }
        return phases.equals(o.phases);
    }

    @Override
    public String toString() {
        return "StateMachine(" + phases + ")";
    }

    @Override
    public void _resolve(config.ConfigMgr mgr, LoadValueErrs errors) {
        for (config.combat.NestedPhase e : this.phases) {
            e._resolve(mgr, errors);
        }
    }

}

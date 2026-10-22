package config

import "fmt"

type CombatCommandStateMachine struct {
    phases []*CombatNestedPhase
}

func createCombatCommandStateMachine(stream *Stream) *CombatCommandStateMachine {
    v := &CombatCommandStateMachine{}
    phasesSize := stream.ReadCount(0)
    v.phases = make([]*CombatNestedPhase, phasesSize)
    for i := 0; i < phasesSize; i++ {
        v.phases[i] = createCombatNestedPhase(stream)
    }
    return v
}

func (t *CombatCommandStateMachine) String() string {
    return fmt.Sprintf("CombatCommandStateMachine{phases=%v}", t.phases)
}

//getters
func (t *CombatCommandStateMachine) Phases() []*CombatNestedPhase {
    return t.phases
}


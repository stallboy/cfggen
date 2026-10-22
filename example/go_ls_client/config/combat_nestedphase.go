package config

import "fmt"

type CombatNestedPhase struct {
    key string
    action CombatCommand
}

func createCombatNestedPhase(stream *Stream) *CombatNestedPhase {
    v := &CombatNestedPhase{}
    v.key = stream.ReadStringInPool()
    v.action = createCombatCommand(stream)
    return v
}

func (t *CombatNestedPhase) String() string {
    return fmt.Sprintf("CombatNestedPhase{key=%v, action=%v}", t.key, t.action)
}

//getters
func (t *CombatNestedPhase) Key() string {
    return t.key
}

func (t *CombatNestedPhase) Action() CombatCommand {
    return t.action
}


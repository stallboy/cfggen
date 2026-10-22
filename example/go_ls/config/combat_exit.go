package config

import "fmt"

type CombatExit struct {
    effect CombatEffect
}

func createCombatExit(stream *Stream) *CombatExit {
    v := &CombatExit{}
    v.effect = createCombatEffect(stream)
    return v
}

func (t *CombatExit) String() string {
    return fmt.Sprintf("CombatExit{effect=%v}", t.effect)
}

//getters
func (t *CombatExit) Effect() CombatEffect {
    return t.effect
}


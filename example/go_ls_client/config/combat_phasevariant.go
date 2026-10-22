package config

import "fmt"

type CombatPhaseVariant struct {
    key *CombatPhaseKey
    effect CombatEffect
}

func createCombatPhaseVariant(stream *Stream) *CombatPhaseVariant {
    v := &CombatPhaseVariant{}
    v.key = createCombatPhaseKey(stream)
    v.effect = createCombatEffect(stream)
    return v
}

func (t *CombatPhaseVariant) String() string {
    return fmt.Sprintf("CombatPhaseVariant{key=%v, effect=%v}", t.key, t.effect)
}

//getters
func (t *CombatPhaseVariant) Key() *CombatPhaseKey {
    return t.key
}

func (t *CombatPhaseVariant) Effect() CombatEffect {
    return t.effect
}


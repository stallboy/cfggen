package config

import "fmt"

type CombatStance struct {
    airborne bool
    effect CombatEffect
}

func createCombatStance(stream *Stream) *CombatStance {
    v := &CombatStance{}
    v.airborne = stream.ReadBool()
    v.effect = createCombatEffect(stream)
    return v
}

func (t *CombatStance) String() string {
    return fmt.Sprintf("CombatStance{airborne=%v, effect=%v}", t.airborne, t.effect)
}

//getters
func (t *CombatStance) Airborne() bool {
    return t.airborne
}

func (t *CombatStance) Effect() CombatEffect {
    return t.effect
}


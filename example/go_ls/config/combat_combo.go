package config

import "fmt"

type CombatCombo struct {
    weapon string
    tier int32
    airborne bool
    effect CombatEffect
}

func createCombatCombo(stream *Stream) *CombatCombo {
    v := &CombatCombo{}
    v.weapon = stream.ReadStringInPool()
    v.tier = stream.ReadInt32()
    v.airborne = stream.ReadBool()
    v.effect = createCombatEffect(stream)
    return v
}

func (t *CombatCombo) String() string {
    return fmt.Sprintf("CombatCombo{weapon=%v, tier=%v, airborne=%v, effect=%v}", t.weapon, t.tier, t.airborne, t.effect)
}

//getters
func (t *CombatCombo) Weapon() string {
    return t.weapon
}

func (t *CombatCombo) Tier() int32 {
    return t.tier
}

func (t *CombatCombo) Airborne() bool {
    return t.airborne
}

func (t *CombatCombo) Effect() CombatEffect {
    return t.effect
}


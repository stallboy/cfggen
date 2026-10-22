package config

import "fmt"

type CombatEffectDamage struct {
    amount int32
}

func createCombatEffectDamage(stream *Stream) *CombatEffectDamage {
    v := &CombatEffectDamage{}
    v.amount = stream.ReadInt32()
    return v
}

func (t *CombatEffectDamage) String() string {
    return fmt.Sprintf("CombatEffectDamage{amount=%v}", t.amount)
}

//getters
func (t *CombatEffectDamage) Amount() int32 {
    return t.amount
}


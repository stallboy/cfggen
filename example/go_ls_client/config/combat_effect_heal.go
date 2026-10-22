package config

import "fmt"

type CombatEffectHeal struct {
    amount int32
}

func createCombatEffectHeal(stream *Stream) *CombatEffectHeal {
    v := &CombatEffectHeal{}
    v.amount = stream.ReadInt32()
    return v
}

func (t *CombatEffectHeal) String() string {
    return fmt.Sprintf("CombatEffectHeal{amount=%v}", t.amount)
}

//getters
func (t *CombatEffectHeal) Amount() int32 {
    return t.amount
}


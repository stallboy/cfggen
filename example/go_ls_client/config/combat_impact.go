package config

import "fmt"

type CombatImpact struct {
    ordinal int32
    damage int32
}

func createCombatImpact(stream *Stream) *CombatImpact {
    v := &CombatImpact{}
    v.ordinal = stream.ReadInt32()
    v.damage = stream.ReadInt32()
    return v
}

func (t *CombatImpact) String() string {
    return fmt.Sprintf("CombatImpact{ordinal=%v, damage=%v}", t.ordinal, t.damage)
}

//getters
func (t *CombatImpact) Ordinal() int32 {
    return t.ordinal
}

func (t *CombatImpact) Damage() int32 {
    return t.damage
}


package config

import "fmt"

type CombatPhaseKey struct {
    name string
    variant int32
}

func createCombatPhaseKey(stream *Stream) *CombatPhaseKey {
    v := &CombatPhaseKey{}
    v.name = stream.ReadStringInPool()
    v.variant = stream.ReadInt32()
    return v
}

func (t *CombatPhaseKey) String() string {
    return fmt.Sprintf("CombatPhaseKey{name=%v, variant=%v}", t.name, t.variant)
}

//getters
func (t *CombatPhaseKey) Name() string {
    return t.name
}

func (t *CombatPhaseKey) Variant() int32 {
    return t.variant
}


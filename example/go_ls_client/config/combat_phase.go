package config

import "fmt"

type CombatPhase struct {
    key string
    next string
    nullableRefNext *CombatPhase
    refExit CombatEffect
}

func createCombatPhase(stream *Stream) *CombatPhase {
    v := &CombatPhase{}
    v.key = stream.ReadStringInPool()
    v.next = stream.ReadStringInPool()
    return v
}

func (t *CombatPhase) String() string {
    return fmt.Sprintf("CombatPhase{key=%v, next=%v}", t.key, t.next)
}

//getters
func (t *CombatPhase) Key() string {
    return t.key
}

func (t *CombatPhase) Next() string {
    return t.next
}

func (t *CombatPhase) NullableRefNext() *CombatPhase {
    return t.nullableRefNext
}
func (t *CombatPhase) RefExit() CombatEffect {
    return t.refExit
}

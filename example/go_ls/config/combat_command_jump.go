package config

import "fmt"

type CombatCommandJump struct {
    key string
    nullableRefKey *CombatNestedPhase
}

func createCombatCommandJump(stream *Stream) *CombatCommandJump {
    v := &CombatCommandJump{}
    v.key = stream.ReadStringInPool()
    return v
}

func (t *CombatCommandJump) String() string {
    return fmt.Sprintf("CombatCommandJump{key=%v}", t.key)
}

//getters
func (t *CombatCommandJump) Key() string {
    return t.key
}

func (t *CombatCommandJump) NullableRefKey() *CombatNestedPhase {
    return t.nullableRefKey
}

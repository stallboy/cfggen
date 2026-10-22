package config

import "fmt"

type CombatMarker struct {
    key string
}

func createCombatMarker(stream *Stream) *CombatMarker {
    v := &CombatMarker{}
    v.key = stream.ReadStringInPool()
    return v
}

func (t *CombatMarker) String() string {
    return fmt.Sprintf("CombatMarker{key=%v}", t.key)
}

//getters
func (t *CombatMarker) Key() string {
    return t.key
}


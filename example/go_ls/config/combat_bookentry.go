package config

import "fmt"

type CombatBookEntry struct {
    name string
}

func createCombatBookEntry(stream *Stream) *CombatBookEntry {
    v := &CombatBookEntry{}
    v.name = stream.ReadStringInPool()
    return v
}

func (t *CombatBookEntry) String() string {
    return fmt.Sprintf("CombatBookEntry{name=%v}", t.name)
}

//getters
func (t *CombatBookEntry) Name() string {
    return t.name
}


package config

import "fmt"

type CombatBinding struct {
    abilityId int32
}

func createCombatBinding(stream *Stream) *CombatBinding {
    v := &CombatBinding{}
    v.abilityId = stream.ReadInt32()
    return v
}

func (t *CombatBinding) String() string {
    return fmt.Sprintf("CombatBinding{abilityId=%v}", t.abilityId)
}

//getters
func (t *CombatBinding) AbilityId() int32 {
    return t.abilityId
}


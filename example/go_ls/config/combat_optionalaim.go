package config

import "fmt"

type CombatOptionalAim struct {
    abilityId int32
    machineKey string
    phaseKey string
    nullableRefTarget *CombatPhase
}

func createCombatOptionalAim(stream *Stream) *CombatOptionalAim {
    v := &CombatOptionalAim{}
    v.abilityId = stream.ReadInt32()
    v.machineKey = stream.ReadStringInPool()
    v.phaseKey = stream.ReadStringInPool()
    return v
}

func (t *CombatOptionalAim) String() string {
    return fmt.Sprintf("CombatOptionalAim{abilityId=%v, machineKey=%v, phaseKey=%v}", t.abilityId, t.machineKey, t.phaseKey)
}

//getters
func (t *CombatOptionalAim) AbilityId() int32 {
    return t.abilityId
}

func (t *CombatOptionalAim) MachineKey() string {
    return t.machineKey
}

func (t *CombatOptionalAim) PhaseKey() string {
    return t.phaseKey
}

func (t *CombatOptionalAim) NullableRefTarget() *CombatPhase {
    return t.nullableRefTarget
}

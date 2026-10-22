package config

import "fmt"

type CombatPhaseUse struct {
    phaseKey string
    refTarget *CombatPhase
    refByCode *CombatPhase
    refOrigin *CombatOrigin
}

func createCombatPhaseUse(stream *Stream) *CombatPhaseUse {
    v := &CombatPhaseUse{}
    v.phaseKey = stream.ReadStringInPool()
    return v
}

func (t *CombatPhaseUse) String() string {
    return fmt.Sprintf("CombatPhaseUse{phaseKey=%v}", t.phaseKey)
}

//getters
func (t *CombatPhaseUse) PhaseKey() string {
    return t.phaseKey
}

func (t *CombatPhaseUse) RefTarget() *CombatPhase {
    return t.refTarget
}
func (t *CombatPhaseUse) RefByCode() *CombatPhase {
    return t.refByCode
}
func (t *CombatPhaseUse) RefOrigin() *CombatOrigin {
    return t.refOrigin
}

package config

import "fmt"

type CombatMachineUse struct {
    machineKey string
    targets []*CombatPhaseUse
}

func createCombatMachineUse(stream *Stream) *CombatMachineUse {
    v := &CombatMachineUse{}
    v.machineKey = stream.ReadStringInPool()
    targetsSize := stream.ReadCount(0)
    v.targets = make([]*CombatPhaseUse, targetsSize)
    for i := 0; i < targetsSize; i++ {
        v.targets[i] = createCombatPhaseUse(stream)
    }
    return v
}

func (t *CombatMachineUse) String() string {
    return fmt.Sprintf("CombatMachineUse{machineKey=%v, targets=%v}", t.machineKey, t.targets)
}

//getters
func (t *CombatMachineUse) MachineKey() string {
    return t.machineKey
}

func (t *CombatMachineUse) Targets() []*CombatPhaseUse {
    return t.targets
}


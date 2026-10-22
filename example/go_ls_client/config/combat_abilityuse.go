package config

import "fmt"

type CombatAbilityUse struct {
    binding *CombatBinding
    machines []*CombatMachineUse
    refAbility *CombatAbility
}

func createCombatAbilityUse(stream *Stream) *CombatAbilityUse {
    v := &CombatAbilityUse{}
    v.binding = createCombatBinding(stream)
    machinesSize := stream.ReadCount(0)
    v.machines = make([]*CombatMachineUse, machinesSize)
    for i := 0; i < machinesSize; i++ {
        v.machines[i] = createCombatMachineUse(stream)
    }
    return v
}

func (t *CombatAbilityUse) String() string {
    return fmt.Sprintf("CombatAbilityUse{binding=%v, machines=%v}", t.binding, t.machines)
}

//getters
func (t *CombatAbilityUse) Binding() *CombatBinding {
    return t.binding
}

func (t *CombatAbilityUse) Machines() []*CombatMachineUse {
    return t.machines
}

func (t *CombatAbilityUse) RefAbility() *CombatAbility {
    return t.refAbility
}

package config

import "fmt"

type CombatAbility struct {
    id int32
    code string
    origin *CombatOrigin
    machines []*CombatMachine
    startMachine string
    startPhase string
    refInitial *CombatPhase
    refSpawn *CombatOrigin
}

func createCombatAbility(stream *Stream) *CombatAbility {
    v := &CombatAbility{}
    v.id = stream.ReadInt32()
    v.code = stream.ReadStringInPool()
    v.origin = createCombatOrigin(stream)
    machinesSize := stream.ReadCount(0)
    v.machines = make([]*CombatMachine, machinesSize)
    for i := 0; i < machinesSize; i++ {
        v.machines[i] = createCombatMachine(stream)
    }
    v.startMachine = stream.ReadStringInPool()
    v.startPhase = stream.ReadStringInPool()
    return v
}

func (t *CombatAbility) String() string {
    return fmt.Sprintf("CombatAbility{id=%v, code=%v, origin=%v, machines=%v, startMachine=%v, startPhase=%v}", t.id, t.code, t.origin, t.machines, t.startMachine, t.startPhase)
}

//getters
func (t *CombatAbility) Id() int32 {
    return t.id
}

func (t *CombatAbility) Code() string {
    return t.code
}

func (t *CombatAbility) Origin() *CombatOrigin {
    return t.origin
}

func (t *CombatAbility) Machines() []*CombatMachine {
    return t.machines
}

func (t *CombatAbility) StartMachine() string {
    return t.startMachine
}

func (t *CombatAbility) StartPhase() string {
    return t.startPhase
}

func (t *CombatAbility) RefInitial() *CombatPhase {
    return t.refInitial
}
func (t *CombatAbility) RefSpawn() *CombatOrigin {
    return t.refSpawn
}
type CombatAbilityMgr struct {
    all []*CombatAbility
    idMap map[int32]*CombatAbility
    codeMap map[string]*CombatAbility
}

func(t *CombatAbilityMgr) GetAll() []*CombatAbility {
    return t.all
}

func(t *CombatAbilityMgr) Get(id int32) *CombatAbility {
    return t.idMap[id]
}

func(t *CombatAbilityMgr) GetBycode(code string) *CombatAbility {
    return t.codeMap[code]
}

func (t *CombatAbilityMgr) Init(stream *Stream) {
    cnt := int32(stream.ReadCount(1))
    t.all = make([]*CombatAbility, 0, cnt)
    t.idMap = make(map[int32]*CombatAbility, cnt)
    t.codeMap = make(map[string]*CombatAbility, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createCombatAbility(stream)
        t.all = append(t.all, v)
        t.idMap[v.id] = v
        t.codeMap[v.code] = v
    }
}

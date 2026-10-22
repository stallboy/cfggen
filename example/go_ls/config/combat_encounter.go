package config

import "fmt"

type CombatEncounter struct {
    id int32
    command CombatCommand
}

func createCombatEncounter(stream *Stream) *CombatEncounter {
    v := &CombatEncounter{}
    v.id = stream.ReadInt32()
    v.command = createCombatCommand(stream)
    return v
}

func (t *CombatEncounter) String() string {
    return fmt.Sprintf("CombatEncounter{id=%v, command=%v}", t.id, t.command)
}

//getters
func (t *CombatEncounter) Id() int32 {
    return t.id
}

func (t *CombatEncounter) Command() CombatCommand {
    return t.command
}

type CombatEncounterMgr struct {
    all []*CombatEncounter
    idMap map[int32]*CombatEncounter
}

func(t *CombatEncounterMgr) GetAll() []*CombatEncounter {
    return t.all
}

func(t *CombatEncounterMgr) Get(id int32) *CombatEncounter {
    return t.idMap[id]
}

func (t *CombatEncounterMgr) Init(stream *Stream) {
    cnt := int32(stream.ReadCount(1))
    t.all = make([]*CombatEncounter, 0, cnt)
    t.idMap = make(map[int32]*CombatEncounter, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createCombatEncounter(stream)
        t.all = append(t.all, v)
        t.idMap[v.id] = v
    }
}

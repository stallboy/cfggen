package config

import "fmt"

type CombatPhasebook struct {
    school string
    tier int32
    airborne bool
    code string
    edition int32
    entry *CombatBookEntry
}

func createCombatPhasebook(stream *Stream) *CombatPhasebook {
    v := &CombatPhasebook{}
    v.school = stream.ReadStringInPool()
    v.tier = stream.ReadInt32()
    v.airborne = stream.ReadBool()
    v.code = stream.ReadStringInPool()
    v.edition = stream.ReadInt32()
    v.entry = createCombatBookEntry(stream)
    return v
}

func (t *CombatPhasebook) String() string {
    return fmt.Sprintf("CombatPhasebook{school=%v, tier=%v, airborne=%v, code=%v, edition=%v, entry=%v}", t.school, t.tier, t.airborne, t.code, t.edition, t.entry)
}

//getters
func (t *CombatPhasebook) School() string {
    return t.school
}

func (t *CombatPhasebook) Tier() int32 {
    return t.tier
}

func (t *CombatPhasebook) Airborne() bool {
    return t.airborne
}

func (t *CombatPhasebook) Code() string {
    return t.code
}

func (t *CombatPhasebook) Edition() int32 {
    return t.edition
}

func (t *CombatPhasebook) Entry() *CombatBookEntry {
    return t.entry
}

type KeySchoolTierAirborne struct {
    school string
    tier int32
    airborne bool
}

type KeyCodeEdition struct {
    code string
    edition int32
}

type CombatPhasebookMgr struct {
    all []*CombatPhasebook
    schoolTierAirborneMap map[KeySchoolTierAirborne]*CombatPhasebook
    codeEditionMap map[KeyCodeEdition]*CombatPhasebook
    schoolMapList map[string][]*CombatPhasebook
    tierMapList map[int32][]*CombatPhasebook
    airborneMapList map[bool][]*CombatPhasebook
}

func(t *CombatPhasebookMgr) GetAll() []*CombatPhasebook {
    return t.all
}

func(t *CombatPhasebookMgr) Get(school string, tier int32, airborne bool) *CombatPhasebook {
    return t.schoolTierAirborneMap[KeySchoolTierAirborne{school, tier, airborne}]
}

func(t *CombatPhasebookMgr) GetByKeyCodeEdition(code string, edition int32) *CombatPhasebook {
    return t.codeEditionMap[KeyCodeEdition{code, edition}]
}

    func (t *CombatPhasebookMgr) GetAllBySchool(school string) []*CombatPhasebook {
        if t.schoolMapList == nil {
            t.schoolMapList = make(map[string][]*CombatPhasebook)
            for _, item := range t.all {
                key := item.school
                t.schoolMapList[key] = append(t.schoolMapList[key], item)
            }
        }
        return t.schoolMapList[school]
    }
    func (t *CombatPhasebookMgr) GetAllByTier(tier int32) []*CombatPhasebook {
        if t.tierMapList == nil {
            t.tierMapList = make(map[int32][]*CombatPhasebook)
            for _, item := range t.all {
                key := item.tier
                t.tierMapList[key] = append(t.tierMapList[key], item)
            }
        }
        return t.tierMapList[tier]
    }
    func (t *CombatPhasebookMgr) GetAllByAirborne(airborne bool) []*CombatPhasebook {
        if t.airborneMapList == nil {
            t.airborneMapList = make(map[bool][]*CombatPhasebook)
            for _, item := range t.all {
                key := item.airborne
                t.airborneMapList[key] = append(t.airborneMapList[key], item)
            }
        }
        return t.airborneMapList[airborne]
    }
func (t *CombatPhasebookMgr) Init(stream *Stream) {
    cnt := int32(stream.ReadCount(1))
    t.all = make([]*CombatPhasebook, 0, cnt)
    t.schoolTierAirborneMap = make(map[KeySchoolTierAirborne]*CombatPhasebook, cnt)
    t.codeEditionMap = make(map[KeyCodeEdition]*CombatPhasebook, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createCombatPhasebook(stream)
        t.all = append(t.all, v)
        t.schoolTierAirborneMap[KeySchoolTierAirborne{v.school, v.tier, v.airborne}] = v
        t.codeEditionMap[KeyCodeEdition{v.code, v.edition}] = v
    }
}

package config

import "fmt"

type CombatCast struct {
    id int32
    abilityCode string
    origin *CombatOrigin
    uses []*CombatAbilityUse
    optional []*CombatOptionalAim
    school string
    tier int32
    airborne bool
    bookCode string
    edition int32
    rewardKey *CombatPhaseKey
    refBook *CombatBookEntry
    refBookByCode *CombatBookEntry
    refBookRecord *CombatPhasebook
    refReward *CombatReward
}

func createCombatCast(stream *Stream) *CombatCast {
    v := &CombatCast{}
    v.id = stream.ReadInt32()
    v.abilityCode = stream.ReadStringInPool()
    v.origin = createCombatOrigin(stream)
    usesSize := stream.ReadCount(0)
    v.uses = make([]*CombatAbilityUse, usesSize)
    for i := 0; i < usesSize; i++ {
        v.uses[i] = createCombatAbilityUse(stream)
    }
    optionalSize := stream.ReadCount(0)
    v.optional = make([]*CombatOptionalAim, optionalSize)
    for i := 0; i < optionalSize; i++ {
        v.optional[i] = createCombatOptionalAim(stream)
    }
    v.school = stream.ReadStringInPool()
    v.tier = stream.ReadInt32()
    v.airborne = stream.ReadBool()
    v.bookCode = stream.ReadStringInPool()
    v.edition = stream.ReadInt32()
    v.rewardKey = createCombatPhaseKey(stream)
    return v
}

func (t *CombatCast) String() string {
    return fmt.Sprintf("CombatCast{id=%v, abilityCode=%v, origin=%v, uses=%v, optional=%v, school=%v, tier=%v, airborne=%v, bookCode=%v, edition=%v, rewardKey=%v}", t.id, t.abilityCode, t.origin, t.uses, t.optional, t.school, t.tier, t.airborne, t.bookCode, t.edition, t.rewardKey)
}

//getters
func (t *CombatCast) Id() int32 {
    return t.id
}

func (t *CombatCast) AbilityCode() string {
    return t.abilityCode
}

func (t *CombatCast) Origin() *CombatOrigin {
    return t.origin
}

func (t *CombatCast) Uses() []*CombatAbilityUse {
    return t.uses
}

func (t *CombatCast) Optional() []*CombatOptionalAim {
    return t.optional
}

func (t *CombatCast) School() string {
    return t.school
}

func (t *CombatCast) Tier() int32 {
    return t.tier
}

func (t *CombatCast) Airborne() bool {
    return t.airborne
}

func (t *CombatCast) BookCode() string {
    return t.bookCode
}

func (t *CombatCast) Edition() int32 {
    return t.edition
}

func (t *CombatCast) RewardKey() *CombatPhaseKey {
    return t.rewardKey
}

func (t *CombatCast) RefBook() *CombatBookEntry {
    return t.refBook
}
func (t *CombatCast) RefBookByCode() *CombatBookEntry {
    return t.refBookByCode
}
func (t *CombatCast) RefBookRecord() *CombatPhasebook {
    return t.refBookRecord
}
func (t *CombatCast) RefReward() *CombatReward {
    return t.refReward
}
type CombatCastMgr struct {
    all []*CombatCast
    idMap map[int32]*CombatCast
}

func(t *CombatCastMgr) GetAll() []*CombatCast {
    return t.all
}

func(t *CombatCastMgr) Get(id int32) *CombatCast {
    return t.idMap[id]
}

func (t *CombatCastMgr) Init(stream *Stream) {
    cnt := int32(stream.ReadCount(1))
    t.all = make([]*CombatCast, 0, cnt)
    t.idMap = make(map[int32]*CombatCast, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createCombatCast(stream)
        t.all = append(t.all, v)
        t.idMap[v.id] = v
    }
}

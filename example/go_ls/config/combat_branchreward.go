package config

import "fmt"

type CombatBranchreward struct {
    key *CombatPhaseKey
    reward *CombatReward
}

func createCombatBranchreward(stream *Stream) *CombatBranchreward {
    v := &CombatBranchreward{}
    v.key = createCombatPhaseKey(stream)
    v.reward = createCombatReward(stream)
    return v
}

func (t *CombatBranchreward) String() string {
    return fmt.Sprintf("CombatBranchreward{key=%v, reward=%v}", t.key, t.reward)
}

//getters
func (t *CombatBranchreward) Key() *CombatPhaseKey {
    return t.key
}

func (t *CombatBranchreward) Reward() *CombatReward {
    return t.reward
}

type keyCombatBranchrewardKey struct {
    name string
    variant int32
}

type CombatBranchrewardMgr struct {
    all []*CombatBranchreward
    keyMap map[keyCombatBranchrewardKey]*CombatBranchreward
}

func(t *CombatBranchrewardMgr) GetAll() []*CombatBranchreward {
    return t.all
}

func(t *CombatBranchrewardMgr) Get(key *CombatPhaseKey) *CombatBranchreward {
    if key == nil { return nil }
    return t.keyMap[keyCombatBranchrewardKey{key.name, key.variant}]
}

func (t *CombatBranchrewardMgr) Init(stream *Stream) {
    cnt := int32(stream.ReadCount(1))
    t.all = make([]*CombatBranchreward, 0, cnt)
    t.keyMap = make(map[keyCombatBranchrewardKey]*CombatBranchreward, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createCombatBranchreward(stream)
        t.all = append(t.all, v)
        t.keyMap[keyCombatBranchrewardKey{v.key.name, v.key.variant}] = v
    }
}

package config

import "fmt"

type CombatReward struct {
    count int32
}

func createCombatReward(stream *Stream) *CombatReward {
    v := &CombatReward{}
    v.count = stream.ReadInt32()
    return v
}

func (t *CombatReward) String() string {
    return fmt.Sprintf("CombatReward{count=%v}", t.count)
}

//getters
func (t *CombatReward) Count() int32 {
    return t.count
}


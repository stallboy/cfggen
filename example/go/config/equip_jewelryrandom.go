package config

import (
	"fmt"
	"os"
)

type EquipJewelryrandom struct {
    lvlRank LevelRank //等级
    attackRange Range //最小攻击力
    otherRange []Range //最小防御力
    testPack []EquipTestPackBean //测试pack
}

func createEquipJewelryrandom(stream *Stream) *EquipJewelryrandom {
    v := &EquipJewelryrandom{}
    v.lvlRank = stream.ReadLevelRank()
    v.attackRange = stream.ReadRange()
    v.otherRange = stream.Read[]Range()
    v.testPack = stream.Read[]EquipTestPackBean()
   return v
}

//getters
func (t *EquipJewelryrandom) GetLvlRank() LevelRank {
    return t.lvlRank
}

func (t *EquipJewelryrandom) GetAttackRange() Range {
    return t.attackRange
}

func (t *EquipJewelryrandom) GetOtherRange() []Range {
    return t.otherRange
}

func (t *EquipJewelryrandom) GetTestPack() []EquipTestPackBean {
    return t.testPack
}

type EquipJewelryrandomMgr struct {
    all []*EquipJewelryrandom
    lvlRankMap map[LevelRank]*EquipJewelryrandom
}

func(t *EquipJewelryrandomMgr) GetAll() []*EquipJewelryrandom {
    return t.all
}

func(t *EquipJewelryrandomMgr) GetByLvlRank(LvlRank LevelRank) (*EquipJewelryrandom,bool) {
    v, ok := t.lvlRankMap[LvlRank]
    return v, ok
}



func (t *EquipJewelryrandomMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*AiAi, 0, cnt)
    for i := 0; i < int(cnt); i++ {
        v := &AiAi{}
        v := createEquipJewelryrandom(stream)
        break
    }
}


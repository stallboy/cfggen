package config

import "fmt"

type EquipJewelry_random struct {
    lvlRank *LevelRank //等级
    attackRange *Range //最小攻击力
    otherRange []*Range //最小防御力
    testPack []*EquipTestPackBean //测试pack
}

func createEquipJewelry_random(stream *Stream) *EquipJewelry_random {
    v := &EquipJewelry_random{}
    v.lvlRank = createLevelRank(stream)
    v.attackRange = createRange(stream)
    otherRangeSize := stream.ReadCount(0)
    v.otherRange = make([]*Range, otherRangeSize)
    for i := 0; i < otherRangeSize; i++ {
        v.otherRange[i] = createRange(stream)
    }
    testPackSize := stream.ReadCount(0)
    v.testPack = make([]*EquipTestPackBean, testPackSize)
    for i := 0; i < testPackSize; i++ {
        v.testPack[i] = createEquipTestPackBean(stream)
    }
    return v
}

func (t *EquipJewelry_random) String() string {
    return fmt.Sprintf("EquipJewelry_random{lvlRank=%v, attackRange=%v, otherRange=%v, testPack=%v}", fmt.Sprintf("%v", t.lvlRank), fmt.Sprintf("%v", t.attackRange), fmt.Sprintf("%v", t.otherRange), fmt.Sprintf("%v", t.testPack))
}

//getters
func (t *EquipJewelry_random) LvlRank() *LevelRank {
    return t.lvlRank
}

func (t *EquipJewelry_random) AttackRange() *Range {
    return t.attackRange
}

func (t *EquipJewelry_random) OtherRange() []*Range {
    return t.otherRange
}

func (t *EquipJewelry_random) TestPack() []*EquipTestPackBean {
    return t.testPack
}

type EquipJewelry_randomMgr struct {
    all []*EquipJewelry_random
    lvlRankMap map[*LevelRank]*EquipJewelry_random
}

func(t *EquipJewelry_randomMgr) GetAll() []*EquipJewelry_random {
    return t.all
}

func(t *EquipJewelry_randomMgr) Get(lvlRank *LevelRank) *EquipJewelry_random {
    return t.lvlRankMap[lvlRank]
}

func (t *EquipJewelry_randomMgr) Init(stream *Stream) {
    cnt := int32(stream.ReadCount(1))
    t.all = make([]*EquipJewelry_random, 0, cnt)
    t.lvlRankMap = make(map[*LevelRank]*EquipJewelry_random, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createEquipJewelry_random(stream)
        t.all = append(t.all, v)
        t.lvlRankMap[v.lvlRank] = v
    }
}

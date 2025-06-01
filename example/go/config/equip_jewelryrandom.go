package config

type EquipJewelryrandom struct {
    lvlRank *LevelRank //等级
    attackRange *Range //最小攻击力
    otherRange []*Range //最小防御力
    testPack []*EquipTestPackBean //测试pack
}

func createEquipJewelryrandom(stream *Stream) *EquipJewelryrandom {
    v := &EquipJewelryrandom{}
    v.lvlRank = createLevelRank(stream)
    v.attackRange = createRange(stream)
    otherRangeSize := stream.ReadInt32()
    v.otherRange = make([]*Range, otherRangeSize)
    for i := 0; i < int(otherRangeSize); i++ {
        v.otherRange[i] = createRange(stream)
    }
    testPackSize := stream.ReadInt32()
    v.testPack = make([]*EquipTestPackBean, testPackSize)
    for i := 0; i < int(testPackSize); i++ {
        v.testPack[i] = createEquipTestPackBean(stream)
    }
    return v
}

//getters
func (t *EquipJewelryrandom) GetLvlRank() *LevelRank {
    return t.lvlRank
}

func (t *EquipJewelryrandom) GetAttackRange() *Range {
    return t.attackRange
}

func (t *EquipJewelryrandom) GetOtherRange() []*Range {
    return t.otherRange
}

func (t *EquipJewelryrandom) GetTestPack() []*EquipTestPackBean {
    return t.testPack
}

type EquipJewelryrandomMgr struct {
    all []*EquipJewelryrandom
    lvlRankMap map[*LevelRank]*EquipJewelryrandom
}

func(t *EquipJewelryrandomMgr) GetAll() []*EquipJewelryrandom {
    return t.all
}

func(t *EquipJewelryrandomMgr) Get(lvlRank *LevelRank) *EquipJewelryrandom {
    return t.lvlRankMap[lvlRank]
}

func (t *EquipJewelryrandomMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*EquipJewelryrandom, 0, cnt)
    t.lvlRankMap = make(map[*LevelRank]*EquipJewelryrandom, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createEquipJewelryrandom(stream)
        t.all = append(t.all, v)
        t.lvlRankMap[v.lvlRank] = v
    }
}

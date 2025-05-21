package config

type EquipJewelryrandom struct {
    lvlRank LevelRank //等级
    attackRange Range //最小攻击力
    otherRange []Range //最小防御力
    testPack []EquipTestPackBean //测试pack
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
    allMap map[LevelRank]*EquipJewelryrandom
}

func(t *EquipJewelryrandomMgr) GetAll() []*EquipJewelryrandom {
    return t.all
}

func(t *EquipJewelryrandomMgr) Get(key LevelRank) (*EquipJewelryrandom,bool) {
    v, ok := t.allMap[key]
    return v, ok
}


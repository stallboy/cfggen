package config

type LevelRank struct {
    level int //等级
    rank int //品质
    refRank *EquipRank
}

//getters
func (t *LevelRank) GetLevel() int {
    return t.level
}

func (t *LevelRank) GetRank() int {
    return t.rank
}

//ref properties
func (t *LevelRank) GetRefRank() *EquipRank {
    return t.refRank
}


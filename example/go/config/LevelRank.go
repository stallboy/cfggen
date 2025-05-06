package config

import (
    config_equip "config/equip"
)

type LevelRank struct {
    level int //等级
    rank int //品质
    refRank config_equip.Rank
}

//getters
func (t *LevelRank) GetLevel() int {
    return t.level
}

func (t *LevelRank) GetRank() int {
    return t.rank
}

//ref properties
func (t *LevelRank) GetRefRank() config_equip.Rank {
    return t.refRank
}


package config

type LevelRank struct {
    level int32 //等级
    rank int32 //品质
    refRank *EquipRank
}

func createLevelRank(stream *Stream) *LevelRank {
    v := &LevelRank{}
    v.level = stream.ReadInt32()
    v.rank = stream.ReadInt32()
    return v
}

//getters
func (t *LevelRank) GetLevel() int32 {
    return t.level
}

func (t *LevelRank) GetRank() int32 {
    return t.rank
}

//ref properties
func (t *LevelRank) GetRefRank() *EquipRank {
    if t.refRank == nil {
        t.refRank = GetEquipRankMgr().Get(t.rank)
    }
    return t.refRank
}



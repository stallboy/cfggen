package config

import "fmt"

type EquipRank struct {
    rankID int32 //稀有度
    rankName string //程序用名字
    rankShowName string //显示名称
}

func createEquipRank(stream *Stream) *EquipRank {
    v := &EquipRank{}
    v.rankID = stream.ReadInt32()
    v.rankName = stream.ReadStringInPool()
    v.rankShowName = stream.ReadStringInPool()
    return v
}

func (t *EquipRank) String() string {
    return fmt.Sprintf("EquipRank{rankID=%v, rankName=%v, rankShowName=%v}", t.rankID, t.rankName, t.rankShowName)
}

//getters
func (t *EquipRank) RankID() int32 {
    return t.rankID
}

func (t *EquipRank) RankName() string {
    return t.rankName
}

func (t *EquipRank) RankShowName() string {
    return t.rankShowName
}

func (t *EquipRankMgr) GetWhite() *EquipRank {
    return t.symbols["white"]
}

func (t *EquipRankMgr) GetGreen() *EquipRank {
    return t.symbols["green"]
}

func (t *EquipRankMgr) GetBlue() *EquipRank {
    return t.symbols["blue"]
}

func (t *EquipRankMgr) GetPurple() *EquipRank {
    return t.symbols["purple"]
}

func (t *EquipRankMgr) GetYellow() *EquipRank {
    return t.symbols["yellow"]
}

func (t *EquipRankMgr) GetRed() *EquipRank {
    return t.symbols["red"]
}

type EquipRankMgr struct {
    all []*EquipRank
    symbols map[string]*EquipRank
    rankIDArr []*EquipRank
}

func(t *EquipRankMgr) GetAll() []*EquipRank {
    return t.all
}

func(t *EquipRankMgr) Get(rankID int32) *EquipRank {
    if rankID < 0 || int(rankID) >= len(t.rankIDArr) {
        return nil
    }
    return t.rankIDArr[rankID]
}

func (t *EquipRankMgr) Init(stream *Stream) {
    cnt := int32(stream.ReadCount(1))
    t.all = make([]*EquipRank, 0, cnt)
    t.symbols = make(map[string]*EquipRank)
    t.rankIDArr = make([]*EquipRank, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createEquipRank(stream)
        t.all = append(t.all, v)
        t.rankIDArr[v.rankID] = v
        switch v.rankName {
        case "White":
            t.symbols["white"] = v
        case "Green":
            t.symbols["green"] = v
        case "Blue":
            t.symbols["blue"] = v
        case "Purple":
            t.symbols["purple"] = v
        case "Yellow":
            t.symbols["yellow"] = v
        case "Red":
            t.symbols["red"] = v
        }
    }
}

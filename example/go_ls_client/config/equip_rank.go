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

//entries
var (
    white EquipRank
    green EquipRank
    blue EquipRank
    purple EquipRank
    yellow EquipRank
)

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
	return &white
}

func (t *EquipRankMgr) GetGreen() *EquipRank {
	return &green
}

func (t *EquipRankMgr) GetBlue() *EquipRank {
	return &blue
}

func (t *EquipRankMgr) GetPurple() *EquipRank {
	return &purple
}

func (t *EquipRankMgr) GetYellow() *EquipRank {
	return &yellow
}

type EquipRankMgr struct {
    all []*EquipRank
    rankIDMap map[int32]*EquipRank
}

func(t *EquipRankMgr) GetAll() []*EquipRank {
    return t.all
}

func(t *EquipRankMgr) Get(rankID int32) *EquipRank {
    return t.rankIDMap[rankID]
}

func (t *EquipRankMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*EquipRank, 0, cnt)
    t.rankIDMap = make(map[int32]*EquipRank, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createEquipRank(stream)
        t.all = append(t.all, v)
        t.rankIDMap[v.rankID] = v
        switch v.rankName {
        case "White":
            white = *v
        case "Green":
            green = *v
        case "Blue":
            blue = *v
        case "Purple":
            purple = *v
        case "Yellow":
            yellow = *v
        }
    }
}

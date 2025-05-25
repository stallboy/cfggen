package config

type EquipRank struct {
    rankID int32 //稀有度
    rankName string //程序用名字
    rankShowName string //显示名称
}

func createEquipRank(stream *Stream) *EquipRank {
    v := &EquipRank{}
    v.rankID = stream.ReadInt32()
    v.rankName = stream.ReadString()
    v.rankShowName = stream.ReadString()
    return v
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
func (t *EquipRank) GetRankID() int32 {
    return t.rankID
}

func (t *EquipRank) GetRankName() string {
    return t.rankName
}

func (t *EquipRank) GetRankShowName() string {
    return t.rankShowName
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
    }
}


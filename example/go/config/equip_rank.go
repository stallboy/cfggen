package config

type EquipRank struct {
    rankID int //稀有度
    rankName string //程序用名字
    rankShowName string //显示名称
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
func (t *EquipRank) GetRankID() int {
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
    allMap map[int]*EquipRank
}

func(t *EquipRankMgr) GetAll() []*EquipRank {
    return t.all
}

func(t *EquipRankMgr) Get(key int) (*EquipRank,bool) {
    v, ok := t.allMap[key]
    return v, ok
}


package config

type OtherLoot struct {
    lootid int //序号
    ename string
    name string //名字
    chanceList []int //掉落0件物品的概率
    listRefLootid []OtherLootitem
}

//getters
func (t *OtherLoot) GetLootid() int {
    return t.lootid
}

func (t *OtherLoot) GetEname() string {
    return t.ename
}

func (t *OtherLoot) GetName() string {
    return t.name
}

func (t *OtherLoot) GetChanceList() []int {
    return t.chanceList
}

//ref properties
func (t *OtherLoot) GetListRefLootid() []OtherLootitem {
    return t.listRefLootid
}

type OtherLootMgr struct {
    all []*OtherLoot
    lootidMap map[int]*OtherLoot
}

func(t *OtherLootMgr) GetAll() []*OtherLoot {
    return t.all
}

func(t *OtherLootMgr) GetBylootid(lootid int) (*OtherLoot,bool) {
    v, ok := t.lootidMap[lootid]
    return v, ok
}




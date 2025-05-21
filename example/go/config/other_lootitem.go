package config

type OtherLootitem struct {
    lootid int //掉落id
    itemid int //掉落物品
    chance int //掉落概率
    countmin int //数量下限
    countmax int //数量上限
}

//getters
func (t *OtherLootitem) GetLootid() int {
    return t.lootid
}

func (t *OtherLootitem) GetItemid() int {
    return t.itemid
}

func (t *OtherLootitem) GetChance() int {
    return t.chance
}

func (t *OtherLootitem) GetCountmin() int {
    return t.countmin
}

func (t *OtherLootitem) GetCountmax() int {
    return t.countmax
}

type KeyLootidItemid struct {
    lootid int
    itemid int
}

type OtherLootitemMgr struct {
    all []*OtherLootitem
    allMap map[KeyLootidItemid]*OtherLootitem
}

func(t *OtherLootitemMgr) GetAll() []*OtherLootitem {
    return t.all
}

func(t *OtherLootitemMgr) Get(key KeyLootidItemid) (*OtherLootitem,bool) {
    v, ok := t.allMap[key]
    return v, ok
}


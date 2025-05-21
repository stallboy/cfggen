package config

type OtherDropItem struct {
    chance int //掉落概率
    itemids []int //掉落物品
    countmin int //数量下限
    countmax int //数量上限
}

//getters
func (t *OtherDropItem) GetChance() int {
    return t.chance
}

func (t *OtherDropItem) GetItemids() []int {
    return t.itemids
}

func (t *OtherDropItem) GetCountmin() int {
    return t.countmin
}

func (t *OtherDropItem) GetCountmax() int {
    return t.countmax
}


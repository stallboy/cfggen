package other

type DropItem struct {
    chance int //掉落概率
    itemids []int //掉落物品
    countmin int //数量下限
    countmax int //数量上限
}

//getters
func (t *DropItem) GetChance() int {
    return t.chance
}

func (t *DropItem) GetItemids() []int {
    return t.itemids
}

func (t *DropItem) GetCountmin() int {
    return t.countmin
}

func (t *DropItem) GetCountmax() int {
    return t.countmax
}


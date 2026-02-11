package config

import "fmt"

type OtherDropItem struct {
    chance int32 //掉落概率
    itemids []int32 //掉落物品
    countmin int32 //数量下限
    countmax int32 //数量上限
}

func createOtherDropItem(stream *Stream) *OtherDropItem {
    v := &OtherDropItem{}
    v.chance = stream.ReadInt32()
    itemidsSize := stream.ReadInt32()
    v.itemids = make([]int32, itemidsSize)
    for i := 0; i < int(itemidsSize); i++ {
        v.itemids[i] = stream.ReadInt32()
    }
    v.countmin = stream.ReadInt32()
    v.countmax = stream.ReadInt32()
    return v
}

func (t *OtherDropItem) String() string {
    return fmt.Sprintf("OtherDropItem{chance=%v, itemids=%v, countmin=%v, countmax=%v}", t.chance, fmt.Sprintf("%v", t.itemids), t.countmin, t.countmax)
}

//getters
func (t *OtherDropItem) Chance() int32 {
    return t.chance
}

func (t *OtherDropItem) Itemids() []int32 {
    return t.itemids
}

func (t *OtherDropItem) Countmin() int32 {
    return t.countmin
}

func (t *OtherDropItem) Countmax() int32 {
    return t.countmax
}


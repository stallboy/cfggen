package config

import (
	"fmt"
	"os"
)

type OtherDropItem struct {
    chance int32 //掉落概率
    itemids []int32 //掉落物品
    countmin int32 //数量下限
    countmax int32 //数量上限
}

func createOtherDropItem(stream *Stream) *OtherDropItem {
    v := &OtherDropItem{}
    v.chance = stream.ReadInt32()
    v.itemids = stream.Read[]int32()
    v.countmin = stream.ReadInt32()
    v.countmax = stream.ReadInt32()
   return v
}

//getters
func (t *OtherDropItem) GetChance() int32 {
    return t.chance
}

func (t *OtherDropItem) GetItemids() []int32 {
    return t.itemids
}

func (t *OtherDropItem) GetCountmin() int32 {
    return t.countmin
}

func (t *OtherDropItem) GetCountmax() int32 {
    return t.countmax
}


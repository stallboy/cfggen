package config

import "fmt"

type OtherLoot_item struct {
    lootid int32 //掉落id
    itemid int32 //掉落物品
    chance int32 //掉落概率
    countmin int32 //数量下限
    countmax int32 //数量上限
}

func createOtherLoot_item(stream *Stream) *OtherLoot_item {
    v := &OtherLoot_item{}
    v.lootid = stream.ReadInt32()
    v.itemid = stream.ReadInt32()
    v.chance = stream.ReadInt32()
    v.countmin = stream.ReadInt32()
    v.countmax = stream.ReadInt32()
    return v
}

func (t *OtherLoot_item) String() string {
    return fmt.Sprintf("OtherLoot_item{lootid=%v, itemid=%v, chance=%v, countmin=%v, countmax=%v}", t.lootid, t.itemid, t.chance, t.countmin, t.countmax)
}

//getters
func (t *OtherLoot_item) Lootid() int32 {
    return t.lootid
}

func (t *OtherLoot_item) Itemid() int32 {
    return t.itemid
}

func (t *OtherLoot_item) Chance() int32 {
    return t.chance
}

func (t *OtherLoot_item) Countmin() int32 {
    return t.countmin
}

func (t *OtherLoot_item) Countmax() int32 {
    return t.countmax
}

type KeyLootidItemid struct {
    lootid int32
    itemid int32
}

type OtherLoot_itemMgr struct {
    all []*OtherLoot_item
    lootidItemidMap map[KeyLootidItemid]*OtherLoot_item
    lootidMapList map[int32][]*OtherLoot_item
    itemidMapList map[int32][]*OtherLoot_item
}

func(t *OtherLoot_itemMgr) GetAll() []*OtherLoot_item {
    return t.all
}

func(t *OtherLoot_itemMgr) Get(lootid int32, itemid int32) *OtherLoot_item {
    return t.lootidItemidMap[KeyLootidItemid{lootid, itemid}]
}

    func (t *OtherLoot_itemMgr) GetAllByLootid(lootid int32) []*OtherLoot_item {
        if t.lootidMapList == nil {
            t.lootidMapList = make(map[int32][]*OtherLoot_item)
            for _, item := range t.all {
                key := item.lootid
                t.lootidMapList[key] = append(t.lootidMapList[key], item)
            }
        }
        return t.lootidMapList[lootid]
    }
    func (t *OtherLoot_itemMgr) GetAllByItemid(itemid int32) []*OtherLoot_item {
        if t.itemidMapList == nil {
            t.itemidMapList = make(map[int32][]*OtherLoot_item)
            for _, item := range t.all {
                key := item.itemid
                t.itemidMapList[key] = append(t.itemidMapList[key], item)
            }
        }
        return t.itemidMapList[itemid]
    }
func (t *OtherLoot_itemMgr) Init(stream *Stream) {
    cnt := int32(stream.ReadCount(1))
    t.all = make([]*OtherLoot_item, 0, cnt)
    t.lootidItemidMap = make(map[KeyLootidItemid]*OtherLoot_item, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createOtherLoot_item(stream)
        t.all = append(t.all, v)
        t.lootidItemidMap[KeyLootidItemid{v.lootid, v.itemid}] = v
    }
}

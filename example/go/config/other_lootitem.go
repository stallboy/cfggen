package config

type OtherLootitem struct {
    lootid int32 //掉落id
    itemid int32 //掉落物品
    chance int32 //掉落概率
    countmin int32 //数量下限
    countmax int32 //数量上限
}

func createOtherLootitem(stream *Stream) *OtherLootitem {
    v := &OtherLootitem{}
    v.lootid = stream.ReadInt32()
    v.itemid = stream.ReadInt32()
    v.chance = stream.ReadInt32()
    v.countmin = stream.ReadInt32()
    v.countmax = stream.ReadInt32()
    return v
}

//getters
func (t *OtherLootitem) GetLootid() int32 {
    return t.lootid
}

func (t *OtherLootitem) GetItemid() int32 {
    return t.itemid
}

func (t *OtherLootitem) GetChance() int32 {
    return t.chance
}

func (t *OtherLootitem) GetCountmin() int32 {
    return t.countmin
}

func (t *OtherLootitem) GetCountmax() int32 {
    return t.countmax
}

type KeyLootidItemid struct {
    lootid int32
    itemid int32
}

type OtherLootitemMgr struct {
    all []*OtherLootitem
    lootidItemidMap map[KeyLootidItemid]*OtherLootitem
}

func(t *OtherLootitemMgr) GetAll() []*OtherLootitem {
    return t.all
}

func(t *OtherLootitemMgr) GetByKeyLootidItemid(lootid int32, itemid int32) *OtherLootitem {
    return t.lootidItemidMap[KeyLootidItemid{lootid, itemid}]
}



func (t *OtherLootitemMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*OtherLootitem, 0, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createOtherLootitem(stream)
        t.all = append(t.all, v)
    }
}


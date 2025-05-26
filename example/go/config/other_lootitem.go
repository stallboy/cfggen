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

    lootidMapList map[int32][]*OtherLootitem
    itemidMapList map[int32][]*OtherLootitem
}

func(t *OtherLootitemMgr) GetAll() []*OtherLootitem {
    return t.all
}

func(t *OtherLootitemMgr) Get(lootid int32, itemid int32) *OtherLootitem {
    return t.lootidItemidMap[KeyLootidItemid{lootid, itemid}]
}

func (t *OtherLootitemMgr) GetAllByLootid(lootid int32) []*OtherLootitem {
	if t.lootidMapList == nil {
		t.lootidMapList = make(map[int32][]*OtherLootitem)
		for _, item := range t.all {
			t.lootidMapList[item.lootid] = append(t.lootidMapList[item.lootid], item)
		}
	}
	return t.lootidMapList[lootid]
}
func (t *OtherLootitemMgr) GetAllByItemid(itemid int32) []*OtherLootitem {
	if t.itemidMapList == nil {
		t.itemidMapList = make(map[int32][]*OtherLootitem)
		for _, item := range t.all {
			t.itemidMapList[item.itemid] = append(t.itemidMapList[item.itemid], item)
		}
	}
	return t.itemidMapList[itemid]
}


func (t *OtherLootitemMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*OtherLootitem, 0, cnt)
    t.lootidItemidMap = make(map[KeyLootidItemid]*OtherLootitem, cnt)

    for i := 0; i < int(cnt); i++ {
        v := createOtherLootitem(stream)
        t.all = append(t.all, v)
        t.lootidItemidMap[KeyLootidItemid{v.lootid, v.itemid}] = v

    }
}


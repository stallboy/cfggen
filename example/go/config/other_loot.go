package config

type OtherLoot struct {
    lootid int32 //序号
    ename string
    name string //名字
    chanceList []int32 //掉落0件物品的概率
    listRefLootid []*OtherLootitem
}

func createOtherLoot(stream *Stream) *OtherLoot {
    v := &OtherLoot{}
    v.lootid = stream.ReadInt32()
    v.ename = stream.ReadString()
    v.name = stream.ReadString()
    chanceListSize := stream.ReadInt32()
    v.chanceList = make([]int32, chanceListSize)
    for i := 0; i < int(chanceListSize); i++ {
        v.chanceList[i] = stream.ReadInt32()
    }
    return v
}

//getters
func (t *OtherLoot) GetLootid() int32 {
    return t.lootid
}

func (t *OtherLoot) GetEname() string {
    return t.ename
}

func (t *OtherLoot) GetName() string {
    return t.name
}

func (t *OtherLoot) GetChanceList() []int32 {
    return t.chanceList
}

//ref properties
func (t *OtherLoot) GetListRefLootid() []*OtherLootitem {
    if t.listRefLootid == nil {
        t.listRefLootid = GetOtherLootitemMgr().GetAllByLootid(t.lootid)
    }
    return t.listRefLootid
}


type OtherLootMgr struct {
    all []*OtherLoot
    lootidMap map[int32]*OtherLoot

}

func(t *OtherLootMgr) GetAll() []*OtherLoot {
    return t.all
}

func(t *OtherLootMgr) Get(lootid int32) *OtherLoot {
    return t.lootidMap[lootid]
}



func (t *OtherLootMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*OtherLoot, 0, cnt)
    t.lootidMap = make(map[int32]*OtherLoot, cnt)

    for i := 0; i < int(cnt); i++ {
        v := createOtherLoot(stream)
        t.all = append(t.all, v)
        t.lootidMap[v.lootid] = v
    }
}


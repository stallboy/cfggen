package config

import "fmt"

type OtherLoot struct {
    lootid int32 //序号
    ename string
    name *Text //名字
    chanceList []int32 //掉落0件物品的概率
    listRefLootid []*OtherLootitem
    listRefAnotherWay []*OtherLootitem
}

func createOtherLoot(stream *Stream) *OtherLoot {
    v := &OtherLoot{}
    v.lootid = stream.ReadInt32()
    v.ename = stream.ReadStringInPool()
    v.name = createText(stream)
    chanceListSize := stream.ReadInt32()
    v.chanceList = make([]int32, chanceListSize)
    for i := 0; i < int(chanceListSize); i++ {
        v.chanceList[i] = stream.ReadInt32()
    }
    return v
}

func (t *OtherLoot) String() string {
    return fmt.Sprintf("OtherLoot{lootid=%v, ename=%v, name=%v, chanceList=%v}", t.lootid, t.ename, t.name, fmt.Sprintf("%v", t.chanceList))
}

//getters
func (t *OtherLoot) Lootid() int32 {
    return t.lootid
}

func (t *OtherLoot) Ename() string {
    return t.ename
}

func (t *OtherLoot) Name() *Text {
    return t.name
}

func (t *OtherLoot) ChanceList() []int32 {
    return t.chanceList
}

func (t *OtherLoot) ListRefLootid() []*OtherLootitem {
    if t.listRefLootid == nil {
        t.listRefLootid = GetOtherLootitemMgr().GetAllByLootid(t.lootid)
    }
    return t.listRefLootid
}

func (t *OtherLoot) ListRefAnotherWay() []*OtherLootitem {
    if t.listRefAnotherWay == nil {
        t.listRefAnotherWay = GetOtherLootitemMgr().GetAllByLootid(t.lootid)
    }
    return t.listRefAnotherWay
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

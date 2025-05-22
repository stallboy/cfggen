package config

import (
	"fmt"
	"os"
)

type OtherLoot struct {
    lootid int32 //序号
    ename string
    name string //名字
    chanceList []int32 //掉落0件物品的概率
    listRefLootid []OtherLootitem
}

func createOtherLoot(stream *Stream) *OtherLoot {
    v := &OtherLoot{}
    v.lootid = stream.ReadInt32()
    v.ename = stream.ReadString()
    v.name = stream.ReadString()
    v.chanceList = stream.Read[]int32()
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
func (t *OtherLoot) GetListRefLootid() []OtherLootitem {
    return t.listRefLootid
}

type OtherLootMgr struct {
    all []*OtherLoot
    lootidMap map[int32]*OtherLoot
}

func(t *OtherLootMgr) GetAll() []*OtherLoot {
    return t.all
}

func(t *OtherLootMgr) GetBylootid(lootid int32) (*OtherLoot,bool) {
    v, ok := t.lootidMap[lootid]
    return v, ok
}



func (t *OtherLootMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*AiAi, 0, cnt)
    for i := 0; i < int(cnt); i++ {
        v := &AiAi{}
        v := createOtherLoot(stream)
        break
    }
}


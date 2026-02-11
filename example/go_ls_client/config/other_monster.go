package config

import "fmt"

type OtherMonster struct {
    id int32
    posList []*Position
    lootId int32 //loot
    lootItemId int32 //item
    refLoot *OtherLootitem
    refAllLoot *OtherLoot
}

func createOtherMonster(stream *Stream) *OtherMonster {
    v := &OtherMonster{}
    v.id = stream.ReadInt32()
    posListSize := stream.ReadInt32()
    v.posList = make([]*Position, posListSize)
    for i := 0; i < int(posListSize); i++ {
        v.posList[i] = createPosition(stream)
    }
    v.lootId = stream.ReadInt32()
    v.lootItemId = stream.ReadInt32()
    return v
}

func (t *OtherMonster) String() string {
    return fmt.Sprintf("OtherMonster{id=%v, posList=%v, lootId=%v, lootItemId=%v}", t.id, fmt.Sprintf("%v", t.posList), t.lootId, t.lootItemId)
}

//getters
func (t *OtherMonster) Id() int32 {
    return t.id
}

func (t *OtherMonster) PosList() []*Position {
    return t.posList
}

func (t *OtherMonster) LootId() int32 {
    return t.lootId
}

func (t *OtherMonster) LootItemId() int32 {
    return t.lootItemId
}

func (t *OtherMonster) RefLoot() *OtherLootitem {
    if t.refLoot == nil {
        t.refLoot = GetOtherLootitemMgr().Get(t.lootId, t.lootItemId)
    }
    return t.refLoot
}

func (t *OtherMonster) RefAllLoot() *OtherLoot {
    if t.refAllLoot == nil {
        t.refAllLoot = GetOtherLootMgr().Get(t.lootId)
    }
    return t.refAllLoot
}

type OtherMonsterMgr struct {
    all []*OtherMonster
    idMap map[int32]*OtherMonster
}

func(t *OtherMonsterMgr) GetAll() []*OtherMonster {
    return t.all
}

func(t *OtherMonsterMgr) Get(id int32) *OtherMonster {
    return t.idMap[id]
}

func (t *OtherMonsterMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*OtherMonster, 0, cnt)
    t.idMap = make(map[int32]*OtherMonster, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createOtherMonster(stream)
        t.all = append(t.all, v)
        t.idMap[v.id] = v
    }
}

package config

import "fmt"

type OtherMonster struct {
    id int32
    posList []*Position
    lootId int32 //loot
    lootItemId int32 //item
    enumMap1 map[string]int32
    enumMap2 map[int32]string
    refLoot *OtherLootitem
    refAllLoot *OtherLoot
    refEnumMap2 map[int32]*OtherArgCaptureMode
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
    enumMap1Size := stream.ReadInt32()
    v.enumMap1 = make(map[string]int32, enumMap1Size)
    for i := 0; i < int(enumMap1Size); i++ {
        var k = stream.ReadStringInPool()
        v.enumMap1[k] = stream.ReadInt32()
    }
    enumMap2Size := stream.ReadInt32()
    v.enumMap2 = make(map[int32]string, enumMap2Size)
    for i := 0; i < int(enumMap2Size); i++ {
        var k = stream.ReadInt32()
        v.enumMap2[k] = stream.ReadStringInPool()
    }
    return v
}

func (t *OtherMonster) String() string {
    return fmt.Sprintf("OtherMonster{id=%v, posList=%v, lootId=%v, lootItemId=%v, enumMap1=%v, enumMap2=%v}", t.id, fmt.Sprintf("%v", t.posList), t.lootId, t.lootItemId, fmt.Sprintf("%v", t.enumMap1), fmt.Sprintf("%v", t.enumMap2))
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

func (t *OtherMonster) EnumMap1() map[string]int32 {
    return t.enumMap1
}

func (t *OtherMonster) EnumMap2() map[int32]string {
    return t.enumMap2
}

//map ref
func (t *OtherMonster) RefEnumMap2() map[int32]*OtherArgCaptureMode {
    if t.refEnumMap2 == nil {
        t.refEnumMap2 = make(map[int32]*OtherArgCaptureMode, len(t.enumMap2))
        for k, v := range t.enumMap2 {
            t.refEnumMap2[k] = GetOtherArgCaptureModeMgr().Get(v)
        }
    }
    return t.refEnumMap2
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

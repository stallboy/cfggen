package config

import "fmt"

type OtherSign_in struct {
    id int32 //礼包ID
    item2countMap map[int32]int32 //普通奖励
    vipitem2vipcountMap map[int32]int32 //vip奖励
    viplevel int32 //领取vip奖励的最低等级
    iconFile string //礼包图标
    refVipitem2vipcountMap map[int32]*OtherLoot
}

func createOtherSign_in(stream *Stream) *OtherSign_in {
    v := &OtherSign_in{}
    v.id = stream.ReadInt32()
    item2countMapSize := stream.ReadCount(0)
    v.item2countMap = make(map[int32]int32, item2countMapSize)
    for i := 0; i < item2countMapSize; i++ {
        var k = stream.ReadInt32()
        if _, exists := v.item2countMap[k]; exists { panic("duplicate map key: other.sign_in.item2countMap") }
        v.item2countMap[k] = stream.ReadInt32()
    }
    vipitem2vipcountMapSize := stream.ReadCount(0)
    v.vipitem2vipcountMap = make(map[int32]int32, vipitem2vipcountMapSize)
    for i := 0; i < vipitem2vipcountMapSize; i++ {
        var k = stream.ReadInt32()
        if _, exists := v.vipitem2vipcountMap[k]; exists { panic("duplicate map key: other.sign_in.vipitem2vipcountMap") }
        v.vipitem2vipcountMap[k] = stream.ReadInt32()
    }
    v.viplevel = stream.ReadInt32()
    v.iconFile = stream.ReadStringInPool()
    return v
}

func (t *OtherSign_in) String() string {
    return fmt.Sprintf("OtherSign_in{id=%v, item2countMap=%v, vipitem2vipcountMap=%v, viplevel=%v, iconFile=%v}", t.id, t.item2countMap, t.vipitem2vipcountMap, t.viplevel, t.iconFile)
}

//getters
func (t *OtherSign_in) Id() int32 {
    return t.id
}

func (t *OtherSign_in) Item2countMap() map[int32]int32 {
    return t.item2countMap
}

func (t *OtherSign_in) Vipitem2vipcountMap() map[int32]int32 {
    return t.vipitem2vipcountMap
}

func (t *OtherSign_in) Viplevel() int32 {
    return t.viplevel
}

func (t *OtherSign_in) IconFile() string {
    return t.iconFile
}

    func (t *OtherSign_in) RefVipitem2vipcountMap() map[int32]*OtherLoot {
        if t.refVipitem2vipcountMap == nil {
            t.refVipitem2vipcountMap = make(map[int32]*OtherLoot, len(t.vipitem2vipcountMap))
            for key, value := range t.vipitem2vipcountMap {
                t.refVipitem2vipcountMap[key] = GetOtherLootMgr().Get(value)
            }
        }
        return t.refVipitem2vipcountMap
    }

type OtherSign_inMgr struct {
    all []*OtherSign_in
    idMap map[int32]*OtherSign_in
}

func(t *OtherSign_inMgr) GetAll() []*OtherSign_in {
    return t.all
}

func(t *OtherSign_inMgr) Get(id int32) *OtherSign_in {
    return t.idMap[id]
}

func (t *OtherSign_inMgr) Init(stream *Stream) {
    cnt := int32(stream.ReadCount(1))
    t.all = make([]*OtherSign_in, 0, cnt)
    t.idMap = make(map[int32]*OtherSign_in, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createOtherSign_in(stream)
        t.all = append(t.all, v)
        t.idMap[v.id] = v
    }
}

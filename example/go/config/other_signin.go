package config

type OtherSignin struct {
    id int32 //礼包ID
    item2countMap map[int32]int32 //普通奖励
    vipitem2vipcountMap map[int32]int32 //vip奖励
    viplevel int32 //领取vip奖励的最低等级
    iconFile string //礼包图标
    refVipitem2vipcountMap map[int32]*OtherLoot
}

func createOtherSignin(stream *Stream) *OtherSignin {
    v := &OtherSignin{}
    v.id = stream.ReadInt32()
    item2countMapSize := stream.ReadInt32()
    v.item2countMap = make(map[int32]int32, item2countMapSize)
    for i := 0; i < int(item2countMapSize); i++ {
        var k = stream.ReadInt32()
        v.item2countMap[k] = stream.ReadInt32()
    }
    vipitem2vipcountMapSize := stream.ReadInt32()
    v.vipitem2vipcountMap = make(map[int32]int32, vipitem2vipcountMapSize)
    for i := 0; i < int(vipitem2vipcountMapSize); i++ {
        var k = stream.ReadInt32()
        v.vipitem2vipcountMap[k] = stream.ReadInt32()
    }
    v.viplevel = stream.ReadInt32()
    v.iconFile = stream.ReadString()
    return v
}

//getters
func (t *OtherSignin) Id() int32 {
    return t.id
}

func (t *OtherSignin) Item2countMap() map[int32]int32 {
    return t.item2countMap
}

func (t *OtherSignin) Vipitem2vipcountMap() map[int32]int32 {
    return t.vipitem2vipcountMap
}

func (t *OtherSignin) Viplevel() int32 {
    return t.viplevel
}

func (t *OtherSignin) IconFile() string {
    return t.iconFile
}

//map ref
func (t *OtherSignin) RefVipitem2vipcountMap() map[int32]*OtherLoot {
    if t.refVipitem2vipcountMap == nil {
        t.refVipitem2vipcountMap = make(map[int32]*OtherLoot, len(t.vipitem2vipcountMap))
        for k, v := range t.vipitem2vipcountMap {
            t.refVipitem2vipcountMap[k] = GetOtherLootMgr().Get(v)
        }
    }
    return t.refVipitem2vipcountMap
}
type OtherSigninMgr struct {
    all []*OtherSignin
    idMap map[int32]*OtherSignin
}

func(t *OtherSigninMgr) GetAll() []*OtherSignin {
    return t.all
}

func(t *OtherSigninMgr) Get(id int32) *OtherSignin {
    return t.idMap[id]
}

func (t *OtherSigninMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*OtherSignin, 0, cnt)
    t.idMap = make(map[int32]*OtherSignin, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createOtherSignin(stream)
        t.all = append(t.all, v)
        t.idMap[v.id] = v
    }
}

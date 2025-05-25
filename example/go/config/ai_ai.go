package config

type AiAi struct {
    iD int32
    desc string //描述----这里测试下多行效果--再来一行
    condID string //触发公式
    trigTick AiTriggerTick //触发间隔(帧)
    trigOdds int32 //触发几率
    actionID []int32 //触发行为
    deathRemove bool //死亡移除
}

func createAiAi(stream *Stream) *AiAi {
    v := &AiAi{}
    v.iD = stream.ReadInt32()
    v.desc = stream.ReadString()
    v.condID = stream.ReadString()
    v.trigTick = createAiTriggerTick(stream)
    v.trigOdds = stream.ReadInt32()
    actionIDSize := stream.ReadInt32()
    v.actionID = make([]int32, actionIDSize)
    for i := 0; i < int(actionIDSize); i++ {
        v.actionID[i] = stream.ReadInt32()
    }
    v.deathRemove = stream.ReadBool()
    return v
}

//getters
func (t *AiAi) GetID() int32 {
    return t.iD
}

func (t *AiAi) GetDesc() string {
    return t.desc
}

func (t *AiAi) GetCondID() string {
    return t.condID
}

func (t *AiAi) GetTrigTick() AiTriggerTick {
    return t.trigTick
}

func (t *AiAi) GetTrigOdds() int32 {
    return t.trigOdds
}

func (t *AiAi) GetActionID() []int32 {
    return t.actionID
}

func (t *AiAi) GetDeathRemove() bool {
    return t.deathRemove
}

type AiAiMgr struct {
    all []*AiAi
    iDMap map[int32]*AiAi
}

func(t *AiAiMgr) GetAll() []*AiAi {
    return t.all
}

func(t *AiAiMgr) GetByiD(iD int32) *AiAi {
    return t.iDMap[iD]
}



func (t *AiAiMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*AiAi, 0, cnt)
    t.iDMap = make(map[int32]*AiAi, cnt)

    for i := 0; i < int(cnt); i++ {
        v := createAiAi(stream)
        t.all = append(t.all, v)
        t.iDMap[v.iD] = v
    }
}


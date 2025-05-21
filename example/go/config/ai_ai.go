package config

type AiAi struct {
    iD int
    desc string //描述----这里测试下多行效果--再来一行
    condID string //触发公式
    trigTick AiTriggerTick //触发间隔(帧)
    trigOdds int //触发几率
    actionID []int //触发行为
    deathRemove bool //死亡移除
}

//getters
func (t *AiAi) GetID() int {
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

func (t *AiAi) GetTrigOdds() int {
    return t.trigOdds
}

func (t *AiAi) GetActionID() []int {
    return t.actionID
}

func (t *AiAi) GetDeathRemove() bool {
    return t.deathRemove
}

type AiAiMgr struct {
    all []*AiAi
    allMap map[int]*AiAi
}

func(t *AiAiMgr) GetAll() []*AiAi {
    return t.all
}

func(t *AiAiMgr) Get(key int) (*AiAi,bool) {
    v, ok := t.allMap[key]
    return v, ok
}


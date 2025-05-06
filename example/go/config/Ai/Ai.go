package ai

type Ai struct {
    iD int
    desc string //描述----这里测试下多行效果--再来一行
    condID string //触发公式
    trigTick config.Ai.TriggerTick //触发间隔(帧)
    trigOdds int //触发几率
    actionID []int //触发行为
    deathRemove bool //死亡移除
}

//getters
func (t *Ai) GetID() int {
    return t.iD
}

func (t *Ai) GetDesc() string {
    return t.desc
}

func (t *Ai) GetCondID() string {
    return t.condID
}

func (t *Ai) GetTrigTick() config.Ai.TriggerTick {
    return t.trigTick
}

func (t *Ai) GetTrigOdds() int {
    return t.trigOdds
}

func (t *Ai) GetActionID() []int {
    return t.actionID
}

func (t *Ai) GetDeathRemove() bool {
    return t.deathRemove
}

var all []Ai
func GetAll() []Ai {
    return all[:len(all)]:len(all)]
}

var allMap map[int]Ai
func Get(key int) (Ai,bool){
    return allMap[key]
}

func Initialize(Stream os, errors LoadErrors) {
    all = new KeyedList<int, Ai>();
    for (var c = os.ReadInt32(); c > 0; c--) {
        var self = _create(os);
        all.Add(self.ID, self);
    }
}

        internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
        {
            all = new Config.KeyedList<int, Ai>();
            for (var c = os.ReadInt32(); c > 0; c--) {
                var self = _create(os);
                all.Add(self.ID, self);
            }
        }


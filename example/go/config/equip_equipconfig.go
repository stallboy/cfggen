package config

type EquipEquipconfig struct {
    entry string //入口，程序填
    stone_count_for_set int //形成套装的音石数量
    draw_protect_name string //保底策略名称
    broadcastid int //公告Id
    broadcast_least_quality int //公告的最低品质
    week_reward_mailid int //抽卡周奖励的邮件id
}

//entries
var (
    instance EquipEquipconfig
    instance2 EquipEquipconfig
)

//getters
func (t *EquipEquipconfig) GetEntry() string {
    return t.entry
}

func (t *EquipEquipconfig) GetStone_count_for_set() int {
    return t.stone_count_for_set
}

func (t *EquipEquipconfig) GetDraw_protect_name() string {
    return t.draw_protect_name
}

func (t *EquipEquipconfig) GetBroadcastid() int {
    return t.broadcastid
}

func (t *EquipEquipconfig) GetBroadcast_least_quality() int {
    return t.broadcast_least_quality
}

func (t *EquipEquipconfig) GetWeek_reward_mailid() int {
    return t.week_reward_mailid
}

type EquipEquipconfigMgr struct {
    all []*EquipEquipconfig
    allMap map[string]*EquipEquipconfig
}

func(t *EquipEquipconfigMgr) GetAll() []*EquipEquipconfig {
    return t.all
}

func(t *EquipEquipconfigMgr) Get(key string) (*EquipEquipconfig,bool) {
    v, ok := t.allMap[key]
    return v, ok
}


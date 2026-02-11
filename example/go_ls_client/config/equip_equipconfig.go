package config

import "fmt"

type EquipEquipconfig struct {
    entry string //入口，程序填
    stone_count_for_set int32 //形成套装的音石数量
    draw_protect_name string //保底策略名称
    broadcastid int32 //公告Id
    broadcast_least_quality int32 //公告的最低品质
    week_reward_mailid int32 //抽卡周奖励的邮件id
}

func createEquipEquipconfig(stream *Stream) *EquipEquipconfig {
    v := &EquipEquipconfig{}
    v.entry = stream.ReadStringInPool()
    v.stone_count_for_set = stream.ReadInt32()
    v.draw_protect_name = stream.ReadStringInPool()
    v.broadcastid = stream.ReadInt32()
    v.broadcast_least_quality = stream.ReadInt32()
    v.week_reward_mailid = stream.ReadInt32()
    return v
}

func (t *EquipEquipconfig) String() string {
    return fmt.Sprintf("EquipEquipconfig{entry=%v, stone_count_for_set=%v, draw_protect_name=%v, broadcastid=%v, broadcast_least_quality=%v, week_reward_mailid=%v}", t.entry, t.stone_count_for_set, t.draw_protect_name, t.broadcastid, t.broadcast_least_quality, t.week_reward_mailid)
}

//entries
var (
    instance EquipEquipconfig
    instance2 EquipEquipconfig
)

//getters
func (t *EquipEquipconfig) Entry() string {
    return t.entry
}

func (t *EquipEquipconfig) Stone_count_for_set() int32 {
    return t.stone_count_for_set
}

func (t *EquipEquipconfig) Draw_protect_name() string {
    return t.draw_protect_name
}

func (t *EquipEquipconfig) Broadcastid() int32 {
    return t.broadcastid
}

func (t *EquipEquipconfig) Broadcast_least_quality() int32 {
    return t.broadcast_least_quality
}

func (t *EquipEquipconfig) Week_reward_mailid() int32 {
    return t.week_reward_mailid
}

func (t *EquipEquipconfigMgr) GetInstance() *EquipEquipconfig {
	return &instance
}

func (t *EquipEquipconfigMgr) GetInstance2() *EquipEquipconfig {
	return &instance2
}

type EquipEquipconfigMgr struct {
    all []*EquipEquipconfig
    entryMap map[string]*EquipEquipconfig
}

func(t *EquipEquipconfigMgr) GetAll() []*EquipEquipconfig {
    return t.all
}

func(t *EquipEquipconfigMgr) Get(entry string) *EquipEquipconfig {
    return t.entryMap[entry]
}

func (t *EquipEquipconfigMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*EquipEquipconfig, 0, cnt)
    t.entryMap = make(map[string]*EquipEquipconfig, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createEquipEquipconfig(stream)
        t.all = append(t.all, v)
        t.entryMap[v.entry] = v
        switch v.entry {
        case "Instance":
            instance = *v
        case "Instance2":
            instance2 = *v
        }
    }
}

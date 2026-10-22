package config

import "fmt"

type EquipEquip_config struct {
    entry string //入口，程序填
    stone_count_for_set int32 //形成套装的音石数量
    draw_protect_name string //保底策略名称
    broadcastid int32 //公告Id
    broadcast_least_quality int32 //公告的最低品质
    week_reward_mailid int32 //抽卡周奖励的邮件id
}

func createEquipEquip_config(stream *Stream) *EquipEquip_config {
    v := &EquipEquip_config{}
    v.entry = stream.ReadStringInPool()
    v.stone_count_for_set = stream.ReadInt32()
    v.draw_protect_name = stream.ReadStringInPool()
    v.broadcastid = stream.ReadInt32()
    v.broadcast_least_quality = stream.ReadInt32()
    v.week_reward_mailid = stream.ReadInt32()
    return v
}

func (t *EquipEquip_config) String() string {
    return fmt.Sprintf("EquipEquip_config{entry=%v, stone_count_for_set=%v, draw_protect_name=%v, broadcastid=%v, broadcast_least_quality=%v, week_reward_mailid=%v}", t.entry, t.stone_count_for_set, t.draw_protect_name, t.broadcastid, t.broadcast_least_quality, t.week_reward_mailid)
}

//getters
func (t *EquipEquip_config) Entry() string {
    return t.entry
}

func (t *EquipEquip_config) Stone_count_for_set() int32 {
    return t.stone_count_for_set
}

func (t *EquipEquip_config) Draw_protect_name() string {
    return t.draw_protect_name
}

func (t *EquipEquip_config) Broadcastid() int32 {
    return t.broadcastid
}

func (t *EquipEquip_config) Broadcast_least_quality() int32 {
    return t.broadcast_least_quality
}

func (t *EquipEquip_config) Week_reward_mailid() int32 {
    return t.week_reward_mailid
}

func (t *EquipEquip_configMgr) GetInstance() *EquipEquip_config {
    return t.symbols["Instance"]
}

func (t *EquipEquip_configMgr) GetInstance2() *EquipEquip_config {
    return t.symbols["Instance2"]
}

type EquipEquip_configMgr struct {
    all []*EquipEquip_config
    symbols map[string]*EquipEquip_config
    entryMap map[string]*EquipEquip_config
}

func(t *EquipEquip_configMgr) GetAll() []*EquipEquip_config {
    return t.all
}

func(t *EquipEquip_configMgr) Get(entry string) *EquipEquip_config {
    return t.entryMap[entry]
}

func (t *EquipEquip_configMgr) Init(stream *Stream) {
    cnt := int32(stream.ReadCount(1))
    t.all = make([]*EquipEquip_config, 0, cnt)
    t.symbols = make(map[string]*EquipEquip_config)
    t.entryMap = make(map[string]*EquipEquip_config, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createEquipEquip_config(stream)
        t.all = append(t.all, v)
        t.entryMap[v.entry] = v
        switch v.entry {
        case "Instance":
            t.symbols["Instance"] = v
        case "Instance2":
            t.symbols["Instance2"] = v
        }
    }
}

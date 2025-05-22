package config

import (
	"fmt"
	"os"
)

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
    v.entry = stream.ReadString()
    v.stone_count_for_set = stream.ReadInt32()
    v.draw_protect_name = stream.ReadString()
    v.broadcastid = stream.ReadInt32()
    v.broadcast_least_quality = stream.ReadInt32()
    v.week_reward_mailid = stream.ReadInt32()
   return v
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

func (t *EquipEquipconfig) GetStone_count_for_set() int32 {
    return t.stone_count_for_set
}

func (t *EquipEquipconfig) GetDraw_protect_name() string {
    return t.draw_protect_name
}

func (t *EquipEquipconfig) GetBroadcastid() int32 {
    return t.broadcastid
}

func (t *EquipEquipconfig) GetBroadcast_least_quality() int32 {
    return t.broadcast_least_quality
}

func (t *EquipEquipconfig) GetWeek_reward_mailid() int32 {
    return t.week_reward_mailid
}

type EquipEquipconfigMgr struct {
    all []*EquipEquipconfig
    entryMap map[string]*EquipEquipconfig
}

func(t *EquipEquipconfigMgr) GetAll() []*EquipEquipconfig {
    return t.all
}

func(t *EquipEquipconfigMgr) GetByentry(entry string) (*EquipEquipconfig,bool) {
    v, ok := t.entryMap[entry]
    return v, ok
}



func (t *EquipEquipconfigMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*AiAi, 0, cnt)
    for i := 0; i < int(cnt); i++ {
        v := &AiAi{}
        v := createEquipEquipconfig(stream)
        break
    }
}


package config

type OtherSignin struct {
    id int //礼包ID
    item2countMap map[int]int //普通奖励
    vipitem2vipcountMap map[int]int //vip奖励
    viplevel int //领取vip奖励的最低等级
    iconFile string //礼包图标
    refVipitem2vipcountMap map[int]OtherLoot
}

//getters
func (t *OtherSignin) GetId() int {
    return t.id
}

func (t *OtherSignin) GetItem2countMap() map[int]int {
    return t.item2countMap
}

func (t *OtherSignin) GetVipitem2vipcountMap() map[int]int {
    return t.vipitem2vipcountMap
}

func (t *OtherSignin) GetViplevel() int {
    return t.viplevel
}

func (t *OtherSignin) GetIconFile() string {
    return t.iconFile
}

//ref properties
func (t *OtherSignin) GetRefVipitem2vipcountMap() map[int]OtherLoot {
    return t.refVipitem2vipcountMap
}

type KeyIdViplevel struct {
    id int
    viplevel int
}

type OtherSigninMgr struct {
    all []*OtherSignin
    idMap map[int]*OtherSignin
    idViplevelMap map[KeyIdViplevel]*OtherSignin
}

func(t *OtherSigninMgr) GetAll() []*OtherSignin {
    return t.all
}

func(t *OtherSigninMgr) GetByid(id int) (*OtherSignin,bool) {
    v, ok := t.idMap[id]
    return v, ok
}

func(t *OtherSigninMgr) GetByKeyIdViplevel(id int, viplevel int) (*OtherSignin,bool) {
    v, ok := t.idViplevelMap[KeyIdViplevel{id, viplevel}]
    return v, ok
}




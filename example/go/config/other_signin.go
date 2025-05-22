package config

import (
	"fmt"
	"os"
)

type OtherSignin struct {
    id int32 //礼包ID
    item2countMap map[int32]int32 //普通奖励
    vipitem2vipcountMap map[int32]int32 //vip奖励
    viplevel int32 //领取vip奖励的最低等级
    iconFile string //礼包图标
    refVipitem2vipcountMap map[int32]OtherLoot
}

func createOtherSignin(stream *Stream) *OtherSignin {
    v := &OtherSignin{}
    v.id = stream.ReadInt32()
    v.item2countMap = stream.ReadMap[int32]int32()
    v.vipitem2vipcountMap = stream.ReadMap[int32]int32()
    v.viplevel = stream.ReadInt32()
    v.iconFile = stream.ReadString()
   return v
}

//getters
func (t *OtherSignin) GetId() int32 {
    return t.id
}

func (t *OtherSignin) GetItem2countMap() map[int32]int32 {
    return t.item2countMap
}

func (t *OtherSignin) GetVipitem2vipcountMap() map[int32]int32 {
    return t.vipitem2vipcountMap
}

func (t *OtherSignin) GetViplevel() int32 {
    return t.viplevel
}

func (t *OtherSignin) GetIconFile() string {
    return t.iconFile
}

//ref properties
func (t *OtherSignin) GetRefVipitem2vipcountMap() map[int32]OtherLoot {
    return t.refVipitem2vipcountMap
}

type KeyIdViplevel struct {
    id int32
    viplevel int32
}

type OtherSigninMgr struct {
    all []*OtherSignin
    idMap map[int32]*OtherSignin
    idViplevelMap map[KeyIdViplevel]*OtherSignin
}

func(t *OtherSigninMgr) GetAll() []*OtherSignin {
    return t.all
}

func(t *OtherSigninMgr) GetByid(id int32) (*OtherSignin,bool) {
    v, ok := t.idMap[id]
    return v, ok
}

func(t *OtherSigninMgr) GetByKeyIdViplevel(id int32, viplevel int32) (*OtherSignin,bool) {
    v, ok := t.idViplevelMap[KeyIdViplevel{id, viplevel}]
    return v, ok
}



func (t *OtherSigninMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*AiAi, 0, cnt)
    for i := 0; i < int(cnt); i++ {
        v := &AiAi{}
        v := createOtherSignin(stream)
        break
    }
}


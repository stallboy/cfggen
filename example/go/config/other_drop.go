package config

import (
	"fmt"
	"os"
)

type OtherDrop struct {
    dropid int32 //序号
    name string //名字
    items []OtherDropItem //掉落概率
    testmap map[int32]int32 //测试map block
}

func createOtherDrop(stream *Stream) *OtherDrop {
    v := &OtherDrop{}
    v.dropid = stream.ReadInt32()
    v.name = stream.ReadString()
    v.items = stream.Read[]OtherDropItem()
    v.testmap = stream.ReadMap[int32]int32()
   return v
}

//getters
func (t *OtherDrop) GetDropid() int32 {
    return t.dropid
}

func (t *OtherDrop) GetName() string {
    return t.name
}

func (t *OtherDrop) GetItems() []OtherDropItem {
    return t.items
}

func (t *OtherDrop) GetTestmap() map[int32]int32 {
    return t.testmap
}

type OtherDropMgr struct {
    all []*OtherDrop
    dropidMap map[int32]*OtherDrop
}

func(t *OtherDropMgr) GetAll() []*OtherDrop {
    return t.all
}

func(t *OtherDropMgr) GetBydropid(dropid int32) (*OtherDrop,bool) {
    v, ok := t.dropidMap[dropid]
    return v, ok
}



func (t *OtherDropMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*AiAi, 0, cnt)
    for i := 0; i < int(cnt); i++ {
        v := &AiAi{}
        v := createOtherDrop(stream)
        break
    }
}


package config

import "fmt"

type OtherDrop struct {
    dropid int32 //序号
    name *Text //名字
    items []*OtherDropItem //掉落概率
    testmap map[int32]int32 //测试map block
}

func createOtherDrop(stream *Stream) *OtherDrop {
    v := &OtherDrop{}
    v.dropid = stream.ReadInt32()
    v.name = createText(stream)
    itemsSize := stream.ReadCount(0)
    v.items = make([]*OtherDropItem, itemsSize)
    for i := 0; i < itemsSize; i++ {
        v.items[i] = createOtherDropItem(stream)
    }
    testmapSize := stream.ReadCount(0)
    v.testmap = make(map[int32]int32, testmapSize)
    for i := 0; i < testmapSize; i++ {
        var k = stream.ReadInt32()
        v.testmap[k] = stream.ReadInt32()
    }
    return v
}

func (t *OtherDrop) String() string {
    return fmt.Sprintf("OtherDrop{dropid=%v, name=%v, items=%v, testmap=%v}", t.dropid, t.name, t.items, t.testmap)
}

//getters
func (t *OtherDrop) Dropid() int32 {
    return t.dropid
}

func (t *OtherDrop) Name() *Text {
    return t.name
}

func (t *OtherDrop) Items() []*OtherDropItem {
    return t.items
}

func (t *OtherDrop) Testmap() map[int32]int32 {
    return t.testmap
}

type OtherDropMgr struct {
    all []*OtherDrop
    dropidArr []*OtherDrop
}

func(t *OtherDropMgr) GetAll() []*OtherDrop {
    return t.all
}

func(t *OtherDropMgr) Get(dropid int32) *OtherDrop {
    if dropid < 0 || int(dropid) >= len(t.dropidArr) {
        return nil
    }
    return t.dropidArr[dropid]
}

func (t *OtherDropMgr) Init(stream *Stream) {
    cnt := int32(stream.ReadCount(1))
    t.all = make([]*OtherDrop, 0, cnt)
    t.dropidArr = make([]*OtherDrop, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createOtherDrop(stream)
        t.all = append(t.all, v)
        t.dropidArr[v.dropid] = v
    }
}

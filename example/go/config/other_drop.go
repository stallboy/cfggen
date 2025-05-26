package config

type OtherDrop struct {
    dropid int32 //序号
    name string //名字
    items []*OtherDropItem //掉落概率
    testmap map[int32]int32 //测试map block
}

func createOtherDrop(stream *Stream) *OtherDrop {
    v := &OtherDrop{}
    v.dropid = stream.ReadInt32()
    v.name = stream.ReadString()
    itemsSize := stream.ReadInt32()
    v.items = make([]*OtherDropItem, itemsSize)
    for i := 0; i < int(itemsSize); i++ {
        v.items[i] = createOtherDropItem(stream)
    }
	testmapSize := stream.ReadInt32()
	v.testmap = make(map[int32]int32, testmapSize)
	for i := 0; i < int(testmapSize); i++ {
		var k = stream.ReadInt32()
		v.testmap[k] = stream.ReadInt32()
	}
    return v
}

//getters
func (t *OtherDrop) GetDropid() int32 {
    return t.dropid
}

func (t *OtherDrop) GetName() string {
    return t.name
}

func (t *OtherDrop) GetItems() []*OtherDropItem {
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

func(t *OtherDropMgr) Get(dropid int32) *OtherDrop {
    return t.dropidMap[dropid]
}



func (t *OtherDropMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*OtherDrop, 0, cnt)
    t.dropidMap = make(map[int32]*OtherDrop, cnt)

    for i := 0; i < int(cnt); i++ {
        v := createOtherDrop(stream)
        t.all = append(t.all, v)
        t.dropidMap[v.dropid] = v

    }
}


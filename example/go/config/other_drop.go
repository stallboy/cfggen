package config

type OtherDrop struct {
    dropid int //序号
    name string //名字
    items []OtherDropItem //掉落概率
    testmap map[int]int //测试map block
}

//getters
func (t *OtherDrop) GetDropid() int {
    return t.dropid
}

func (t *OtherDrop) GetName() string {
    return t.name
}

func (t *OtherDrop) GetItems() []OtherDropItem {
    return t.items
}

func (t *OtherDrop) GetTestmap() map[int]int {
    return t.testmap
}

type OtherDropMgr struct {
    all []*OtherDrop
    dropidMap map[int]*OtherDrop
}

func(t *OtherDropMgr) GetAll() []*OtherDrop {
    return t.all
}

func(t *OtherDropMgr) GetBydropid(dropid int) (*OtherDrop,bool) {
    v, ok := t.dropidMap[dropid]
    return v, ok
}




package config

type OtherKeytest struct {
    id1 int
    id2 int
    id3 int
}

//getters
func (t *OtherKeytest) GetId1() int {
    return t.id1
}

func (t *OtherKeytest) GetId2() int {
    return t.id2
}

func (t *OtherKeytest) GetId3() int {
    return t.id3
}

type KeyId1Id2 struct {
    id1 int
    id2 int
}

type KeyId1Id3 struct {
    id1 int
    id3 int
}

type KeyId2Id3 struct {
    id2 int
    id3 int
}

type OtherKeytestMgr struct {
    all []*OtherKeytest
    id1Id2Map map[KeyId1Id2]*OtherKeytest
    id1Id3Map map[KeyId1Id3]*OtherKeytest
    id2Map map[int]*OtherKeytest
    id2Id3Map map[KeyId2Id3]*OtherKeytest
}

func(t *OtherKeytestMgr) GetAll() []*OtherKeytest {
    return t.all
}

func(t *OtherKeytestMgr) GetByKeyId1Id2(id1 int, id2 int) (*OtherKeytest,bool) {
    v, ok := t.id1Id2Map[KeyId1Id2{id1, id2}]
    return v, ok
}

func(t *OtherKeytestMgr) GetByKeyId1Id3(id1 int, id3 int) (*OtherKeytest,bool) {
    v, ok := t.id1Id3Map[KeyId1Id3{id1, id3}]
    return v, ok
}

func(t *OtherKeytestMgr) GetByid2(id2 int) (*OtherKeytest,bool) {
    v, ok := t.id2Map[id2]
    return v, ok
}

func(t *OtherKeytestMgr) GetByKeyId2Id3(id2 int, id3 int) (*OtherKeytest,bool) {
    v, ok := t.id2Id3Map[KeyId2Id3{id2, id3}]
    return v, ok
}




package config

type OtherKeytest struct {
    id1 int32
    id2 int64
    id3 int32
}

func createOtherKeytest(stream *Stream) *OtherKeytest {
    v := &OtherKeytest{}
    v.id1 = stream.ReadInt32()
    v.id2 = stream.ReadInt64()
    v.id3 = stream.ReadInt32()
    return v
}

//getters
func (t *OtherKeytest) GetId1() int32 {
    return t.id1
}

func (t *OtherKeytest) GetId2() int64 {
    return t.id2
}

func (t *OtherKeytest) GetId3() int32 {
    return t.id3
}

type KeyId1Id2 struct {
    id1 int32
    id2 int64
}

type KeyId1Id3 struct {
    id1 int32
    id3 int32
}

type KeyId2Id3 struct {
    id2 int64
    id3 int32
}

type OtherKeytestMgr struct {
    all []*OtherKeytest
    id1Id2Map map[KeyId1Id2]*OtherKeytest
    id1Id3Map map[KeyId1Id3]*OtherKeytest
    id2Map map[int64]*OtherKeytest
    id2Id3Map map[KeyId2Id3]*OtherKeytest
}

func(t *OtherKeytestMgr) GetAll() []*OtherKeytest {
    return t.all
}

func(t *OtherKeytestMgr) GetByKeyId1Id2(id1 int32, id2 int64) *OtherKeytest {
    return t.id1Id2Map[KeyId1Id2{id1, id2}]
}

func(t *OtherKeytestMgr) GetByKeyId1Id3(id1 int32, id3 int32) *OtherKeytest {
    return t.id1Id3Map[KeyId1Id3{id1, id3}]
}

func(t *OtherKeytestMgr) GetByid2(id2 int64) *OtherKeytest {
    return t.id2Map[id2]
}

func(t *OtherKeytestMgr) GetByKeyId2Id3(id2 int64, id3 int32) *OtherKeytest {
    return t.id2Id3Map[KeyId2Id3{id2, id3}]
}



func (t *OtherKeytestMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*OtherKeytest, 0, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createOtherKeytest(stream)
        t.all = append(t.all, v)
    }
}


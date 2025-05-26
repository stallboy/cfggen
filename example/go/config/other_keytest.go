package config

type OtherKeytest struct {
    id1 int32
    id2 int64
    id3 int32
    ids []int32
    refIds []*OtherSignin
}

func createOtherKeytest(stream *Stream) *OtherKeytest {
    v := &OtherKeytest{}
    v.id1 = stream.ReadInt32()
    v.id2 = stream.ReadInt64()
    v.id3 = stream.ReadInt32()
    idsSize := stream.ReadInt32()
    v.ids = make([]int32, idsSize)
    for i := 0; i < int(idsSize); i++ {
        v.ids[i] = stream.ReadInt32()
    }
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

func (t *OtherKeytest) GetIds() []int32 {
    return t.ids
}

//ref properties
func (t *OtherKeytest) GetRefIds() []*OtherSignin {
	if t.refIds == nil {
		t.refIds = make([]*OtherSignin, len(t.ids))
		for i, v := range t.ids {
			t.refIds[i] = GetOtherSigninMgr().Get(v)
		}
	}
	return t.refIds
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

    id1MapList map[int32][]*OtherKeytest
    id2MapList map[int64][]*OtherKeytest
}

func(t *OtherKeytestMgr) GetAll() []*OtherKeytest {
    return t.all
}

func(t *OtherKeytestMgr) Get(id1 int32, id2 int64) *OtherKeytest {
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

func (t *OtherKeytestMgr) GetAllById1(id1 int32) []*OtherKeytest {
	if t.id1MapList == nil {
		t.id1MapList = make(map[int32][]*OtherKeytest)
		for _, item := range t.all {
			t.id1MapList[item.id1] = append(t.id1MapList[item.id1], item)
		}
	}
	return t.id1MapList[id1]
}
func (t *OtherKeytestMgr) GetAllById2(id2 int64) []*OtherKeytest {
	if t.id2MapList == nil {
		t.id2MapList = make(map[int64][]*OtherKeytest)
		for _, item := range t.all {
			t.id2MapList[item.id2] = append(t.id2MapList[item.id2], item)
		}
	}
	return t.id2MapList[id2]
}


func (t *OtherKeytestMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*OtherKeytest, 0, cnt)
    t.id1Id2Map = make(map[KeyId1Id2]*OtherKeytest, cnt)
    t.id1Id3Map = make(map[KeyId1Id3]*OtherKeytest, cnt)
    t.id2Map = make(map[int64]*OtherKeytest, cnt)
    t.id2Id3Map = make(map[KeyId2Id3]*OtherKeytest, cnt)

    for i := 0; i < int(cnt); i++ {
        v := createOtherKeytest(stream)
        t.all = append(t.all, v)
        t.id1Id2Map[KeyId1Id2{v.id1, v.id2}] = v
        t.id1Id3Map[KeyId1Id3{v.id1, v.id3}] = v
        t.id2Map[v.id2] = v
        t.id2Id3Map[KeyId2Id3{v.id2, v.id3}] = v

    }
}


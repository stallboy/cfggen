package config

import "fmt"

type OtherKey_test struct {
    id1 int32
    id2 int64
    id3 int32
    ids []int32
    enumTest string
    enumList []string
    refIds []*OtherSign_in
    refEnumTest *OtherArgCaptureMode
    refEnumList []*OtherArgCaptureMode
}

func createOtherKey_test(stream *Stream) *OtherKey_test {
    v := &OtherKey_test{}
    v.id1 = stream.ReadInt32()
    v.id2 = stream.ReadInt64()
    v.id3 = stream.ReadInt32()
    idsSize := stream.ReadCount(0)
    v.ids = make([]int32, idsSize)
    for i := 0; i < idsSize; i++ {
        v.ids[i] = stream.ReadInt32()
    }
    v.enumTest = stream.ReadStringInPool()
    enumListSize := stream.ReadCount(0)
    v.enumList = make([]string, enumListSize)
    for i := 0; i < enumListSize; i++ {
        v.enumList[i] = stream.ReadStringInPool()
    }
    return v
}

func (t *OtherKey_test) String() string {
    return fmt.Sprintf("OtherKey_test{id1=%v, id2=%v, id3=%v, ids=%v, enumTest=%v, enumList=%v}", t.id1, t.id2, t.id3, fmt.Sprintf("%v", t.ids), t.enumTest, fmt.Sprintf("%v", t.enumList))
}

//getters
func (t *OtherKey_test) Id1() int32 {
    return t.id1
}

func (t *OtherKey_test) Id2() int64 {
    return t.id2
}

func (t *OtherKey_test) Id3() int32 {
    return t.id3
}

func (t *OtherKey_test) Ids() []int32 {
    return t.ids
}

func (t *OtherKey_test) EnumTest() string {
    return t.enumTest
}

func (t *OtherKey_test) EnumList() []string {
    return t.enumList
}

//list ref
func (t *OtherKey_test) RefIds() []*OtherSign_in {
    if t.refIds == nil {
        t.refIds = make([]*OtherSign_in, len(t.ids))
        for i, v := range t.ids {
            t.refIds[i] = GetOtherSign_inMgr().Get(v)
        }
    }
    return t.refIds
}
//list ref
func (t *OtherKey_test) RefEnumList() []*OtherArgCaptureMode {
    if t.refEnumList == nil {
        t.refEnumList = make([]*OtherArgCaptureMode, len(t.enumList))
        for i, v := range t.enumList {
            t.refEnumList[i] = GetOtherArgCaptureModeMgr().Get(v)
        }
    }
    return t.refEnumList
}
func (t *OtherKey_test) RefEnumTest() *OtherArgCaptureMode {
    if t.refEnumTest == nil {
        t.refEnumTest = GetOtherArgCaptureModeMgr().Get(t.enumTest)
    }
    return t.refEnumTest
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

type OtherKey_testMgr struct {
    all []*OtherKey_test
    id1Id2Map map[KeyId1Id2]*OtherKey_test
    id1Id3Map map[KeyId1Id3]*OtherKey_test
    id2Map map[int64]*OtherKey_test
    id2Id3Map map[KeyId2Id3]*OtherKey_test
    id1MapList map[int32][]*OtherKey_test
    id2MapList map[int64][]*OtherKey_test
}

func(t *OtherKey_testMgr) GetAll() []*OtherKey_test {
    return t.all
}

func(t *OtherKey_testMgr) Get(id1 int32, id2 int64) *OtherKey_test {
    return t.id1Id2Map[KeyId1Id2{id1, id2}]
}

func(t *OtherKey_testMgr) GetByKeyId1Id3(id1 int32, id3 int32) *OtherKey_test {
    return t.id1Id3Map[KeyId1Id3{id1, id3}]
}

func(t *OtherKey_testMgr) GetByid2(id2 int64) *OtherKey_test {
    return t.id2Map[id2]
}

func(t *OtherKey_testMgr) GetByKeyId2Id3(id2 int64, id3 int32) *OtherKey_test {
    return t.id2Id3Map[KeyId2Id3{id2, id3}]
}

func (t *OtherKey_testMgr) GetAllById1(id1 int32) []*OtherKey_test {
    if t.id1MapList == nil {
        t.id1MapList = make(map[int32][]*OtherKey_test)
        for _, item := range t.all {
            t.id1MapList[item.id1] = append(t.id1MapList[item.id1], item)
        }
    }
    return t.id1MapList[id1]
}
func (t *OtherKey_testMgr) GetAllById2(id2 int64) []*OtherKey_test {
    if t.id2MapList == nil {
        t.id2MapList = make(map[int64][]*OtherKey_test)
        for _, item := range t.all {
            t.id2MapList[item.id2] = append(t.id2MapList[item.id2], item)
        }
    }
    return t.id2MapList[id2]
}
func (t *OtherKey_testMgr) Init(stream *Stream) {
    cnt := int32(stream.ReadCount(1))
    t.all = make([]*OtherKey_test, 0, cnt)
    t.id1Id2Map = make(map[KeyId1Id2]*OtherKey_test, cnt)
    t.id1Id3Map = make(map[KeyId1Id3]*OtherKey_test, cnt)
    t.id2Map = make(map[int64]*OtherKey_test, cnt)
    t.id2Id3Map = make(map[KeyId2Id3]*OtherKey_test, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createOtherKey_test(stream)
        t.all = append(t.all, v)
        t.id1Id2Map[KeyId1Id2{v.id1, v.id2}] = v
        t.id1Id3Map[KeyId1Id3{v.id1, v.id3}] = v
        t.id2Map[v.id2] = v
        t.id2Id3Map[KeyId2Id3{v.id2, v.id3}] = v
    }
}

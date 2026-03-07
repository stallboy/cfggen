package config

import "fmt"

type OtherArgCaptureMode struct {
    name string
    comment string
}

func createOtherArgCaptureMode(stream *Stream) *OtherArgCaptureMode {
    v := &OtherArgCaptureMode{}
    v.name = stream.ReadStringInPool()
    v.comment = stream.ReadStringInPool()
    return v
}

func (t *OtherArgCaptureMode) String() string {
    return fmt.Sprintf("OtherArgCaptureMode{name=%v, comment=%v}", t.name, t.comment)
}

//entries
var (
    snapshot OtherArgCaptureMode
    dynamic OtherArgCaptureMode
)

//getters
func (t *OtherArgCaptureMode) Name() string {
    return t.name
}

func (t *OtherArgCaptureMode) Comment() string {
    return t.comment
}

func (t *OtherArgCaptureModeMgr) GetSnapshot() *OtherArgCaptureMode {
	return &snapshot
}

func (t *OtherArgCaptureModeMgr) GetDynamic() *OtherArgCaptureMode {
	return &dynamic
}

type OtherArgCaptureModeMgr struct {
    all []*OtherArgCaptureMode
    nameMap map[string]*OtherArgCaptureMode
}

func(t *OtherArgCaptureModeMgr) GetAll() []*OtherArgCaptureMode {
    return t.all
}

func(t *OtherArgCaptureModeMgr) Get(name string) *OtherArgCaptureMode {
    return t.nameMap[name]
}

func (t *OtherArgCaptureModeMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*OtherArgCaptureMode, 0, cnt)
    t.nameMap = make(map[string]*OtherArgCaptureMode, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createOtherArgCaptureMode(stream)
        t.all = append(t.all, v)
        t.nameMap[v.name] = v
        switch v.name {
        case "Snapshot":
            snapshot = *v
        case "Dynamic":
            dynamic = *v
        }
    }
}

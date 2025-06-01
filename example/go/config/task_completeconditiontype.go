package config

type TaskCompleteconditiontype struct {
    id int32 //任务完成条件类型（id的范围为1-100）
    name string //程序用名字
}

func createTaskCompleteconditiontype(stream *Stream) *TaskCompleteconditiontype {
    v := &TaskCompleteconditiontype{}
    v.id = stream.ReadInt32()
    v.name = stream.ReadString()
    return v
}

//entries
var (
    killMonster TaskCompleteconditiontype
    talkNpc TaskCompleteconditiontype
    collectItem TaskCompleteconditiontype
    conditionAnd TaskCompleteconditiontype
    chat TaskCompleteconditiontype
    testNoColumn TaskCompleteconditiontype
)

//getters
func (t *TaskCompleteconditiontype) GetId() int32 {
    return t.id
}

func (t *TaskCompleteconditiontype) GetName() string {
    return t.name
}

func (t *TaskCompleteconditiontypeMgr) GetKillMonster() *TaskCompleteconditiontype {
	return &killMonster
}

func (t *TaskCompleteconditiontypeMgr) GetTalkNpc() *TaskCompleteconditiontype {
	return &talkNpc
}

func (t *TaskCompleteconditiontypeMgr) GetCollectItem() *TaskCompleteconditiontype {
	return &collectItem
}

func (t *TaskCompleteconditiontypeMgr) GetConditionAnd() *TaskCompleteconditiontype {
	return &conditionAnd
}

func (t *TaskCompleteconditiontypeMgr) GetChat() *TaskCompleteconditiontype {
	return &chat
}

func (t *TaskCompleteconditiontypeMgr) GetTestNoColumn() *TaskCompleteconditiontype {
	return &testNoColumn
}

type TaskCompleteconditiontypeMgr struct {
    all []*TaskCompleteconditiontype
    idMap map[int32]*TaskCompleteconditiontype
}

func(t *TaskCompleteconditiontypeMgr) GetAll() []*TaskCompleteconditiontype {
    return t.all
}

func(t *TaskCompleteconditiontypeMgr) Get(id int32) *TaskCompleteconditiontype {
    return t.idMap[id]
}


func (t *TaskCompleteconditiontypeMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*TaskCompleteconditiontype, 0, cnt)
    t.idMap = make(map[int32]*TaskCompleteconditiontype, cnt)

    for i := 0; i < int(cnt); i++ {
        v := createTaskCompleteconditiontype(stream)
        t.all = append(t.all, v)
        t.idMap[v.id] = v

        switch v.name {
        case "KillMonster":
            killMonster = *v
        case "TalkNpc":
            talkNpc = *v
        case "CollectItem":
            collectItem = *v
        case "ConditionAnd":
            conditionAnd = *v
        case "Chat":
            chat = *v
        case "TestNoColumn":
            testNoColumn = *v

        }
    }
}

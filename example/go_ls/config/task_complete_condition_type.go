package config

import "fmt"

type TaskComplete_condition_type struct {
    id int32 //任务完成条件类型（id的范围为1-100）
    name string //程序用名字
}

func createTaskComplete_condition_type(stream *Stream) *TaskComplete_condition_type {
    v := &TaskComplete_condition_type{}
    v.id = stream.ReadInt32()
    v.name = stream.ReadStringInPool()
    return v
}

func (t *TaskComplete_condition_type) String() string {
    return fmt.Sprintf("TaskComplete_condition_type{id=%v, name=%v}", t.id, t.name)
}

//getters
func (t *TaskComplete_condition_type) Id() int32 {
    return t.id
}

func (t *TaskComplete_condition_type) Name() string {
    return t.name
}

func (t *TaskComplete_condition_typeMgr) GetKillMonster() *TaskComplete_condition_type {
    return t.symbols["KillMonster"]
}

func (t *TaskComplete_condition_typeMgr) GetTalkNpc() *TaskComplete_condition_type {
    return t.symbols["TalkNpc"]
}

func (t *TaskComplete_condition_typeMgr) GetCollectItem() *TaskComplete_condition_type {
    return t.symbols["CollectItem"]
}

func (t *TaskComplete_condition_typeMgr) GetConditionAnd() *TaskComplete_condition_type {
    return t.symbols["ConditionAnd"]
}

func (t *TaskComplete_condition_typeMgr) GetChat() *TaskComplete_condition_type {
    return t.symbols["Chat"]
}

func (t *TaskComplete_condition_typeMgr) GetTestNoColumn() *TaskComplete_condition_type {
    return t.symbols["TestNoColumn"]
}

func (t *TaskComplete_condition_typeMgr) GetAa() *TaskComplete_condition_type {
    return t.symbols["aa"]
}

type TaskComplete_condition_typeMgr struct {
    all []*TaskComplete_condition_type
    symbols map[string]*TaskComplete_condition_type
    idMap map[int32]*TaskComplete_condition_type
}

func(t *TaskComplete_condition_typeMgr) GetAll() []*TaskComplete_condition_type {
    return t.all
}

func(t *TaskComplete_condition_typeMgr) Get(id int32) *TaskComplete_condition_type {
    return t.idMap[id]
}

func (t *TaskComplete_condition_typeMgr) Init(stream *Stream) {
    cnt := int32(stream.ReadCount(1))
    t.all = make([]*TaskComplete_condition_type, 0, cnt)
    t.symbols = make(map[string]*TaskComplete_condition_type)
    t.idMap = make(map[int32]*TaskComplete_condition_type, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createTaskComplete_condition_type(stream)
        t.all = append(t.all, v)
        t.idMap[v.id] = v
        switch v.name {
        case "KillMonster":
            t.symbols["KillMonster"] = v
        case "TalkNpc":
            t.symbols["TalkNpc"] = v
        case "CollectItem":
            t.symbols["CollectItem"] = v
        case "ConditionAnd":
            t.symbols["ConditionAnd"] = v
        case "Chat":
            t.symbols["Chat"] = v
        case "TestNoColumn":
            t.symbols["TestNoColumn"] = v
        case "Aa":
            t.symbols["aa"] = v
        }
    }
}

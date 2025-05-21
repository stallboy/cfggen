package config

type TaskCompleteconditiontype struct {
    id int //任务完成条件类型（id的范围为1-100）
    name string //程序用名字
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
func (t *TaskCompleteconditiontype) GetId() int {
    return t.id
}

func (t *TaskCompleteconditiontype) GetName() string {
    return t.name
}

type TaskCompleteconditiontypeMgr struct {
    all []*TaskCompleteconditiontype
    allMap map[int]*TaskCompleteconditiontype
}

func(t *TaskCompleteconditiontypeMgr) GetAll() []*TaskCompleteconditiontype {
    return t.all
}

func(t *TaskCompleteconditiontypeMgr) Get(key int) (*TaskCompleteconditiontype,bool) {
    v, ok := t.allMap[key]
    return v, ok
}


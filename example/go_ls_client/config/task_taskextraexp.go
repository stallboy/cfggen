package config

import "fmt"

type TaskTaskextraexp struct {
    taskid int32 //任务完成条件类型（id的范围为1-100）
    extraexp int32 //额外奖励经验
    test1 string
    test2 string
    fielda string
    fieldb string
    fieldc string
    fieldd string
}

func createTaskTaskextraexp(stream *Stream) *TaskTaskextraexp {
    v := &TaskTaskextraexp{}
    v.taskid = stream.ReadInt32()
    v.extraexp = stream.ReadInt32()
    v.test1 = stream.ReadStringInPool()
    v.test2 = stream.ReadStringInPool()
    v.fielda = stream.ReadStringInPool()
    v.fieldb = stream.ReadStringInPool()
    v.fieldc = stream.ReadStringInPool()
    v.fieldd = stream.ReadStringInPool()
    return v
}

func (t *TaskTaskextraexp) String() string {
    return fmt.Sprintf("TaskTaskextraexp{taskid=%v, extraexp=%v, test1=%v, test2=%v, fielda=%v, fieldb=%v, fieldc=%v, fieldd=%v}", t.taskid, t.extraexp, t.test1, t.test2, t.fielda, t.fieldb, t.fieldc, t.fieldd)
}

//getters
func (t *TaskTaskextraexp) Taskid() int32 {
    return t.taskid
}

func (t *TaskTaskextraexp) Extraexp() int32 {
    return t.extraexp
}

func (t *TaskTaskextraexp) Test1() string {
    return t.test1
}

func (t *TaskTaskextraexp) Test2() string {
    return t.test2
}

func (t *TaskTaskextraexp) Fielda() string {
    return t.fielda
}

func (t *TaskTaskextraexp) Fieldb() string {
    return t.fieldb
}

func (t *TaskTaskextraexp) Fieldc() string {
    return t.fieldc
}

func (t *TaskTaskextraexp) Fieldd() string {
    return t.fieldd
}

type TaskTaskextraexpMgr struct {
    all []*TaskTaskextraexp
    taskidMap map[int32]*TaskTaskextraexp
}

func(t *TaskTaskextraexpMgr) GetAll() []*TaskTaskextraexp {
    return t.all
}

func(t *TaskTaskextraexpMgr) Get(taskid int32) *TaskTaskextraexp {
    return t.taskidMap[taskid]
}

func (t *TaskTaskextraexpMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*TaskTaskextraexp, 0, cnt)
    t.taskidMap = make(map[int32]*TaskTaskextraexp, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createTaskTaskextraexp(stream)
        t.all = append(t.all, v)
        t.taskidMap[v.taskid] = v
    }
}

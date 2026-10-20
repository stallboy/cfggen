package config

import "fmt"

type TaskTask_extra_exp struct {
    taskid int32 //任务完成条件类型（id的范围为1-100）
    extraexp int32 //额外奖励经验
    test1 string
    test2 string
    fielda string
    fieldb string
    fieldc string
    fieldd string
}

func createTaskTask_extra_exp(stream *Stream) *TaskTask_extra_exp {
    v := &TaskTask_extra_exp{}
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

func (t *TaskTask_extra_exp) String() string {
    return fmt.Sprintf("TaskTask_extra_exp{taskid=%v, extraexp=%v, test1=%v, test2=%v, fielda=%v, fieldb=%v, fieldc=%v, fieldd=%v}", t.taskid, t.extraexp, t.test1, t.test2, t.fielda, t.fieldb, t.fieldc, t.fieldd)
}

//getters
func (t *TaskTask_extra_exp) Taskid() int32 {
    return t.taskid
}

func (t *TaskTask_extra_exp) Extraexp() int32 {
    return t.extraexp
}

func (t *TaskTask_extra_exp) Test1() string {
    return t.test1
}

func (t *TaskTask_extra_exp) Test2() string {
    return t.test2
}

func (t *TaskTask_extra_exp) Fielda() string {
    return t.fielda
}

func (t *TaskTask_extra_exp) Fieldb() string {
    return t.fieldb
}

func (t *TaskTask_extra_exp) Fieldc() string {
    return t.fieldc
}

func (t *TaskTask_extra_exp) Fieldd() string {
    return t.fieldd
}

type TaskTask_extra_expMgr struct {
    all []*TaskTask_extra_exp
    taskidMap map[int32]*TaskTask_extra_exp
}

func(t *TaskTask_extra_expMgr) GetAll() []*TaskTask_extra_exp {
    return t.all
}

func(t *TaskTask_extra_expMgr) Get(taskid int32) *TaskTask_extra_exp {
    return t.taskidMap[taskid]
}

func (t *TaskTask_extra_expMgr) Init(stream *Stream) {
    cnt := int32(stream.ReadCount(1))
    t.all = make([]*TaskTask_extra_exp, 0, cnt)
    t.taskidMap = make(map[int32]*TaskTask_extra_exp, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createTaskTask_extra_exp(stream)
        t.all = append(t.all, v)
        t.taskidMap[v.taskid] = v
    }
}

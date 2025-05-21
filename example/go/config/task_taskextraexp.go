package config

type TaskTaskextraexp struct {
    taskid int //任务完成条件类型（id的范围为1-100）
    extraexp int //额外奖励经验
    test1 string
    test2 string
    fielda string
    fieldb string
    fieldc string
    fieldd string
}

//getters
func (t *TaskTaskextraexp) GetTaskid() int {
    return t.taskid
}

func (t *TaskTaskextraexp) GetExtraexp() int {
    return t.extraexp
}

func (t *TaskTaskextraexp) GetTest1() string {
    return t.test1
}

func (t *TaskTaskextraexp) GetTest2() string {
    return t.test2
}

func (t *TaskTaskextraexp) GetFielda() string {
    return t.fielda
}

func (t *TaskTaskextraexp) GetFieldb() string {
    return t.fieldb
}

func (t *TaskTaskextraexp) GetFieldc() string {
    return t.fieldc
}

func (t *TaskTaskextraexp) GetFieldd() string {
    return t.fieldd
}

type TaskTaskextraexpMgr struct {
    all []*TaskTaskextraexp
    taskidMap map[int]*TaskTaskextraexp
}

func(t *TaskTaskextraexpMgr) GetAll() []*TaskTaskextraexp {
    return t.all
}

func(t *TaskTaskextraexpMgr) GetBytaskid(taskid int) (*TaskTaskextraexp,bool) {
    v, ok := t.taskidMap[taskid]
    return v, ok
}




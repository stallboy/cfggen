package config

type TaskTask struct {
    taskid int //任务完成条件类型（id的范围为1-100）
    name []string //程序用名字
    nexttask int
    completecondition TaskCompletecondition
    exp int
    testDefaultBean TaskTestDefaultBean //测试
    nullableRefTaskid *TaskTaskextraexp
    nullableRefNexttask *TaskTask
}

//getters
func (t *TaskTask) GetTaskid() int {
    return t.taskid
}

func (t *TaskTask) GetName() []string {
    return t.name
}

func (t *TaskTask) GetNexttask() int {
    return t.nexttask
}

func (t *TaskTask) GetCompletecondition() TaskCompletecondition {
    return t.completecondition
}

func (t *TaskTask) GetExp() int {
    return t.exp
}

func (t *TaskTask) GetTestDefaultBean() TaskTestDefaultBean {
    return t.testDefaultBean
}

//ref properties
func (t *TaskTask) GetNullableRefTaskid() *TaskTaskextraexp {
    return t.nullableRefTaskid
}
func (t *TaskTask) GetNullableRefNexttask() *TaskTask {
    return t.nullableRefNexttask
}

type TaskTaskMgr struct {
    all []*TaskTask
    taskidMap map[int]*TaskTask
}

func(t *TaskTaskMgr) GetAll() []*TaskTask {
    return t.all
}

func(t *TaskTaskMgr) GetBytaskid(taskid int) (*TaskTask,bool) {
    v, ok := t.taskidMap[taskid]
    return v, ok
}




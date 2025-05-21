package config

type TaskTask2 struct {
    taskid int //任务完成条件类型（id的范围为1-100）
    name []string
    nexttask int
    completecondition TaskCompletecondition
    exp int
    testBool bool
    testString string
    testStruct Position
    testList []int
    testListStruct []Position
    testListInterface []AiTriggerTick
    nullableRefTaskid *TaskTaskextraexp
    nullableRefNexttask *TaskTask
}

//getters
func (t *TaskTask2) GetTaskid() int {
    return t.taskid
}

func (t *TaskTask2) GetName() []string {
    return t.name
}

func (t *TaskTask2) GetNexttask() int {
    return t.nexttask
}

func (t *TaskTask2) GetCompletecondition() TaskCompletecondition {
    return t.completecondition
}

func (t *TaskTask2) GetExp() int {
    return t.exp
}

func (t *TaskTask2) GetTestBool() bool {
    return t.testBool
}

func (t *TaskTask2) GetTestString() string {
    return t.testString
}

func (t *TaskTask2) GetTestStruct() Position {
    return t.testStruct
}

func (t *TaskTask2) GetTestList() []int {
    return t.testList
}

func (t *TaskTask2) GetTestListStruct() []Position {
    return t.testListStruct
}

func (t *TaskTask2) GetTestListInterface() []AiTriggerTick {
    return t.testListInterface
}

//ref properties
func (t *TaskTask2) GetNullableRefTaskid() *TaskTaskextraexp {
    return t.nullableRefTaskid
}
func (t *TaskTask2) GetNullableRefNexttask() *TaskTask {
    return t.nullableRefNexttask
}

type TaskTask2Mgr struct {
    all []*TaskTask2
    taskidMap map[int]*TaskTask2
}

func(t *TaskTask2Mgr) GetAll() []*TaskTask2 {
    return t.all
}

func(t *TaskTask2Mgr) GetBytaskid(taskid int) (*TaskTask2,bool) {
    v, ok := t.taskidMap[taskid]
    return v, ok
}




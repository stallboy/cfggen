package config

type TaskTask struct {
    taskid int32 //任务完成条件类型（id的范围为1-100）
    name []string //程序用名字
    nexttask int32
    completecondition TaskCompletecondition
    exp int32
    testDefaultBean *TaskTestDefaultBean //测试
    nullableRefTaskid *TaskTaskextraexp
    nullableRefNexttask *TaskTask
}

func createTaskTask(stream *Stream) *TaskTask {
    v := &TaskTask{}
    v.taskid = stream.ReadInt32()
    nameSize := stream.ReadInt32()
    v.name = make([]string, nameSize)
    for i := 0; i < int(nameSize); i++ {
        v.name = append(v.name, stream.ReadString())
    }
    v.nexttask = stream.ReadInt32()
    v.completecondition = createTaskCompletecondition(stream)
    v.exp = stream.ReadInt32()
    v.testDefaultBean = createTaskTestDefaultBean(stream)
    return v
}

//getters
func (t *TaskTask) GetTaskid() int32 {
    return t.taskid
}

func (t *TaskTask) GetName() []string {
    return t.name
}

func (t *TaskTask) GetNexttask() int32 {
    return t.nexttask
}

func (t *TaskTask) GetCompletecondition() TaskCompletecondition {
    return t.completecondition
}

func (t *TaskTask) GetExp() int32 {
    return t.exp
}

func (t *TaskTask) GetTestDefaultBean() *TaskTestDefaultBean {
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
    taskidMap map[int32]*TaskTask
}

func(t *TaskTaskMgr) GetAll() []*TaskTask {
    return t.all
}

func(t *TaskTaskMgr) GetBytaskid(taskid int32) (*TaskTask,bool) {
    v, ok := t.taskidMap[taskid]
    return v, ok
}



func (t *TaskTaskMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*TaskTask, 0, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createTaskTask(stream)
        t.all = append(t.all, v)
    }
}


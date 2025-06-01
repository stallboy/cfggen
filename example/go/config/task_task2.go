package config

type TaskTask2 struct {
    taskid int32 //任务完成条件类型（id的范围为1-100）
    name []string
    nexttask int32
    completecondition TaskCompletecondition
    exp int32
    testBool bool
    testString string
    testStruct *Position
    testList []int32
    testListStruct []*Position
    testListInterface []AiTriggerTick
    nullableRefTaskid *TaskTaskextraexp
    nullableRefNexttask *TaskTask
}

func createTaskTask2(stream *Stream) *TaskTask2 {
    v := &TaskTask2{}
    v.taskid = stream.ReadInt32()
    nameSize := stream.ReadInt32()
    v.name = make([]string, nameSize)
    for i := 0; i < int(nameSize); i++ {
        v.name[i] = stream.ReadString()
    }
    v.nexttask = stream.ReadInt32()
    v.completecondition = createTaskCompletecondition(stream)
    v.exp = stream.ReadInt32()
    v.testBool = stream.ReadBool()
    v.testString = stream.ReadString()
    v.testStruct = createPosition(stream)
    testListSize := stream.ReadInt32()
    v.testList = make([]int32, testListSize)
    for i := 0; i < int(testListSize); i++ {
        v.testList[i] = stream.ReadInt32()
    }
    testListStructSize := stream.ReadInt32()
    v.testListStruct = make([]*Position, testListStructSize)
    for i := 0; i < int(testListStructSize); i++ {
        v.testListStruct[i] = createPosition(stream)
    }
    testListInterfaceSize := stream.ReadInt32()
    v.testListInterface = make([]AiTriggerTick, testListInterfaceSize)
    for i := 0; i < int(testListInterfaceSize); i++ {
        v.testListInterface[i] = createAiTriggerTick(stream)
    }
    return v
}

//getters
func (t *TaskTask2) GetTaskid() int32 {
    return t.taskid
}

func (t *TaskTask2) GetName() []string {
    return t.name
}

func (t *TaskTask2) GetNexttask() int32 {
    return t.nexttask
}

func (t *TaskTask2) GetCompletecondition() TaskCompletecondition {
    return t.completecondition
}

func (t *TaskTask2) GetExp() int32 {
    return t.exp
}

func (t *TaskTask2) GetTestBool() bool {
    return t.testBool
}

func (t *TaskTask2) GetTestString() string {
    return t.testString
}

func (t *TaskTask2) GetTestStruct() *Position {
    return t.testStruct
}

func (t *TaskTask2) GetTestList() []int32 {
    return t.testList
}

func (t *TaskTask2) GetTestListStruct() []*Position {
    return t.testListStruct
}

func (t *TaskTask2) GetTestListInterface() []AiTriggerTick {
    return t.testListInterface
}

func (t *TaskTask2) GetNullableRefTaskid() *TaskTaskextraexp {
    if t.nullableRefTaskid == nil {
        t.nullableRefTaskid = GetTaskTaskextraexpMgr().Get(t.taskid)
    }
    return t.nullableRefTaskid
}

func (t *TaskTask2) GetNullableRefNexttask() *TaskTask {
    if t.nullableRefNexttask == nil {
        t.nullableRefNexttask = GetTaskTaskMgr().Get(t.nexttask)
    }
    return t.nullableRefNexttask
}

type TaskTask2Mgr struct {
    all []*TaskTask2
    taskidMap map[int32]*TaskTask2

}

func(t *TaskTask2Mgr) GetAll() []*TaskTask2 {
    return t.all
}

func(t *TaskTask2Mgr) Get(taskid int32) *TaskTask2 {
    return t.taskidMap[taskid]
}



func (t *TaskTask2Mgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*TaskTask2, 0, cnt)
    t.taskidMap = make(map[int32]*TaskTask2, cnt)

    for i := 0; i < int(cnt); i++ {
        v := createTaskTask2(stream)
        t.all = append(t.all, v)
        t.taskidMap[v.taskid] = v

    }
}


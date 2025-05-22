package config

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
    v.test1 = stream.ReadString()
    v.test2 = stream.ReadString()
    v.fielda = stream.ReadString()
    v.fieldb = stream.ReadString()
    v.fieldc = stream.ReadString()
    v.fieldd = stream.ReadString()
    return v
}

//getters
func (t *TaskTaskextraexp) GetTaskid() int32 {
    return t.taskid
}

func (t *TaskTaskextraexp) GetExtraexp() int32 {
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
    taskidMap map[int32]*TaskTaskextraexp
}

func(t *TaskTaskextraexpMgr) GetAll() []*TaskTaskextraexp {
    return t.all
}

func(t *TaskTaskextraexpMgr) GetBytaskid(taskid int32) (*TaskTaskextraexp,bool) {
    v, ok := t.taskidMap[taskid]
    return v, ok
}



func (t *TaskTaskextraexpMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*TaskTaskextraexp, 0, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createTaskTaskextraexp(stream)
        t.all = append(t.all, v)
    }
}


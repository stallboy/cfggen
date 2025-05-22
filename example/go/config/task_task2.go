package config

import (
	"fmt"
	"os"
)

type TaskTask2 struct {
    taskid int32 //任务完成条件类型（id的范围为1-100）
    name []string
    nexttask int32
    completecondition TaskCompletecondition
    exp int32
    testBool bool
    testString string
    testStruct Position
    testList []int32
    testListStruct []Position
    testListInterface []AiTriggerTick
    nullableRefTaskid *TaskTaskextraexp
    nullableRefNexttask *TaskTask
}

func createTaskTask2(stream *Stream) *TaskTask2 {
    v := &TaskTask2{}
    v.taskid = stream.ReadInt32()
    v.name = stream.Read[]string()
    v.nexttask = stream.ReadInt32()
    v.completecondition = stream.ReadTaskCompletecondition()
    v.exp = stream.ReadInt32()
    v.testBool = stream.ReadBool()
    v.testString = stream.ReadString()
    v.testStruct = stream.ReadPosition()
    v.testList = stream.Read[]int32()
    v.testListStruct = stream.Read[]Position()
    v.testListInterface = stream.Read[]AiTriggerTick()
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

func (t *TaskTask2) GetTestStruct() Position {
    return t.testStruct
}

func (t *TaskTask2) GetTestList() []int32 {
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
    taskidMap map[int32]*TaskTask2
}

func(t *TaskTask2Mgr) GetAll() []*TaskTask2 {
    return t.all
}

func(t *TaskTask2Mgr) GetBytaskid(taskid int32) (*TaskTask2,bool) {
    v, ok := t.taskidMap[taskid]
    return v, ok
}



func (t *TaskTask2Mgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*AiAi, 0, cnt)
    for i := 0; i < int(cnt); i++ {
        v := &AiAi{}
        v := createTaskTask2(stream)
        break
    }
}


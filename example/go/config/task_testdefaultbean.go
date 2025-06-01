package config

type TaskTestDefaultBean struct {
    testInt int32
    testBool bool
    testString string
    testSubBean *Position
    testList []int32
    testList2 []int32
    testMap map[int32]string
}

func createTaskTestDefaultBean(stream *Stream) *TaskTestDefaultBean {
    v := &TaskTestDefaultBean{}
    v.testInt = stream.ReadInt32()
    v.testBool = stream.ReadBool()
    v.testString = stream.ReadString()
    v.testSubBean = createPosition(stream)
    testListSize := stream.ReadInt32()
    v.testList = make([]int32, testListSize)
    for i := 0; i < int(testListSize); i++ {
        v.testList[i] = stream.ReadInt32()
    }
    testList2Size := stream.ReadInt32()
    v.testList2 = make([]int32, testList2Size)
    for i := 0; i < int(testList2Size); i++ {
        v.testList2[i] = stream.ReadInt32()
    }
    testMapSize := stream.ReadInt32()
    v.testMap = make(map[int32]string, testMapSize)
    for i := 0; i < int(testMapSize); i++ {
        var k = stream.ReadInt32()
        v.testMap[k] = stream.ReadString()
    }
    return v
}

//getters
func (t *TaskTestDefaultBean) GetTestInt() int32 {
    return t.testInt
}

func (t *TaskTestDefaultBean) GetTestBool() bool {
    return t.testBool
}

func (t *TaskTestDefaultBean) GetTestString() string {
    return t.testString
}

func (t *TaskTestDefaultBean) GetTestSubBean() *Position {
    return t.testSubBean
}

func (t *TaskTestDefaultBean) GetTestList() []int32 {
    return t.testList
}

func (t *TaskTestDefaultBean) GetTestList2() []int32 {
    return t.testList2
}

func (t *TaskTestDefaultBean) GetTestMap() map[int32]string {
    return t.testMap
}


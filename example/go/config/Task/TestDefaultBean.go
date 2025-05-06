package task

type TestDefaultBean struct {
    testInt int
    testBool bool
    testString string
    testSubBean config.Position
    testList []int
    testList2 []int
    testMap KeyedList<int, string>
}

//getters
func (t *TestDefaultBean) GetTestInt() int {
    return t.testInt
}

func (t *TestDefaultBean) GetTestBool() bool {
    return t.testBool
}

func (t *TestDefaultBean) GetTestString() string {
    return t.testString
}

func (t *TestDefaultBean) GetTestSubBean() config.Position {
    return t.testSubBean
}

func (t *TestDefaultBean) GetTestList() []int {
    return t.testList
}

func (t *TestDefaultBean) GetTestList2() []int {
    return t.testList2
}

func (t *TestDefaultBean) GetTestMap() KeyedList<int, string> {
    return t.testMap
}


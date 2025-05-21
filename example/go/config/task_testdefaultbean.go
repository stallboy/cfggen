package config

type TaskTestDefaultBean struct {
    testInt int
    testBool bool
    testString string
    testSubBean Position
    testList []int
    testList2 []int
    testMap map[int]string
}

//getters
func (t *TaskTestDefaultBean) GetTestInt() int {
    return t.testInt
}

func (t *TaskTestDefaultBean) GetTestBool() bool {
    return t.testBool
}

func (t *TaskTestDefaultBean) GetTestString() string {
    return t.testString
}

func (t *TaskTestDefaultBean) GetTestSubBean() Position {
    return t.testSubBean
}

func (t *TaskTestDefaultBean) GetTestList() []int {
    return t.testList
}

func (t *TaskTestDefaultBean) GetTestList2() []int {
    return t.testList2
}

func (t *TaskTestDefaultBean) GetTestMap() map[int]string {
    return t.testMap
}


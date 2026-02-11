package config

import "fmt"

type TaskCompleteconditionConditionAnd struct {
    cond1 TaskCompletecondition
    cond2 TaskCompletecondition
}

func createTaskCompleteconditionConditionAnd(stream *Stream) *TaskCompleteconditionConditionAnd {
    v := &TaskCompleteconditionConditionAnd{}
    v.cond1 = createTaskCompletecondition(stream)
    v.cond2 = createTaskCompletecondition(stream)
    return v
}

func (t *TaskCompleteconditionConditionAnd) String() string {
    return fmt.Sprintf("TaskCompleteconditionConditionAnd{cond1=%v, cond2=%v}", fmt.Sprintf("%v", t.cond1), fmt.Sprintf("%v", t.cond2))
}

//getters
func (t *TaskCompleteconditionConditionAnd) Cond1() TaskCompletecondition {
    return t.cond1
}

func (t *TaskCompleteconditionConditionAnd) Cond2() TaskCompletecondition {
    return t.cond2
}


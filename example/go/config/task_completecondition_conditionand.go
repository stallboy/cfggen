package config

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

//getters
func (t *TaskCompleteconditionConditionAnd) GetCond1() TaskCompletecondition {
    return t.cond1
}

func (t *TaskCompleteconditionConditionAnd) GetCond2() TaskCompletecondition {
    return t.cond2
}



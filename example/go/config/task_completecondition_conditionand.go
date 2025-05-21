package config

type TaskCompleteconditionConditionAnd struct {
    cond1 TaskCompletecondition
    cond2 TaskCompletecondition
}

//getters
func (t *TaskCompleteconditionConditionAnd) GetCond1() TaskCompletecondition {
    return t.cond1
}

func (t *TaskCompleteconditionConditionAnd) GetCond2() TaskCompletecondition {
    return t.cond2
}


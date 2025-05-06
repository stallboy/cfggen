package completecondition

type ConditionAnd struct {
    cond1 config.Task.Completecondition
    cond2 config.Task.Completecondition
}

//is config.Task.Completecondition
func getType() string {
    return "ConditionAnd"
}

//getters
func (t *ConditionAnd) GetCond1() config.Task.Completecondition {
    return t.cond1
}

func (t *ConditionAnd) GetCond2() config.Task.Completecondition {
    return t.cond2
}


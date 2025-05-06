package triggertick

type ConstValue struct {
    value int
}

//is config.Ai.TriggerTick
//getters
func (t *ConstValue) GetValue() int {
    return t.value
}


package triggertick

type ByLevel struct {
    init int
    coefficient float
}

//is config.Ai.TriggerTick
//getters
func (t *ByLevel) GetInit() int {
    return t.init
}

func (t *ByLevel) GetCoefficient() float {
    return t.coefficient
}


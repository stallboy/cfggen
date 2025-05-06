package triggertick

type ByServerUpDay struct {
    init int
    coefficient1 float
    coefficient2 float
}

//is config.Ai.TriggerTick
//getters
func (t *ByServerUpDay) GetInit() int {
    return t.init
}

func (t *ByServerUpDay) GetCoefficient1() float {
    return t.coefficient1
}

func (t *ByServerUpDay) GetCoefficient2() float {
    return t.coefficient2
}


package config

type AiTriggertickByServerUpDay struct {
    init int
    coefficient1 float64
    coefficient2 float64
}

//getters
func (t *AiTriggertickByServerUpDay) GetInit() int {
    return t.init
}

func (t *AiTriggertickByServerUpDay) GetCoefficient1() float64 {
    return t.coefficient1
}

func (t *AiTriggertickByServerUpDay) GetCoefficient2() float64 {
    return t.coefficient2
}


package config

type AiTriggertickByLevel struct {
    init int
    coefficient float64
}

//getters
func (t *AiTriggertickByLevel) GetInit() int {
    return t.init
}

func (t *AiTriggertickByLevel) GetCoefficient() float64 {
    return t.coefficient
}


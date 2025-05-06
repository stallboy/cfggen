package config

type Range struct {
    min int //最小
    max int //最大
}

//getters
func (t *Range) GetMin() int {
    return t.min
}

func (t *Range) GetMax() int {
    return t.max
}


package config

type AiTriggertickConstValue struct {
    value int
}

//getters
func (t *AiTriggertickConstValue) GetValue() int {
    return t.value
}


package equip

type TestPackBean struct {
    name string
    range config.Range
}

//getters
func (t *TestPackBean) GetName() string {
    return t.name
}

func (t *TestPackBean) GetRange() config.Range {
    return t.range
}


package config

type EquipTestPackBean struct {
    name string
    iRange Range
}

//getters
func (t *EquipTestPackBean) GetName() string {
    return t.name
}

func (t *EquipTestPackBean) GetIRange() Range {
    return t.iRange
}


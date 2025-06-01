package config

type EquipTestPackBean struct {
    name string
    iRange *Range
}

func createEquipTestPackBean(stream *Stream) *EquipTestPackBean {
    v := &EquipTestPackBean{}
    v.name = stream.ReadString()
    v.iRange = createRange(stream)
    return v
}

//getters
func (t *EquipTestPackBean) GetName() string {
    return t.name
}

func (t *EquipTestPackBean) GetIRange() *Range {
    return t.iRange
}



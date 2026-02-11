package config

import "fmt"

type EquipTestPackBean struct {
    name string
    iRange *Range
}

func createEquipTestPackBean(stream *Stream) *EquipTestPackBean {
    v := &EquipTestPackBean{}
    v.name = stream.ReadStringInPool()
    v.iRange = createRange(stream)
    return v
}

func (t *EquipTestPackBean) String() string {
    return fmt.Sprintf("EquipTestPackBean{name=%v, iRange=%v}", t.name, fmt.Sprintf("%v", t.iRange))
}

//getters
func (t *EquipTestPackBean) Name() string {
    return t.name
}

func (t *EquipTestPackBean) IRange() *Range {
    return t.iRange
}


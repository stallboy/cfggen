package config

import "fmt"

type AiTriggerTickByServerUpDay struct {
    init int32
    coefficient1 float32
    coefficient2 float32
}

func createAiTriggerTickByServerUpDay(stream *Stream) *AiTriggerTickByServerUpDay {
    v := &AiTriggerTickByServerUpDay{}
    v.init = stream.ReadInt32()
    v.coefficient1 = stream.ReadFloat32()
    v.coefficient2 = stream.ReadFloat32()
    return v
}

func (t *AiTriggerTickByServerUpDay) String() string {
    return fmt.Sprintf("AiTriggerTickByServerUpDay{init=%v, coefficient1=%v, coefficient2=%v}", t.init, t.coefficient1, t.coefficient2)
}

//getters
func (t *AiTriggerTickByServerUpDay) Init() int32 {
    return t.init
}

func (t *AiTriggerTickByServerUpDay) Coefficient1() float32 {
    return t.coefficient1
}

func (t *AiTriggerTickByServerUpDay) Coefficient2() float32 {
    return t.coefficient2
}


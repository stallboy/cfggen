package config

import "fmt"

type AiTriggerTickByLevel struct {
    init int32
    coefficient float32
}

func createAiTriggerTickByLevel(stream *Stream) *AiTriggerTickByLevel {
    v := &AiTriggerTickByLevel{}
    v.init = stream.ReadInt32()
    v.coefficient = stream.ReadFloat32()
    return v
}

func (t *AiTriggerTickByLevel) String() string {
    return fmt.Sprintf("AiTriggerTickByLevel{init=%v, coefficient=%v}", t.init, t.coefficient)
}

//getters
func (t *AiTriggerTickByLevel) Init() int32 {
    return t.init
}

func (t *AiTriggerTickByLevel) Coefficient() float32 {
    return t.coefficient
}


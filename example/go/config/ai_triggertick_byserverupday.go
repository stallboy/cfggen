package config

type AiTriggertickByServerUpDay struct {
    init int32
    coefficient1 float32
    coefficient2 float32
}

func createAiTriggertickByServerUpDay(stream *Stream) *AiTriggertickByServerUpDay {
    v := &AiTriggertickByServerUpDay{}
    v.init = stream.ReadInt32()
    v.coefficient1 = stream.ReadFloat32()
    v.coefficient2 = stream.ReadFloat32()
    return v
}

//getters
func (t *AiTriggertickByServerUpDay) Init() int32 {
    return t.init
}

func (t *AiTriggertickByServerUpDay) Coefficient1() float32 {
    return t.coefficient1
}

func (t *AiTriggertickByServerUpDay) Coefficient2() float32 {
    return t.coefficient2
}


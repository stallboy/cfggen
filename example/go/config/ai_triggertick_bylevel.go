package config

type AiTriggertickByLevel struct {
    init int32
    coefficient float32
}

func createAiTriggertickByLevel(stream *Stream) *AiTriggertickByLevel {
    v := &AiTriggertickByLevel{}
    v.init = stream.ReadInt32()
    v.coefficient = stream.ReadFloat32()
    return v
}

//getters
func (t *AiTriggertickByLevel) GetInit() int32 {
    return t.init
}

func (t *AiTriggertickByLevel) GetCoefficient() float32 {
    return t.coefficient
}



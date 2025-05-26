package config

type AiTriggertickConstValue struct {
    value int32
}

func createAiTriggertickConstValue(stream *Stream) *AiTriggertickConstValue {
    v := &AiTriggertickConstValue{}
    v.value = stream.ReadInt32()
    return v
}

//getters
func (t *AiTriggertickConstValue) GetValue() int32 {
    return t.value
}


package config

import "fmt"

type AiTriggertickConstValue struct {
    value int32
}

func createAiTriggertickConstValue(stream *Stream) *AiTriggertickConstValue {
    v := &AiTriggertickConstValue{}
    v.value = stream.ReadInt32()
    return v
}

func (t *AiTriggertickConstValue) String() string {
    return fmt.Sprintf("AiTriggertickConstValue{value=%v}", t.value)
}

//getters
func (t *AiTriggertickConstValue) Value() int32 {
    return t.value
}


package config

import "fmt"

type AiTriggerTickConstValue struct {
    value int32
}

func createAiTriggerTickConstValue(stream *Stream) *AiTriggerTickConstValue {
    v := &AiTriggerTickConstValue{}
    v.value = stream.ReadInt32()
    return v
}

func (t *AiTriggerTickConstValue) String() string {
    return fmt.Sprintf("AiTriggerTickConstValue{value=%v}", t.value)
}

//getters
func (t *AiTriggerTickConstValue) Value() int32 {
    return t.value
}


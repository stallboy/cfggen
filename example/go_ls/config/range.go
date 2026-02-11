package config

import "fmt"

type Range struct {
    min int32 //最小
    max int32 //最大
}

func createRange(stream *Stream) *Range {
    v := &Range{}
    v.min = stream.ReadInt32()
    v.max = stream.ReadInt32()
    return v
}

func (t *Range) String() string {
    return fmt.Sprintf("Range{min=%v, max=%v}", t.min, t.max)
}

//getters
func (t *Range) Min() int32 {
    return t.min
}

func (t *Range) Max() int32 {
    return t.max
}


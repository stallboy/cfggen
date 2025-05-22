package config

import (
	"fmt"
	"os"
)

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

//getters
func (t *Range) GetMin() int32 {
    return t.min
}

func (t *Range) GetMax() int32 {
    return t.max
}


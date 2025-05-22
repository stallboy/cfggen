package config

import (
	"fmt"
	"os"
)

type TaskCompleteconditionKillMonster struct {
    monsterid int32
    count int32
    refMonsterid *OtherMonster
}

func createTaskCompleteconditionKillMonster(stream *Stream) *TaskCompleteconditionKillMonster {
    v := &TaskCompleteconditionKillMonster{}
    v.monsterid = stream.ReadInt32()
    v.count = stream.ReadInt32()
   return v
}

//getters
func (t *TaskCompleteconditionKillMonster) GetMonsterid() int32 {
    return t.monsterid
}

func (t *TaskCompleteconditionKillMonster) GetCount() int32 {
    return t.count
}

//ref properties
func (t *TaskCompleteconditionKillMonster) GetRefMonsterid() *OtherMonster {
    return t.refMonsterid
}


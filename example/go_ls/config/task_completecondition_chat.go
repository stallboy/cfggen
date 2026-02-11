package config

import "fmt"

type TaskCompleteconditionChat struct {
    msg string
}

func createTaskCompleteconditionChat(stream *Stream) *TaskCompleteconditionChat {
    v := &TaskCompleteconditionChat{}
    v.msg = stream.ReadStringInPool()
    return v
}

func (t *TaskCompleteconditionChat) String() string {
    return fmt.Sprintf("TaskCompleteconditionChat{msg=%v}", t.msg)
}

//getters
func (t *TaskCompleteconditionChat) Msg() string {
    return t.msg
}


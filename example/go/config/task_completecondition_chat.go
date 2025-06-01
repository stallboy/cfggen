package config

type TaskCompleteconditionChat struct {
    msg string
}

func createTaskCompleteconditionChat(stream *Stream) *TaskCompleteconditionChat {
    v := &TaskCompleteconditionChat{}
    v.msg = stream.ReadString()
    return v
}

//getters
func (t *TaskCompleteconditionChat) GetMsg() string {
    return t.msg
}



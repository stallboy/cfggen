package config

type TaskCompleteconditionChat struct {
    msg string
}

//getters
func (t *TaskCompleteconditionChat) GetMsg() string {
    return t.msg
}


package config

type TaskCompleteconditionTestNoColumn struct {
    _identity byte
}

func createTaskCompleteconditionTestNoColumn(_ *Stream) *TaskCompleteconditionTestNoColumn {
    v := &TaskCompleteconditionTestNoColumn{}
    return v
}

func (t *TaskCompleteconditionTestNoColumn) String() string {
    return "TaskCompleteconditionTestNoColumn{}"
}


package completecondition

type Chat struct {
    msg string
}

//is config.Task.Completecondition
func getType() string {
    return "Chat"
}

//getters
func (t *Chat) GetMsg() string {
    return t.msg
}


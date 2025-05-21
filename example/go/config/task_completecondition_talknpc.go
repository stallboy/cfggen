package config

type TaskCompleteconditionTalkNpc struct {
    npcid int
}

//getters
func (t *TaskCompleteconditionTalkNpc) GetNpcid() int {
    return t.npcid
}


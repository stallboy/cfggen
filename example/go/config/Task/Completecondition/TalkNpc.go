package completecondition

type TalkNpc struct {
    npcid int
}

//is config.Task.Completecondition
func getType() string {
    return "TalkNpc"
}

//getters
func (t *TalkNpc) GetNpcid() int {
    return t.npcid
}


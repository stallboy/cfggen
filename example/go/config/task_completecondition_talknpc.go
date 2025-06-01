package config

type TaskCompleteconditionTalkNpc struct {
    npcid int32
}

func createTaskCompleteconditionTalkNpc(stream *Stream) *TaskCompleteconditionTalkNpc {
    v := &TaskCompleteconditionTalkNpc{}
    v.npcid = stream.ReadInt32()
    return v
}

//getters
func (t *TaskCompleteconditionTalkNpc) Npcid() int32 {
    return t.npcid
}


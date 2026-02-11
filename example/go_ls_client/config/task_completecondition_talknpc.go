package config

import "fmt"

type TaskCompleteconditionTalkNpc struct {
    npcid int32
}

func createTaskCompleteconditionTalkNpc(stream *Stream) *TaskCompleteconditionTalkNpc {
    v := &TaskCompleteconditionTalkNpc{}
    v.npcid = stream.ReadInt32()
    return v
}

func (t *TaskCompleteconditionTalkNpc) String() string {
    return fmt.Sprintf("TaskCompleteconditionTalkNpc{npcid=%v}", t.npcid)
}

//getters
func (t *TaskCompleteconditionTalkNpc) Npcid() int32 {
    return t.npcid
}


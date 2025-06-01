package config

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
func (t *TaskCompleteconditionKillMonster) Monsterid() int32 {
    return t.monsterid
}

func (t *TaskCompleteconditionKillMonster) Count() int32 {
    return t.count
}

func (t *TaskCompleteconditionKillMonster) GetRefMonsterid() *OtherMonster {
    if t.refMonsterid == nil {
        t.refMonsterid = GetOtherMonsterMgr().Get(t.monsterid)
    }
    return t.refMonsterid
}


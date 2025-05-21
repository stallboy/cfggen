package config

type TaskCompleteconditionKillMonster struct {
    monsterid int
    count int
    refMonsterid *OtherMonster
}

//getters
func (t *TaskCompleteconditionKillMonster) GetMonsterid() int {
    return t.monsterid
}

func (t *TaskCompleteconditionKillMonster) GetCount() int {
    return t.count
}

//ref properties
func (t *TaskCompleteconditionKillMonster) GetRefMonsterid() *OtherMonster {
    return t.refMonsterid
}


package completecondition

import (
    config_other "config/other"
)

type KillMonster struct {
    monsterid int
    count int
    refMonsterid config_other.Monster
}

//is config.Task.Completecondition
func getType() string {
    return "KillMonster"
}

//getters
func (t *KillMonster) GetMonsterid() int {
    return t.monsterid
}

func (t *KillMonster) GetCount() int {
    return t.count
}

//ref properties
func (t *KillMonster) GetRefMonsterid() config_other.Monster {
    return t.refMonsterid
}


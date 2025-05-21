package config

type OtherMonster struct {
    id int
    posList []Position
}

//getters
func (t *OtherMonster) GetId() int {
    return t.id
}

func (t *OtherMonster) GetPosList() []Position {
    return t.posList
}

type OtherMonsterMgr struct {
    all []*OtherMonster
    idMap map[int]*OtherMonster
}

func(t *OtherMonsterMgr) GetAll() []*OtherMonster {
    return t.all
}

func(t *OtherMonsterMgr) GetByid(id int) (*OtherMonster,bool) {
    v, ok := t.idMap[id]
    return v, ok
}




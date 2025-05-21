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
    allMap map[int]*OtherMonster
}

func(t *OtherMonsterMgr) GetAll() []*OtherMonster {
    return t.all
}

func(t *OtherMonsterMgr) Get(key int) (*OtherMonster,bool) {
    v, ok := t.allMap[key]
    return v, ok
}


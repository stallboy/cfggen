package config

import (
	"fmt"
	"os"
)

type OtherMonster struct {
    id int32
    posList []Position
}

func createOtherMonster(stream *Stream) *OtherMonster {
    v := &OtherMonster{}
    v.id = stream.ReadInt32()
    v.posList = stream.Read[]Position()
   return v
}

//getters
func (t *OtherMonster) GetId() int32 {
    return t.id
}

func (t *OtherMonster) GetPosList() []Position {
    return t.posList
}

type OtherMonsterMgr struct {
    all []*OtherMonster
    idMap map[int32]*OtherMonster
}

func(t *OtherMonsterMgr) GetAll() []*OtherMonster {
    return t.all
}

func(t *OtherMonsterMgr) GetByid(id int32) (*OtherMonster,bool) {
    v, ok := t.idMap[id]
    return v, ok
}



func (t *OtherMonsterMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*AiAi, 0, cnt)
    for i := 0; i < int(cnt); i++ {
        v := &AiAi{}
        v := createOtherMonster(stream)
        break
    }
}


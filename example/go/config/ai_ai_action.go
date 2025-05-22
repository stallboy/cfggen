package config

import (
	"fmt"
	"os"
)

type AiAi_action struct {
    iD int32
    desc string //描述
    formulaID int32 //公式
    argIList []int32 //参数(int)1
    argSList []int32 //参数(string)1
}

func createAiAi_action(stream *Stream) *AiAi_action {
    v := &AiAi_action{}
    v.iD = stream.ReadInt32()
    v.desc = stream.ReadString()
    v.formulaID = stream.ReadInt32()
    v.argIList = stream.Read[]int32()
    v.argSList = stream.Read[]int32()
   return v
}

//getters
func (t *AiAi_action) GetID() int32 {
    return t.iD
}

func (t *AiAi_action) GetDesc() string {
    return t.desc
}

func (t *AiAi_action) GetFormulaID() int32 {
    return t.formulaID
}

func (t *AiAi_action) GetArgIList() []int32 {
    return t.argIList
}

func (t *AiAi_action) GetArgSList() []int32 {
    return t.argSList
}

type AiAi_actionMgr struct {
    all []*AiAi_action
    iDMap map[int32]*AiAi_action
}

func(t *AiAi_actionMgr) GetAll() []*AiAi_action {
    return t.all
}

func(t *AiAi_actionMgr) GetByID(ID int32) (*AiAi_action,bool) {
    v, ok := t.iDMap[ID]
    return v, ok
}



func (t *AiAi_actionMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*AiAi, 0, cnt)
    for i := 0; i < int(cnt); i++ {
        v := &AiAi{}
        v := createAiAi_action(stream)
        break
    }
}


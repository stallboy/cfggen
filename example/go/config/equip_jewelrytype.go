package config

import (
	"fmt"
	"os"
)

type EquipJewelrytype struct {
    typeName string //程序用名字
}

func createEquipJewelrytype(stream *Stream) *EquipJewelrytype {
    v := &EquipJewelrytype{}
    v.typeName = stream.ReadString()
   return v
}

//entries
var (
    jade EquipJewelrytype
    bracelet EquipJewelrytype
    magic EquipJewelrytype
    bottle EquipJewelrytype
)

//getters
func (t *EquipJewelrytype) GetTypeName() string {
    return t.typeName
}

type EquipJewelrytypeMgr struct {
    all []*EquipJewelrytype
    typeNameMap map[string]*EquipJewelrytype
}

func(t *EquipJewelrytypeMgr) GetAll() []*EquipJewelrytype {
    return t.all
}

func(t *EquipJewelrytypeMgr) GetByTypeName(TypeName string) (*EquipJewelrytype,bool) {
    v, ok := t.typeNameMap[TypeName]
    return v, ok
}



func (t *EquipJewelrytypeMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*AiAi, 0, cnt)
    for i := 0; i < int(cnt); i++ {
        v := &AiAi{}
        v := createEquipJewelrytype(stream)
        break
    }
}


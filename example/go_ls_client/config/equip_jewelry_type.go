package config

import "fmt"

type EquipJewelry_type struct {
    typeName string //程序用名字
}

func createEquipJewelry_type(stream *Stream) *EquipJewelry_type {
    v := &EquipJewelry_type{}
    v.typeName = stream.ReadStringInPool()
    return v
}

func (t *EquipJewelry_type) String() string {
    return fmt.Sprintf("EquipJewelry_type{typeName=%v}", t.typeName)
}

//entries
var (
    jade EquipJewelry_type
    bracelet EquipJewelry_type
    magic EquipJewelry_type
    bottle EquipJewelry_type
)

//getters
func (t *EquipJewelry_type) TypeName() string {
    return t.typeName
}

func (t *EquipJewelry_typeMgr) GetJade() *EquipJewelry_type {
	return &jade
}

func (t *EquipJewelry_typeMgr) GetBracelet() *EquipJewelry_type {
	return &bracelet
}

func (t *EquipJewelry_typeMgr) GetMagic() *EquipJewelry_type {
	return &magic
}

func (t *EquipJewelry_typeMgr) GetBottle() *EquipJewelry_type {
	return &bottle
}

type EquipJewelry_typeMgr struct {
    all []*EquipJewelry_type
    typeNameMap map[string]*EquipJewelry_type
}

func(t *EquipJewelry_typeMgr) GetAll() []*EquipJewelry_type {
    return t.all
}

func(t *EquipJewelry_typeMgr) Get(typeName string) *EquipJewelry_type {
    return t.typeNameMap[typeName]
}

func (t *EquipJewelry_typeMgr) Init(stream *Stream) {
    cnt := int32(stream.ReadCount(1))
    t.all = make([]*EquipJewelry_type, 0, cnt)
    t.typeNameMap = make(map[string]*EquipJewelry_type, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createEquipJewelry_type(stream)
        t.all = append(t.all, v)
        t.typeNameMap[v.typeName] = v
        switch v.typeName {
        case "Jade":
            jade = *v
        case "Bracelet":
            bracelet = *v
        case "Magic":
            magic = *v
        case "Bottle":
            bottle = *v
        }
    }
}

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

//getters
func (t *EquipJewelry_type) TypeName() string {
    return t.typeName
}

func (t *EquipJewelry_typeMgr) GetJade() *EquipJewelry_type {
    return t.symbols["Jade"]
}

func (t *EquipJewelry_typeMgr) GetBracelet() *EquipJewelry_type {
    return t.symbols["Bracelet"]
}

func (t *EquipJewelry_typeMgr) GetMagic() *EquipJewelry_type {
    return t.symbols["Magic"]
}

func (t *EquipJewelry_typeMgr) GetBottle() *EquipJewelry_type {
    return t.symbols["Bottle"]
}

type EquipJewelry_typeMgr struct {
    all []*EquipJewelry_type
    symbols map[string]*EquipJewelry_type
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
    t.symbols = make(map[string]*EquipJewelry_type)
    t.typeNameMap = make(map[string]*EquipJewelry_type, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createEquipJewelry_type(stream)
        t.all = append(t.all, v)
        t.typeNameMap[v.typeName] = v
        switch v.typeName {
        case "Jade":
            t.symbols["Jade"] = v
        case "Bracelet":
            t.symbols["Bracelet"] = v
        case "Magic":
            t.symbols["Magic"] = v
        case "Bottle":
            t.symbols["Bottle"] = v
        }
    }
}

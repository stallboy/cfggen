package config

type EquipJewelrytype struct {
    typeName string //程序用名字
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
    allMap map[string]*EquipJewelrytype
}

func(t *EquipJewelrytypeMgr) GetAll() []*EquipJewelrytype {
    return t.all
}

func(t *EquipJewelrytypeMgr) Get(key string) (*EquipJewelrytype,bool) {
    v, ok := t.allMap[key]
    return v, ok
}


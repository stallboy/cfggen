package config

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

func (t *EquipJewelrytypeMgr) GetJade() *EquipJewelrytype {
	return &jade
}

func (t *EquipJewelrytypeMgr) GetBracelet() *EquipJewelrytype {
	return &bracelet
}

func (t *EquipJewelrytypeMgr) GetMagic() *EquipJewelrytype {
	return &magic
}

func (t *EquipJewelrytypeMgr) GetBottle() *EquipJewelrytype {
	return &bottle
}

type EquipJewelrytypeMgr struct {
    all []*EquipJewelrytype
    typeNameMap map[string]*EquipJewelrytype
}

func(t *EquipJewelrytypeMgr) GetAll() []*EquipJewelrytype {
    return t.all
}

func(t *EquipJewelrytypeMgr) Get(typeName string) *EquipJewelrytype {
    return t.typeNameMap[typeName]
}


func (t *EquipJewelrytypeMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*EquipJewelrytype, 0, cnt)
    t.typeNameMap = make(map[string]*EquipJewelrytype, cnt)

    for i := 0; i < int(cnt); i++ {
        v := createEquipJewelrytype(stream)
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

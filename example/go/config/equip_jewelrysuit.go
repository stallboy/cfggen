package config

type EquipJewelrysuit struct {
    suitID int32 //饰品套装ID
    ename string
    name string //策划用名字
    ability1 int32 //套装属性类型1（装备套装中的两件时增加的属性）
    ability1Value int32 //套装属性1
    ability2 int32 //套装属性类型2（装备套装中的三件时增加的属性）
    ability2Value int32 //套装属性2
    ability3 int32 //套装属性类型3（装备套装中的四件时增加的属性）
    ability3Value int32 //套装属性3
    suitList []int32 //部件1
}

func createEquipJewelrysuit(stream *Stream) *EquipJewelrysuit {
    v := &EquipJewelrysuit{}
    v.suitID = stream.ReadInt32()
    v.ename = stream.ReadString()
    v.name = stream.ReadString()
    v.ability1 = stream.ReadInt32()
    v.ability1Value = stream.ReadInt32()
    v.ability2 = stream.ReadInt32()
    v.ability2Value = stream.ReadInt32()
    v.ability3 = stream.ReadInt32()
    v.ability3Value = stream.ReadInt32()
    suitListSize := stream.ReadInt32()
    v.suitList = make([]int32, suitListSize)
    for i := 0; i < int(suitListSize); i++ {
        v.suitList[i] = stream.ReadInt32()
    }
    return v
}

//entries
var (
    specialSuit EquipJewelrysuit
)

//getters
func (t *EquipJewelrysuit) GetSuitID() int32 {
    return t.suitID
}

func (t *EquipJewelrysuit) GetEname() string {
    return t.ename
}

func (t *EquipJewelrysuit) GetName() string {
    return t.name
}

func (t *EquipJewelrysuit) GetAbility1() int32 {
    return t.ability1
}

func (t *EquipJewelrysuit) GetAbility1Value() int32 {
    return t.ability1Value
}

func (t *EquipJewelrysuit) GetAbility2() int32 {
    return t.ability2
}

func (t *EquipJewelrysuit) GetAbility2Value() int32 {
    return t.ability2Value
}

func (t *EquipJewelrysuit) GetAbility3() int32 {
    return t.ability3
}

func (t *EquipJewelrysuit) GetAbility3Value() int32 {
    return t.ability3Value
}

func (t *EquipJewelrysuit) GetSuitList() []int32 {
    return t.suitList
}

func (t *EquipJewelrysuitMgr) GetSpecialSuit() *EquipJewelrysuit {
	return &specialSuit
}

type EquipJewelrysuitMgr struct {
    all []*EquipJewelrysuit
    suitIDMap map[int32]*EquipJewelrysuit
}

func(t *EquipJewelrysuitMgr) GetAll() []*EquipJewelrysuit {
    return t.all
}

func(t *EquipJewelrysuitMgr) Get(suitID int32) *EquipJewelrysuit {
    return t.suitIDMap[suitID]
}


func (t *EquipJewelrysuitMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*EquipJewelrysuit, 0, cnt)
    t.suitIDMap = make(map[int32]*EquipJewelrysuit, cnt)

    for i := 0; i < int(cnt); i++ {
        v := createEquipJewelrysuit(stream)
        t.all = append(t.all, v)
        t.suitIDMap[v.suitID] = v

        switch v.ename {
        case "SpecialSuit":
            specialSuit = *v
        }
    }
}

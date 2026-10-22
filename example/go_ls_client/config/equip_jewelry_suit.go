package config

import "fmt"

type EquipJewelry_suit struct {
    suitID int32 //饰品套装ID
    ename string
    name *Text //策划用名字
    ability1 int32 //套装属性类型1（装备套装中的两件时增加的属性）
    ability1Value int32 //套装属性1
    ability2 int32 //套装属性类型2（装备套装中的三件时增加的属性）
    ability2Value int32 //套装属性2
    ability3 int32 //套装属性类型3（装备套装中的四件时增加的属性）
    ability3Value int32 //套装属性3
    suitList []int32 //部件1
}

func createEquipJewelry_suit(stream *Stream) *EquipJewelry_suit {
    v := &EquipJewelry_suit{}
    v.suitID = stream.ReadInt32()
    v.ename = stream.ReadStringInPool()
    v.name = createText(stream)
    v.ability1 = stream.ReadInt32()
    v.ability1Value = stream.ReadInt32()
    v.ability2 = stream.ReadInt32()
    v.ability2Value = stream.ReadInt32()
    v.ability3 = stream.ReadInt32()
    v.ability3Value = stream.ReadInt32()
    suitListSize := stream.ReadCount(0)
    v.suitList = make([]int32, suitListSize)
    for i := 0; i < suitListSize; i++ {
        v.suitList[i] = stream.ReadInt32()
    }
    return v
}

func (t *EquipJewelry_suit) String() string {
    return fmt.Sprintf("EquipJewelry_suit{suitID=%v, ename=%v, name=%v, ability1=%v, ability1Value=%v, ability2=%v, ability2Value=%v, ability3=%v, ability3Value=%v, suitList=%v}", t.suitID, t.ename, t.name, t.ability1, t.ability1Value, t.ability2, t.ability2Value, t.ability3, t.ability3Value, t.suitList)
}

//getters
func (t *EquipJewelry_suit) SuitID() int32 {
    return t.suitID
}

func (t *EquipJewelry_suit) Ename() string {
    return t.ename
}

func (t *EquipJewelry_suit) Name() *Text {
    return t.name
}

func (t *EquipJewelry_suit) Ability1() int32 {
    return t.ability1
}

func (t *EquipJewelry_suit) Ability1Value() int32 {
    return t.ability1Value
}

func (t *EquipJewelry_suit) Ability2() int32 {
    return t.ability2
}

func (t *EquipJewelry_suit) Ability2Value() int32 {
    return t.ability2Value
}

func (t *EquipJewelry_suit) Ability3() int32 {
    return t.ability3
}

func (t *EquipJewelry_suit) Ability3Value() int32 {
    return t.ability3Value
}

func (t *EquipJewelry_suit) SuitList() []int32 {
    return t.suitList
}

func (t *EquipJewelry_suitMgr) GetSpecialSuit() *EquipJewelry_suit {
    return t.symbols["SpecialSuit"]
}

type EquipJewelry_suitMgr struct {
    all []*EquipJewelry_suit
    symbols map[string]*EquipJewelry_suit
    suitIDMap map[int32]*EquipJewelry_suit
}

func(t *EquipJewelry_suitMgr) GetAll() []*EquipJewelry_suit {
    return t.all
}

func(t *EquipJewelry_suitMgr) Get(suitID int32) *EquipJewelry_suit {
    return t.suitIDMap[suitID]
}

func (t *EquipJewelry_suitMgr) Init(stream *Stream) {
    cnt := int32(stream.ReadCount(1))
    t.all = make([]*EquipJewelry_suit, 0, cnt)
    t.symbols = make(map[string]*EquipJewelry_suit)
    t.suitIDMap = make(map[int32]*EquipJewelry_suit, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createEquipJewelry_suit(stream)
        t.all = append(t.all, v)
        t.suitIDMap[v.suitID] = v
        switch v.ename {
        case "SpecialSuit":
            t.symbols["SpecialSuit"] = v
        }
    }
}

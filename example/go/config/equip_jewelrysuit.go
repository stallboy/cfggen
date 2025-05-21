package config

type EquipJewelrysuit struct {
    suitID int //饰品套装ID
    ename string
    name string //策划用名字
    ability1 int //套装属性类型1（装备套装中的两件时增加的属性）
    ability1Value int //套装属性1
    ability2 int //套装属性类型2（装备套装中的三件时增加的属性）
    ability2Value int //套装属性2
    ability3 int //套装属性类型3（装备套装中的四件时增加的属性）
    ability3Value int //套装属性3
    suitList []int //部件1
}

//entries
var (
    specialSuit EquipJewelrysuit
)

//getters
func (t *EquipJewelrysuit) GetSuitID() int {
    return t.suitID
}

func (t *EquipJewelrysuit) GetEname() string {
    return t.ename
}

func (t *EquipJewelrysuit) GetName() string {
    return t.name
}

func (t *EquipJewelrysuit) GetAbility1() int {
    return t.ability1
}

func (t *EquipJewelrysuit) GetAbility1Value() int {
    return t.ability1Value
}

func (t *EquipJewelrysuit) GetAbility2() int {
    return t.ability2
}

func (t *EquipJewelrysuit) GetAbility2Value() int {
    return t.ability2Value
}

func (t *EquipJewelrysuit) GetAbility3() int {
    return t.ability3
}

func (t *EquipJewelrysuit) GetAbility3Value() int {
    return t.ability3Value
}

func (t *EquipJewelrysuit) GetSuitList() []int {
    return t.suitList
}

type EquipJewelrysuitMgr struct {
    all []*EquipJewelrysuit
    allMap map[int]*EquipJewelrysuit
}

func(t *EquipJewelrysuitMgr) GetAll() []*EquipJewelrysuit {
    return t.all
}

func(t *EquipJewelrysuitMgr) Get(key int) (*EquipJewelrysuit,bool) {
    v, ok := t.allMap[key]
    return v, ok
}


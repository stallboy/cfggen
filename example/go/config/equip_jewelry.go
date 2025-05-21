package config

type EquipJewelry struct {
    iD int //首饰ID
    name string //首饰名称
    iconFile string //图标ID
    lvlRank LevelRank //首饰等级
    jType string //首饰类型
    suitID int //套装ID（为0是没有不属于套装，首饰品级为4的首饰该参数为套装id，其余情况为0,引用JewelrySuit.csv）
    keyAbility int //关键属性类型
    keyAbilityValue int //关键属性数值
    salePrice int //售卖价格
    description string //描述,根据Lvl和Rank来随机3个属性，第一个属性由Lvl,Rank行随机，剩下2个由Lvl和小于Rank的行里随机。Rank最小的时候都从Lvl，Rank里随机。
    refLvlRank *EquipJewelryrandom
    refJType *EquipJewelrytype
    nullableRefSuitID *EquipJewelrysuit
    refKeyAbility *EquipAbility
}

//getters
func (t *EquipJewelry) GetID() int {
    return t.iD
}

func (t *EquipJewelry) GetName() string {
    return t.name
}

func (t *EquipJewelry) GetIconFile() string {
    return t.iconFile
}

func (t *EquipJewelry) GetLvlRank() LevelRank {
    return t.lvlRank
}

func (t *EquipJewelry) GetJType() string {
    return t.jType
}

func (t *EquipJewelry) GetSuitID() int {
    return t.suitID
}

func (t *EquipJewelry) GetKeyAbility() int {
    return t.keyAbility
}

func (t *EquipJewelry) GetKeyAbilityValue() int {
    return t.keyAbilityValue
}

func (t *EquipJewelry) GetSalePrice() int {
    return t.salePrice
}

func (t *EquipJewelry) GetDescription() string {
    return t.description
}

//ref properties
func (t *EquipJewelry) GetRefLvlRank() *EquipJewelryrandom {
    return t.refLvlRank
}
func (t *EquipJewelry) GetRefJType() *EquipJewelrytype {
    return t.refJType
}
func (t *EquipJewelry) GetNullableRefSuitID() *EquipJewelrysuit {
    return t.nullableRefSuitID
}
func (t *EquipJewelry) GetRefKeyAbility() *EquipAbility {
    return t.refKeyAbility
}

type EquipJewelryMgr struct {
    all []*EquipJewelry
    iDMap map[int]*EquipJewelry
}

func(t *EquipJewelryMgr) GetAll() []*EquipJewelry {
    return t.all
}

func(t *EquipJewelryMgr) GetByID(ID int) (*EquipJewelry,bool) {
    v, ok := t.iDMap[ID]
    return v, ok
}




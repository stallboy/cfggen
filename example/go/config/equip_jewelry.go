package config

type EquipJewelry struct {
    iD int32 //首饰ID
    name string //首饰名称
    iconFile string //图标ID
    lvlRank *LevelRank //首饰等级
    jType string //首饰类型
    suitID int32 //套装ID（为0是没有不属于套装，首饰品级为4的首饰该参数为套装id，其余情况为0,引用JewelrySuit.csv）
    keyAbility int32 //关键属性类型
    keyAbilityValue int32 //关键属性数值
    salePrice int32 //售卖价格
    description string //描述,根据Lvl和Rank来随机3个属性，第一个属性由Lvl,Rank行随机，剩下2个由Lvl和小于Rank的行里随机。Rank最小的时候都从Lvl，Rank里随机。
    refLvlRank *EquipJewelryrandom
    refJType *EquipJewelrytype
    nullableRefSuitID *EquipJewelrysuit
    refKeyAbility *EquipAbility
}

func createEquipJewelry(stream *Stream) *EquipJewelry {
    v := &EquipJewelry{}
    v.iD = stream.ReadInt32()
    v.name = stream.ReadString()
    v.iconFile = stream.ReadString()
    v.lvlRank = createLevelRank(stream)
    v.jType = stream.ReadString()
    v.suitID = stream.ReadInt32()
    v.keyAbility = stream.ReadInt32()
    v.keyAbilityValue = stream.ReadInt32()
    v.salePrice = stream.ReadInt32()
    v.description = stream.ReadString()
    return v
}

//getters
func (t *EquipJewelry) GetID() int32 {
    return t.iD
}

func (t *EquipJewelry) GetName() string {
    return t.name
}

func (t *EquipJewelry) GetIconFile() string {
    return t.iconFile
}

func (t *EquipJewelry) GetLvlRank() *LevelRank {
    return t.lvlRank
}

func (t *EquipJewelry) GetJType() string {
    return t.jType
}

func (t *EquipJewelry) GetSuitID() int32 {
    return t.suitID
}

func (t *EquipJewelry) GetKeyAbility() int32 {
    return t.keyAbility
}

func (t *EquipJewelry) GetKeyAbilityValue() int32 {
    return t.keyAbilityValue
}

func (t *EquipJewelry) GetSalePrice() int32 {
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
    iDMap map[int32]*EquipJewelry
}

func(t *EquipJewelryMgr) GetAll() []*EquipJewelry {
    return t.all
}

func(t *EquipJewelryMgr) GetByiD(iD int32) *EquipJewelry {
    return t.iDMap[iD]
}



func (t *EquipJewelryMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*EquipJewelry, 0, cnt)
    t.iDMap = make(map[int32]*EquipJewelry, cnt)

    for i := 0; i < int(cnt); i++ {
        v := createEquipJewelry(stream)
        t.all = append(t.all, v)
        t.iDMap[v.iD] = v
    }
}


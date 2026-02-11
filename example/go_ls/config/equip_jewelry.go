package config

import "fmt"

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
    v.name = stream.ReadStringInPool()
    v.iconFile = stream.ReadStringInPool()
    v.lvlRank = createLevelRank(stream)
    v.jType = stream.ReadStringInPool()
    v.suitID = stream.ReadInt32()
    v.keyAbility = stream.ReadInt32()
    v.keyAbilityValue = stream.ReadInt32()
    v.salePrice = stream.ReadInt32()
    v.description = stream.ReadStringInPool()
    return v
}

func (t *EquipJewelry) String() string {
    return fmt.Sprintf("EquipJewelry{iD=%v, name=%v, iconFile=%v, lvlRank=%v, jType=%v, suitID=%v, keyAbility=%v, keyAbilityValue=%v, salePrice=%v, description=%v}", t.iD, t.name, t.iconFile, fmt.Sprintf("%v", t.lvlRank), t.jType, t.suitID, t.keyAbility, t.keyAbilityValue, t.salePrice, t.description)
}

//getters
func (t *EquipJewelry) ID() int32 {
    return t.iD
}

func (t *EquipJewelry) Name() string {
    return t.name
}

func (t *EquipJewelry) IconFile() string {
    return t.iconFile
}

func (t *EquipJewelry) LvlRank() *LevelRank {
    return t.lvlRank
}

func (t *EquipJewelry) JType() string {
    return t.jType
}

func (t *EquipJewelry) SuitID() int32 {
    return t.suitID
}

func (t *EquipJewelry) KeyAbility() int32 {
    return t.keyAbility
}

func (t *EquipJewelry) KeyAbilityValue() int32 {
    return t.keyAbilityValue
}

func (t *EquipJewelry) SalePrice() int32 {
    return t.salePrice
}

func (t *EquipJewelry) Description() string {
    return t.description
}

func (t *EquipJewelry) RefLvlRank() *EquipJewelryrandom {
    if t.refLvlRank == nil {
        t.refLvlRank = GetEquipJewelryrandomMgr().Get(t.lvlRank)
    }
    return t.refLvlRank
}

func (t *EquipJewelry) RefJType() *EquipJewelrytype {
    if t.refJType == nil {
        t.refJType = GetEquipJewelrytypeMgr().Get(t.jType)
    }
    return t.refJType
}

func (t *EquipJewelry) NullableRefSuitID() *EquipJewelrysuit {
    if t.nullableRefSuitID == nil {
        t.nullableRefSuitID = GetEquipJewelrysuitMgr().Get(t.suitID)
    }
    return t.nullableRefSuitID
}

func (t *EquipJewelry) RefKeyAbility() *EquipAbility {
    if t.refKeyAbility == nil {
        t.refKeyAbility = GetEquipAbilityMgr().Get(t.keyAbility)
    }
    return t.refKeyAbility
}

type EquipJewelryMgr struct {
    all []*EquipJewelry
    iDMap map[int32]*EquipJewelry
}

func(t *EquipJewelryMgr) GetAll() []*EquipJewelry {
    return t.all
}

func(t *EquipJewelryMgr) Get(iD int32) *EquipJewelry {
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

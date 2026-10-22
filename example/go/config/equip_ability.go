package config

import "fmt"

type EquipAbility struct {
    id int32 //属性类型
    name string //程序用名字
}

func createEquipAbility(stream *Stream) *EquipAbility {
    v := &EquipAbility{}
    v.id = stream.ReadInt32()
    v.name = stream.ReadStringInPool()
    return v
}

func (t *EquipAbility) String() string {
    return fmt.Sprintf("EquipAbility{id=%v, name=%v}", t.id, t.name)
}

//getters
func (t *EquipAbility) Id() int32 {
    return t.id
}

func (t *EquipAbility) Name() string {
    return t.name
}

func (t *EquipAbilityMgr) GetAttack() *EquipAbility {
    return t.symbols["attack"]
}

func (t *EquipAbilityMgr) GetDefence() *EquipAbility {
    return t.symbols["defence"]
}

func (t *EquipAbilityMgr) GetHp() *EquipAbility {
    return t.symbols["hp"]
}

func (t *EquipAbilityMgr) GetCritical() *EquipAbility {
    return t.symbols["critical"]
}

func (t *EquipAbilityMgr) GetCritical_resist() *EquipAbility {
    return t.symbols["critical_resist"]
}

func (t *EquipAbilityMgr) GetBlock() *EquipAbility {
    return t.symbols["block"]
}

func (t *EquipAbilityMgr) GetBreak_armor() *EquipAbility {
    return t.symbols["break_armor"]
}

type EquipAbilityMgr struct {
    all []*EquipAbility
    symbols map[string]*EquipAbility
    idMap map[int32]*EquipAbility
}

func(t *EquipAbilityMgr) GetAll() []*EquipAbility {
    return t.all
}

func(t *EquipAbilityMgr) Get(id int32) *EquipAbility {
    return t.idMap[id]
}

func (t *EquipAbilityMgr) Init(stream *Stream) {
    cnt := int32(stream.ReadCount(1))
    t.all = make([]*EquipAbility, 0, cnt)
    t.symbols = make(map[string]*EquipAbility)
    t.idMap = make(map[int32]*EquipAbility, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createEquipAbility(stream)
        t.all = append(t.all, v)
        t.idMap[v.id] = v
        switch v.name {
        case "Attack":
            t.symbols["attack"] = v
        case "Defence":
            t.symbols["defence"] = v
        case "Hp":
            t.symbols["hp"] = v
        case "Critical":
            t.symbols["critical"] = v
        case "Critical_resist":
            t.symbols["critical_resist"] = v
        case "Block":
            t.symbols["block"] = v
        case "Break_armor":
            t.symbols["break_armor"] = v
        }
    }
}

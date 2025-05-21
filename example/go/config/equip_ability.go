package config

type EquipAbility struct {
    id int //属性类型
    name string //程序用名字
}

//entries
var (
    attack EquipAbility
    defence EquipAbility
    hp EquipAbility
    critical EquipAbility
    critical_resist EquipAbility
    block EquipAbility
    break_armor EquipAbility
)

//getters
func (t *EquipAbility) GetId() int {
    return t.id
}

func (t *EquipAbility) GetName() string {
    return t.name
}

type EquipAbilityMgr struct {
    all []*EquipAbility
    allMap map[int]*EquipAbility
}

func(t *EquipAbilityMgr) GetAll() []*EquipAbility {
    return t.all
}

func(t *EquipAbilityMgr) Get(key int) (*EquipAbility,bool) {
    v, ok := t.allMap[key]
    return v, ok
}


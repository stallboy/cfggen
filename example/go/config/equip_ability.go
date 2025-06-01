package config

type EquipAbility struct {
    id int32 //属性类型
    name string //程序用名字
}

func createEquipAbility(stream *Stream) *EquipAbility {
    v := &EquipAbility{}
    v.id = stream.ReadInt32()
    v.name = stream.ReadString()
    return v
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
func (t *EquipAbility) GetId() int32 {
    return t.id
}

func (t *EquipAbility) GetName() string {
    return t.name
}

func (t *EquipAbilityMgr) GetAttack() *EquipAbility {
	return &attack
}

func (t *EquipAbilityMgr) GetDefence() *EquipAbility {
	return &defence
}

func (t *EquipAbilityMgr) GetHp() *EquipAbility {
	return &hp
}

func (t *EquipAbilityMgr) GetCritical() *EquipAbility {
	return &critical
}

func (t *EquipAbilityMgr) GetCritical_resist() *EquipAbility {
	return &critical_resist
}

func (t *EquipAbilityMgr) GetBlock() *EquipAbility {
	return &block
}

func (t *EquipAbilityMgr) GetBreak_armor() *EquipAbility {
	return &break_armor
}

type EquipAbilityMgr struct {
    all []*EquipAbility
    idMap map[int32]*EquipAbility
}

func(t *EquipAbilityMgr) GetAll() []*EquipAbility {
    return t.all
}

func(t *EquipAbilityMgr) Get(id int32) *EquipAbility {
    return t.idMap[id]
}


func (t *EquipAbilityMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*EquipAbility, 0, cnt)
    t.idMap = make(map[int32]*EquipAbility, cnt)

    for i := 0; i < int(cnt); i++ {
        v := createEquipAbility(stream)
        t.all = append(t.all, v)
        t.idMap[v.id] = v

        switch v.name {
        case "Attack":
            attack = *v
        case "Defence":
            defence = *v
        case "Hp":
            hp = *v
        case "Critical":
            critical = *v
        case "Critical_resist":
            critical_resist = *v
        case "Block":
            block = *v
        case "Break_armor":
            break_armor = *v

        }
    }
}

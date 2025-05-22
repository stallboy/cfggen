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

type EquipAbilityMgr struct {
    all []*EquipAbility
    idMap map[int32]*EquipAbility
}

func(t *EquipAbilityMgr) GetAll() []*EquipAbility {
    return t.all
}

func(t *EquipAbilityMgr) GetByid(id int32) (*EquipAbility,bool) {
    v, ok := t.idMap[id]
    return v, ok
}



func (t *EquipAbilityMgr) Init(stream *Stream) {
    cnt := stream.ReadInt32()
    t.all = make([]*EquipAbility, 0, cnt)
    for i := 0; i < int(cnt); i++ {
        v := createEquipAbility(stream)
        t.all = append(t.all, v)
    }
}


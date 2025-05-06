package equip

type Jewelrysuit struct {
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
    specialSuit Jewelrysuit
)

//getters
func (t *Jewelrysuit) GetSuitID() int {
    return t.suitID
}

func (t *Jewelrysuit) GetEname() string {
    return t.ename
}

func (t *Jewelrysuit) GetName() string {
    return t.name
}

func (t *Jewelrysuit) GetAbility1() int {
    return t.ability1
}

func (t *Jewelrysuit) GetAbility1Value() int {
    return t.ability1Value
}

func (t *Jewelrysuit) GetAbility2() int {
    return t.ability2
}

func (t *Jewelrysuit) GetAbility2Value() int {
    return t.ability2Value
}

func (t *Jewelrysuit) GetAbility3() int {
    return t.ability3
}

func (t *Jewelrysuit) GetAbility3Value() int {
    return t.ability3Value
}

func (t *Jewelrysuit) GetSuitList() []int {
    return t.suitList
}

var all []Jewelrysuit
func GetAll() []Jewelrysuit {
    return all[:len(all)]:len(all)]
}

var allMap map[int]Jewelrysuit
func Get(key int) (Jewelrysuit,bool){
    return allMap[key]
}
        public static Jewelrysuit Get(int suitID)
        {
            Jewelrysuit v;
            return all.TryGetValue(suitID, out v) ? v : null;
        }

        public static List<Jewelrysuit> All()
        {
            return all.OrderedValues;
        }

        public static List<Jewelrysuit> Filter(Predicate<Jewelrysuit> predicate)
        {
            var r = new List<Jewelrysuit>();
            foreach (var e in all.OrderedValues)
            {
                if (predicate(e))
                    r.Add(e);
            }
            return r;
        }

        internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
        {
            all = new Config.KeyedList<int, Jewelrysuit>();
            for (var c = os.ReadInt32(); c > 0; c--) {
                var self = _create(os);
                all.Add(self.SuitID, self);
                if (self.Ename.Trim().Length == 0)
                    continue;
                switch(self.Ename.Trim())
                {
                    case "SpecialSuit":
                        if (SpecialSuit != null)
                            errors.EnumDup("equip.jewelrysuit", self.ToString());
                        SpecialSuit = self;
                        break;
                    default:
                        errors.EnumDataAdd("equip.jewelrysuit", self.ToString());
                        break;
                }
            }
            if (SpecialSuit == null)
                errors.EnumNull("equip.jewelrysuit", "SpecialSuit");
        }


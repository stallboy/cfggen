package equip

type Ability struct {
    id int //属性类型
    name string //程序用名字
}

//entries
var (
    attack Ability
    defence Ability
    hp Ability
    critical Ability
    critical_resist Ability
    block Ability
    break_armor Ability
)

//getters
func (t *Ability) GetId() int {
    return t.id
}

func (t *Ability) GetName() string {
    return t.name
}

var all []Ability
func GetAll() []Ability {
    return all[:len(all)]:len(all)]
}

var allMap map[int]Ability
func Get(key int) (Ability,bool){
    return allMap[key]
}
        public static Ability Get(int id)
        {
            Ability v;
            return all.TryGetValue(id, out v) ? v : null;
        }

        public static List<Ability> All()
        {
            return all.OrderedValues;
        }

        public static List<Ability> Filter(Predicate<Ability> predicate)
        {
            var r = new List<Ability>();
            foreach (var e in all.OrderedValues)
            {
                if (predicate(e))
                    r.Add(e);
            }
            return r;
        }

        internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
        {
            all = new Config.KeyedList<int, Ability>();
            for (var c = os.ReadInt32(); c > 0; c--) {
                var self = _create(os);
                all.Add(self.Id, self);
                if (self.Name.Trim().Length == 0)
                    continue;
                switch(self.Name.Trim())
                {
                    case "attack":
                        if (Attack != null)
                            errors.EnumDup("equip.ability", self.ToString());
                        Attack = self;
                        break;
                    case "defence":
                        if (Defence != null)
                            errors.EnumDup("equip.ability", self.ToString());
                        Defence = self;
                        break;
                    case "hp":
                        if (Hp != null)
                            errors.EnumDup("equip.ability", self.ToString());
                        Hp = self;
                        break;
                    case "critical":
                        if (Critical != null)
                            errors.EnumDup("equip.ability", self.ToString());
                        Critical = self;
                        break;
                    case "critical_resist":
                        if (Critical_resist != null)
                            errors.EnumDup("equip.ability", self.ToString());
                        Critical_resist = self;
                        break;
                    case "block":
                        if (Block != null)
                            errors.EnumDup("equip.ability", self.ToString());
                        Block = self;
                        break;
                    case "break_armor":
                        if (Break_armor != null)
                            errors.EnumDup("equip.ability", self.ToString());
                        Break_armor = self;
                        break;
                    default:
                        errors.EnumDataAdd("equip.ability", self.ToString());
                        break;
                }
            }
            if (Attack == null)
                errors.EnumNull("equip.ability", "attack");
            if (Defence == null)
                errors.EnumNull("equip.ability", "defence");
            if (Hp == null)
                errors.EnumNull("equip.ability", "hp");
            if (Critical == null)
                errors.EnumNull("equip.ability", "critical");
            if (Critical_resist == null)
                errors.EnumNull("equip.ability", "critical_resist");
            if (Block == null)
                errors.EnumNull("equip.ability", "block");
            if (Break_armor == null)
                errors.EnumNull("equip.ability", "break_armor");
        }


package other

type Loot struct {
    lootid int //序号
    ename string
    name string //名字
    chanceList []int //掉落0件物品的概率
    listRefLootid List<config.Other.Lootitem>
}

//getters
func (t *Loot) GetLootid() int {
    return t.lootid
}

func (t *Loot) GetEname() string {
    return t.ename
}

func (t *Loot) GetName() string {
    return t.name
}

func (t *Loot) GetChanceList() []int {
    return t.chanceList
}

//ref properties
func (t *other.loot) GetListRefLootid() List<config.Other.Lootitem> {
    return t.listRefLootid
}

var all []Loot
func GetAll() []Loot {
    return all[:len(all)]:len(all)]
}

var allMap map[int]Loot
func Get(key int) (Loot,bool){
    return allMap[key]
}
        public static Loot Get(int lootid)
        {
            Loot v;
            return all.TryGetValue(lootid, out v) ? v : null;
        }

        public static List<Loot> All()
        {
            return all.OrderedValues;
        }

        public static List<Loot> Filter(Predicate<Loot> predicate)
        {
            var r = new List<Loot>();
            foreach (var e in all.OrderedValues)
            {
                if (predicate(e))
                    r.Add(e);
            }
            return r;
        }

        internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
        {
            all = new Config.KeyedList<int, Loot>();
            for (var c = os.ReadInt32(); c > 0; c--) {
                var self = _create(os);
                all.Add(self.Lootid, self);
            }
        }

        internal static void Resolve(Config.LoadErrors errors) {
            foreach (var v in All())
                v._resolve(errors);
        }


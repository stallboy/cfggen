package other

type Lootitem struct {
    lootid int //掉落id
    itemid int //掉落物品
    chance int //掉落概率
    countmin int //数量下限
    countmax int //数量上限
}

//getters
func (t *Lootitem) GetLootid() int {
    return t.lootid
}

func (t *Lootitem) GetItemid() int {
    return t.itemid
}

func (t *Lootitem) GetChance() int {
    return t.chance
}

func (t *Lootitem) GetCountmin() int {
    return t.countmin
}

func (t *Lootitem) GetCountmax() int {
    return t.countmax
}

var all []Lootitem
func GetAll() []Lootitem {
    return all[:len(all)]:len(all)]
}

var allMap map[LootidItemidKey]Lootitem
func Get(key LootidItemidKey) (Lootitem,bool){
    return allMap[key]
}
        public static Lootitem Get(int lootid, int itemid)
        {
            Lootitem v;
            return all.TryGetValue(new LootidItemidKey(lootid, itemid), out v) ? v : null;
        }

        public static List<Lootitem> All()
        {
            return all.OrderedValues;
        }

        public static List<Lootitem> Filter(Predicate<Lootitem> predicate)
        {
            var r = new List<Lootitem>();
            foreach (var e in all.OrderedValues)
            {
                if (predicate(e))
                    r.Add(e);
            }
            return r;
        }

        internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
        {
            all = new Config.KeyedList<LootidItemidKey, Lootitem>();
            for (var c = os.ReadInt32(); c > 0; c--) {
                var self = _create(os);
                all.Add(new LootidItemidKey(self.Lootid, self.Itemid), self);
            }
        }


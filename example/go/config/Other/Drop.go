package other

type Drop struct {
    dropid int //序号
    name string //名字
    items []config.Other.DropItem //掉落概率
    testmap KeyedList<int, int> //测试map block
}

//getters
func (t *Drop) GetDropid() int {
    return t.dropid
}

func (t *Drop) GetName() string {
    return t.name
}

func (t *Drop) GetItems() []config.Other.DropItem {
    return t.items
}

func (t *Drop) GetTestmap() KeyedList<int, int> {
    return t.testmap
}

var all []Drop
func GetAll() []Drop {
    return all[:len(all)]:len(all)]
}

var allMap map[int]Drop
func Get(key int) (Drop,bool){
    return allMap[key]
}
        public static Drop Get(int dropid)
        {
            Drop v;
            return all.TryGetValue(dropid, out v) ? v : null;
        }

        public static List<Drop> All()
        {
            return all.OrderedValues;
        }

        public static List<Drop> Filter(Predicate<Drop> predicate)
        {
            var r = new List<Drop>();
            foreach (var e in all.OrderedValues)
            {
                if (predicate(e))
                    r.Add(e);
            }
            return r;
        }

        internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
        {
            all = new Config.KeyedList<int, Drop>();
            for (var c = os.ReadInt32(); c > 0; c--) {
                var self = _create(os);
                all.Add(self.Dropid, self);
            }
        }


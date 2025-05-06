package other

type Monster struct {
    id int
    posList []config.Position
}

//getters
func (t *Monster) GetId() int {
    return t.id
}

func (t *Monster) GetPosList() []config.Position {
    return t.posList
}

var all []Monster
func GetAll() []Monster {
    return all[:len(all)]:len(all)]
}

var allMap map[int]Monster
func Get(key int) (Monster,bool){
    return allMap[key]
}
        public static Monster Get(int id)
        {
            Monster v;
            return all.TryGetValue(id, out v) ? v : null;
        }

        public static List<Monster> All()
        {
            return all.OrderedValues;
        }

        public static List<Monster> Filter(Predicate<Monster> predicate)
        {
            var r = new List<Monster>();
            foreach (var e in all.OrderedValues)
            {
                if (predicate(e))
                    r.Add(e);
            }
            return r;
        }

        internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
        {
            all = new Config.KeyedList<int, Monster>();
            for (var c = os.ReadInt32(); c > 0; c--) {
                var self = _create(os);
                all.Add(self.Id, self);
            }
        }


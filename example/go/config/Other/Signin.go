package other

type Signin struct {
    id int //礼包ID
    item2countMap KeyedList<int, int> //普通奖励
    vipitem2vipcountMap KeyedList<int, int> //vip奖励
    viplevel int //领取vip奖励的最低等级
    iconFile string //礼包图标
    refVipitem2vipcountMap KeyedList<int, config.Other.Loot>
}

//getters
func (t *Signin) GetId() int {
    return t.id
}

func (t *Signin) GetItem2countMap() KeyedList<int, int> {
    return t.item2countMap
}

func (t *Signin) GetVipitem2vipcountMap() KeyedList<int, int> {
    return t.vipitem2vipcountMap
}

func (t *Signin) GetViplevel() int {
    return t.viplevel
}

func (t *Signin) GetIconFile() string {
    return t.iconFile
}

//ref properties
func (t *other.signin) GetRefVipitem2vipcountMap() KeyedList<int, config.Other.Loot> {
    return t.refVipitem2vipcountMap
}

var all []Signin
func GetAll() []Signin {
    return all[:len(all)]:len(all)]
}

var allMap map[int]Signin
func Get(key int) (Signin,bool){
    return allMap[key]
}
        public static Signin Get(int id)
        {
            Signin v;
            return all.TryGetValue(id, out v) ? v : null;
        }

var all []Signin
func GetAll() []Signin {
    return all[:len(all)]:len(all)]
}

var allMap map[IdViplevelKey]Signin
func Get(key IdViplevelKey) (Signin,bool){
    return allMap[key]
}
        public static Signin GetByIdViplevel(int id, int viplevel)
        {
            Signin v;
            return idViplevelMap.TryGetValue(new IdViplevelKey(id, viplevel), out v) ? v : null;
        }

        public static List<Signin> All()
        {
            return all.OrderedValues;
        }

        public static List<Signin> Filter(Predicate<Signin> predicate)
        {
            var r = new List<Signin>();
            foreach (var e in all.OrderedValues)
            {
                if (predicate(e))
                    r.Add(e);
            }
            return r;
        }

        internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
        {
            all = new Config.KeyedList<int, Signin>();
            idViplevelMap = new Config.KeyedList<IdViplevelKey, Signin>();
            for (var c = os.ReadInt32(); c > 0; c--) {
                var self = _create(os);
                all.Add(self.Id, self);
                idViplevelMap.Add(new IdViplevelKey(self.Id, self.Viplevel), self);
            }
        }

        internal static void Resolve(Config.LoadErrors errors) {
            foreach (var v in All())
                v._resolve(errors);
        }


package equip

type Jewelryrandom struct {
    lvlRank config.LevelRank //等级
    attackRange config.Range //最小攻击力
    otherRange []config.Range //最小防御力
    testPack []config.Equip.TestPackBean //测试pack
}

//getters
func (t *Jewelryrandom) GetLvlRank() config.LevelRank {
    return t.lvlRank
}

func (t *Jewelryrandom) GetAttackRange() config.Range {
    return t.attackRange
}

func (t *Jewelryrandom) GetOtherRange() []config.Range {
    return t.otherRange
}

func (t *Jewelryrandom) GetTestPack() []config.Equip.TestPackBean {
    return t.testPack
}

var all []Jewelryrandom
func GetAll() []Jewelryrandom {
    return all[:len(all)]:len(all)]
}

var allMap map[config.LevelRank]Jewelryrandom
func Get(key config.LevelRank) (Jewelryrandom,bool){
    return allMap[key]
}
        public static Jewelryrandom Get(config.LevelRank lvlRank)
        {
            Jewelryrandom v;
            return all.TryGetValue(lvlRank, out v) ? v : null;
        }

        public static List<Jewelryrandom> All()
        {
            return all.OrderedValues;
        }

        public static List<Jewelryrandom> Filter(Predicate<Jewelryrandom> predicate)
        {
            var r = new List<Jewelryrandom>();
            foreach (var e in all.OrderedValues)
            {
                if (predicate(e))
                    r.Add(e);
            }
            return r;
        }

        internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
        {
            all = new Config.KeyedList<config.LevelRank, Jewelryrandom>();
            for (var c = os.ReadInt32(); c > 0; c--) {
                var self = _create(os);
                all.Add(self.LvlRank, self);
            }
        }

        internal static void Resolve(Config.LoadErrors errors) {
            foreach (var v in All())
                v._resolve(errors);
        }


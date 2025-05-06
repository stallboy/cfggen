package equip

type Jewelry struct {
    iD int //首饰ID
    name string //首饰名称
    iconFile string //图标ID
    lvlRank config.LevelRank //首饰等级
    type string //首饰类型
    suitID int //套装ID（为0是没有不属于套装，首饰品级为4的首饰该参数为套装id，其余情况为0,引用JewelrySuit.csv）
    keyAbility int //关键属性类型
    keyAbilityValue int //关键属性数值
    salePrice int //售卖价格
    description string //描述,根据Lvl和Rank来随机3个属性，第一个属性由Lvl,Rank行随机，剩下2个由Lvl和小于Rank的行里随机。Rank最小的时候都从Lvl，Rank里随机。
    refLvlRank config_equip.Jewelryrandom
    refType config_equip.Jewelrytype
    nullableRefSuitID config_equip.Jewelrysuit
    refKeyAbility config_equip.Ability
}

//getters
func (t *Jewelry) GetID() int {
    return t.iD
}

func (t *Jewelry) GetName() string {
    return t.name
}

func (t *Jewelry) GetIconFile() string {
    return t.iconFile
}

func (t *Jewelry) GetLvlRank() config.LevelRank {
    return t.lvlRank
}

func (t *Jewelry) GetType() string {
    return t.type
}

func (t *Jewelry) GetSuitID() int {
    return t.suitID
}

func (t *Jewelry) GetKeyAbility() int {
    return t.keyAbility
}

func (t *Jewelry) GetKeyAbilityValue() int {
    return t.keyAbilityValue
}

func (t *Jewelry) GetSalePrice() int {
    return t.salePrice
}

func (t *Jewelry) GetDescription() string {
    return t.description
}

//ref properties
func (t *equip.jewelry) GetRefLvlRank() config_equip.Jewelryrandom {
    return t.refLvlRank
}
func (t *equip.jewelry) GetRefType() config_equip.Jewelrytype {
    return t.refType
}
func (t *equip.jewelry) GetNullableRefSuitID() config_equip.Jewelrysuit {
    return t.nullableRefSuitID
}
func (t *equip.jewelry) GetRefKeyAbility() config_equip.Ability {
    return t.refKeyAbility
}

var all []Jewelry
func GetAll() []Jewelry {
    return all[:len(all)]:len(all)]
}

var allMap map[int]Jewelry
func Get(key int) (Jewelry,bool){
    return allMap[key]
}
        public static Jewelry Get(int iD)
        {
            Jewelry v;
            return all.TryGetValue(iD, out v) ? v : null;
        }

        public static List<Jewelry> All()
        {
            return all.OrderedValues;
        }

        public static List<Jewelry> Filter(Predicate<Jewelry> predicate)
        {
            var r = new List<Jewelry>();
            foreach (var e in all.OrderedValues)
            {
                if (predicate(e))
                    r.Add(e);
            }
            return r;
        }

        internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
        {
            all = new Config.KeyedList<int, Jewelry>();
            for (var c = os.ReadInt32(); c > 0; c--) {
                var self = _create(os);
                all.Add(self.ID, self);
            }
        }

        internal static void Resolve(Config.LoadErrors errors) {
            foreach (var v in All())
                v._resolve(errors);
        }


package equip

type Rank struct {
    rankID int //稀有度
    rankName string //程序用名字
    rankShowName string //显示名称
}

//entries
var (
    white Rank
    green Rank
    blue Rank
    purple Rank
    yellow Rank
)

//getters
func (t *Rank) GetRankID() int {
    return t.rankID
}

func (t *Rank) GetRankName() string {
    return t.rankName
}

func (t *Rank) GetRankShowName() string {
    return t.rankShowName
}

var all []Rank
func GetAll() []Rank {
    return all[:len(all)]:len(all)]
}

var allMap map[int]Rank
func Get(key int) (Rank,bool){
    return allMap[key]
}
        public static Rank Get(int rankID)
        {
            Rank v;
            return all.TryGetValue(rankID, out v) ? v : null;
        }

        public static List<Rank> All()
        {
            return all.OrderedValues;
        }

        public static List<Rank> Filter(Predicate<Rank> predicate)
        {
            var r = new List<Rank>();
            foreach (var e in all.OrderedValues)
            {
                if (predicate(e))
                    r.Add(e);
            }
            return r;
        }

        internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
        {
            all = new Config.KeyedList<int, Rank>();
            for (var c = os.ReadInt32(); c > 0; c--) {
                var self = _create(os);
                all.Add(self.RankID, self);
                if (self.RankName.Trim().Length == 0)
                    continue;
                switch(self.RankName.Trim())
                {
                    case "white":
                        if (White != null)
                            errors.EnumDup("equip.rank", self.ToString());
                        White = self;
                        break;
                    case "green":
                        if (Green != null)
                            errors.EnumDup("equip.rank", self.ToString());
                        Green = self;
                        break;
                    case "blue":
                        if (Blue != null)
                            errors.EnumDup("equip.rank", self.ToString());
                        Blue = self;
                        break;
                    case "purple":
                        if (Purple != null)
                            errors.EnumDup("equip.rank", self.ToString());
                        Purple = self;
                        break;
                    case "yellow":
                        if (Yellow != null)
                            errors.EnumDup("equip.rank", self.ToString());
                        Yellow = self;
                        break;
                    default:
                        errors.EnumDataAdd("equip.rank", self.ToString());
                        break;
                }
            }
            if (White == null)
                errors.EnumNull("equip.rank", "white");
            if (Green == null)
                errors.EnumNull("equip.rank", "green");
            if (Blue == null)
                errors.EnumNull("equip.rank", "blue");
            if (Purple == null)
                errors.EnumNull("equip.rank", "purple");
            if (Yellow == null)
                errors.EnumNull("equip.rank", "yellow");
        }


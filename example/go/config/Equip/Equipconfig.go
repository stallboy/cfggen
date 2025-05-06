package equip

type Equipconfig struct {
    entry string //入口，程序填
    stone_count_for_set int //形成套装的音石数量
    draw_protect_name string //保底策略名称
    broadcastid int //公告Id
    broadcast_least_quality int //公告的最低品质
    week_reward_mailid int //抽卡周奖励的邮件id
}

//entries
var (
    instance Equipconfig
    instance2 Equipconfig
)

//getters
func (t *Equipconfig) GetEntry() string {
    return t.entry
}

func (t *Equipconfig) GetStone_count_for_set() int {
    return t.stone_count_for_set
}

func (t *Equipconfig) GetDraw_protect_name() string {
    return t.draw_protect_name
}

func (t *Equipconfig) GetBroadcastid() int {
    return t.broadcastid
}

func (t *Equipconfig) GetBroadcast_least_quality() int {
    return t.broadcast_least_quality
}

func (t *Equipconfig) GetWeek_reward_mailid() int {
    return t.week_reward_mailid
}

var all []Equipconfig
func GetAll() []Equipconfig {
    return all[:len(all)]:len(all)]
}

var allMap map[string]Equipconfig
func Get(key string) (Equipconfig,bool){
    return allMap[key]
}
        public static Equipconfig Get(string entry)
        {
            Equipconfig v;
            return all.TryGetValue(entry, out v) ? v : null;
        }

        public static List<Equipconfig> All()
        {
            return all.OrderedValues;
        }

        public static List<Equipconfig> Filter(Predicate<Equipconfig> predicate)
        {
            var r = new List<Equipconfig>();
            foreach (var e in all.OrderedValues)
            {
                if (predicate(e))
                    r.Add(e);
            }
            return r;
        }

        internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
        {
            all = new Config.KeyedList<string, Equipconfig>();
            for (var c = os.ReadInt32(); c > 0; c--) {
                var self = _create(os);
                all.Add(self.Entry, self);
                if (self.Entry.Trim().Length == 0)
                    continue;
                switch(self.Entry.Trim())
                {
                    case "Instance":
                        if (Instance != null)
                            errors.EnumDup("equip.equipconfig", self.ToString());
                        Instance = self;
                        break;
                    case "Instance2":
                        if (Instance2 != null)
                            errors.EnumDup("equip.equipconfig", self.ToString());
                        Instance2 = self;
                        break;
                    default:
                        errors.EnumDataAdd("equip.equipconfig", self.ToString());
                        break;
                }
            }
            if (Instance == null)
                errors.EnumNull("equip.equipconfig", "Instance");
            if (Instance2 == null)
                errors.EnumNull("equip.equipconfig", "Instance2");
        }


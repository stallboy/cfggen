package task

type Completeconditiontype struct {
    id int //任务完成条件类型（id的范围为1-100）
    name string //程序用名字
}

//entries
var (
    killMonster Completeconditiontype
    talkNpc Completeconditiontype
    collectItem Completeconditiontype
    conditionAnd Completeconditiontype
    chat Completeconditiontype
    testNoColumn Completeconditiontype
)

//getters
func (t *Completeconditiontype) GetId() int {
    return t.id
}

func (t *Completeconditiontype) GetName() string {
    return t.name
}

var all []Completeconditiontype
func GetAll() []Completeconditiontype {
    return all[:len(all)]:len(all)]
}

var allMap map[int]Completeconditiontype
func Get(key int) (Completeconditiontype,bool){
    return allMap[key]
}
        public static Completeconditiontype Get(int id)
        {
            Completeconditiontype v;
            return all.TryGetValue(id, out v) ? v : null;
        }

        public static List<Completeconditiontype> All()
        {
            return all.OrderedValues;
        }

        public static List<Completeconditiontype> Filter(Predicate<Completeconditiontype> predicate)
        {
            var r = new List<Completeconditiontype>();
            foreach (var e in all.OrderedValues)
            {
                if (predicate(e))
                    r.Add(e);
            }
            return r;
        }

        internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
        {
            all = new Config.KeyedList<int, Completeconditiontype>();
            for (var c = os.ReadInt32(); c > 0; c--) {
                var self = _create(os);
                all.Add(self.Id, self);
                if (self.Name.Trim().Length == 0)
                    continue;
                switch(self.Name.Trim())
                {
                    case "KillMonster":
                        if (KillMonster != null)
                            errors.EnumDup("task.completeconditiontype", self.ToString());
                        KillMonster = self;
                        break;
                    case "TalkNpc":
                        if (TalkNpc != null)
                            errors.EnumDup("task.completeconditiontype", self.ToString());
                        TalkNpc = self;
                        break;
                    case "CollectItem":
                        if (CollectItem != null)
                            errors.EnumDup("task.completeconditiontype", self.ToString());
                        CollectItem = self;
                        break;
                    case "ConditionAnd":
                        if (ConditionAnd != null)
                            errors.EnumDup("task.completeconditiontype", self.ToString());
                        ConditionAnd = self;
                        break;
                    case "Chat":
                        if (Chat != null)
                            errors.EnumDup("task.completeconditiontype", self.ToString());
                        Chat = self;
                        break;
                    case "TestNoColumn":
                        if (TestNoColumn != null)
                            errors.EnumDup("task.completeconditiontype", self.ToString());
                        TestNoColumn = self;
                        break;
                    default:
                        errors.EnumDataAdd("task.completeconditiontype", self.ToString());
                        break;
                }
            }
            if (KillMonster == null)
                errors.EnumNull("task.completeconditiontype", "KillMonster");
            if (TalkNpc == null)
                errors.EnumNull("task.completeconditiontype", "TalkNpc");
            if (CollectItem == null)
                errors.EnumNull("task.completeconditiontype", "CollectItem");
            if (ConditionAnd == null)
                errors.EnumNull("task.completeconditiontype", "ConditionAnd");
            if (Chat == null)
                errors.EnumNull("task.completeconditiontype", "Chat");
            if (TestNoColumn == null)
                errors.EnumNull("task.completeconditiontype", "TestNoColumn");
        }


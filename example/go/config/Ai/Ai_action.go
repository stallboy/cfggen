package ai

type Ai_action struct {
    iD int
    desc string //描述
    formulaID int //公式
    argIList []int //参数(int)1
    argSList []int //参数(string)1
}

//getters
func (t *Ai_action) GetID() int {
    return t.iD
}

func (t *Ai_action) GetDesc() string {
    return t.desc
}

func (t *Ai_action) GetFormulaID() int {
    return t.formulaID
}

func (t *Ai_action) GetArgIList() []int {
    return t.argIList
}

func (t *Ai_action) GetArgSList() []int {
    return t.argSList
}

var all []Ai_action
func GetAll() []Ai_action {
    return all[:len(all)]:len(all)]
}

var allMap map[int]Ai_action
func Get(key int) (Ai_action,bool){
    return allMap[key]
}
        public static Ai_action Get(int iD)
        {
            Ai_action v;
            return all.TryGetValue(iD, out v) ? v : null;
        }

        public static List<Ai_action> All()
        {
            return all.OrderedValues;
        }

        public static List<Ai_action> Filter(Predicate<Ai_action> predicate)
        {
            var r = new List<Ai_action>();
            foreach (var e in all.OrderedValues)
            {
                if (predicate(e))
                    r.Add(e);
            }
            return r;
        }

        internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
        {
            all = new Config.KeyedList<int, Ai_action>();
            for (var c = os.ReadInt32(); c > 0; c--) {
                var self = _create(os);
                all.Add(self.ID, self);
            }
        }


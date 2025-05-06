package ai

type Ai_condition struct {
    iD int
    desc string //描述
    formulaID int //公式
    argIList []int //参数(int)1
    argSList []int //参数(string)1
}

//getters
func (t *Ai_condition) GetID() int {
    return t.iD
}

func (t *Ai_condition) GetDesc() string {
    return t.desc
}

func (t *Ai_condition) GetFormulaID() int {
    return t.formulaID
}

func (t *Ai_condition) GetArgIList() []int {
    return t.argIList
}

func (t *Ai_condition) GetArgSList() []int {
    return t.argSList
}

var all []Ai_condition
func GetAll() []Ai_condition {
    return all[:len(all)]:len(all)]
}

var allMap map[int]Ai_condition
func Get(key int) (Ai_condition,bool){
    return allMap[key]
}
        public static Ai_condition Get(int iD)
        {
            Ai_condition v;
            return all.TryGetValue(iD, out v) ? v : null;
        }

        public static List<Ai_condition> All()
        {
            return all.OrderedValues;
        }

        public static List<Ai_condition> Filter(Predicate<Ai_condition> predicate)
        {
            var r = new List<Ai_condition>();
            foreach (var e in all.OrderedValues)
            {
                if (predicate(e))
                    r.Add(e);
            }
            return r;
        }

        internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
        {
            all = new Config.KeyedList<int, Ai_condition>();
            for (var c = os.ReadInt32(); c > 0; c--) {
                var self = _create(os);
                all.Add(self.ID, self);
            }
        }


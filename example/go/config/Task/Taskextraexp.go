package task

type Taskextraexp struct {
    taskid int //任务完成条件类型（id的范围为1-100）
    extraexp int //额外奖励经验
    test1 string
    test2 string
    fielda string
    fieldb string
    fieldc string
    fieldd string
}

//getters
func (t *Taskextraexp) GetTaskid() int {
    return t.taskid
}

func (t *Taskextraexp) GetExtraexp() int {
    return t.extraexp
}

func (t *Taskextraexp) GetTest1() string {
    return t.test1
}

func (t *Taskextraexp) GetTest2() string {
    return t.test2
}

func (t *Taskextraexp) GetFielda() string {
    return t.fielda
}

func (t *Taskextraexp) GetFieldb() string {
    return t.fieldb
}

func (t *Taskextraexp) GetFieldc() string {
    return t.fieldc
}

func (t *Taskextraexp) GetFieldd() string {
    return t.fieldd
}

var all []Taskextraexp
func GetAll() []Taskextraexp {
    return all[:len(all)]:len(all)]
}

var allMap map[int]Taskextraexp
func Get(key int) (Taskextraexp,bool){
    return allMap[key]
}
        public static Taskextraexp Get(int taskid)
        {
            Taskextraexp v;
            return all.TryGetValue(taskid, out v) ? v : null;
        }

        public static List<Taskextraexp> All()
        {
            return all.OrderedValues;
        }

        public static List<Taskextraexp> Filter(Predicate<Taskextraexp> predicate)
        {
            var r = new List<Taskextraexp>();
            foreach (var e in all.OrderedValues)
            {
                if (predicate(e))
                    r.Add(e);
            }
            return r;
        }

        internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
        {
            all = new Config.KeyedList<int, Taskextraexp>();
            for (var c = os.ReadInt32(); c > 0; c--) {
                var self = _create(os);
                all.Add(self.Taskid, self);
            }
        }


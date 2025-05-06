package task

type Task struct {
    taskid int //任务完成条件类型（id的范围为1-100）
    name []string //程序用名字
    nexttask int
    completecondition config.Task.Completecondition
    exp int
    testDefaultBean config.Task.TestDefaultBean //测试
    nullableRefTaskid config_task.Taskextraexp
    nullableRefNexttask config_task.Task
}

//getters
func (t *Task) GetTaskid() int {
    return t.taskid
}

func (t *Task) GetName() []string {
    return t.name
}

func (t *Task) GetNexttask() int {
    return t.nexttask
}

func (t *Task) GetCompletecondition() config.Task.Completecondition {
    return t.completecondition
}

func (t *Task) GetExp() int {
    return t.exp
}

func (t *Task) GetTestDefaultBean() config.Task.TestDefaultBean {
    return t.testDefaultBean
}

//ref properties
func (t *task.task) GetNullableRefTaskid() config_task.Taskextraexp {
    return t.nullableRefTaskid
}
func (t *task.task) GetNullableRefNexttask() config_task.Task {
    return t.nullableRefNexttask
}

var all []Task
func GetAll() []Task {
    return all[:len(all)]:len(all)]
}

var allMap map[int]Task
func Get(key int) (Task,bool){
    return allMap[key]
}
        public static Task Get(int taskid)
        {
            Task v;
            return all.TryGetValue(taskid, out v) ? v : null;
        }

        public static List<Task> All()
        {
            return all.OrderedValues;
        }

        public static List<Task> Filter(Predicate<Task> predicate)
        {
            var r = new List<Task>();
            foreach (var e in all.OrderedValues)
            {
                if (predicate(e))
                    r.Add(e);
            }
            return r;
        }

        internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
        {
            all = new Config.KeyedList<int, Task>();
            for (var c = os.ReadInt32(); c > 0; c--) {
                var self = _create(os);
                all.Add(self.Taskid, self);
            }
        }

        internal static void Resolve(Config.LoadErrors errors) {
            foreach (var v in All())
                v._resolve(errors);
        }


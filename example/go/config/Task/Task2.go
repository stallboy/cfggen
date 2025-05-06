package task

type Task2 struct {
    taskid int //任务完成条件类型（id的范围为1-100）
    name []string
    nexttask int
    completecondition config.Task.Completecondition
    exp int
    testBool bool
    testString string
    testStruct config.Position
    testList []int
    testListStruct []config.Position
    testListInterface []config.Ai.TriggerTick
    nullableRefTaskid config_task.Taskextraexp
    nullableRefNexttask config_task.Task
}

//getters
func (t *Task2) GetTaskid() int {
    return t.taskid
}

func (t *Task2) GetName() []string {
    return t.name
}

func (t *Task2) GetNexttask() int {
    return t.nexttask
}

func (t *Task2) GetCompletecondition() config.Task.Completecondition {
    return t.completecondition
}

func (t *Task2) GetExp() int {
    return t.exp
}

func (t *Task2) GetTestBool() bool {
    return t.testBool
}

func (t *Task2) GetTestString() string {
    return t.testString
}

func (t *Task2) GetTestStruct() config.Position {
    return t.testStruct
}

func (t *Task2) GetTestList() []int {
    return t.testList
}

func (t *Task2) GetTestListStruct() []config.Position {
    return t.testListStruct
}

func (t *Task2) GetTestListInterface() []config.Ai.TriggerTick {
    return t.testListInterface
}

//ref properties
func (t *task.task2) GetNullableRefTaskid() config_task.Taskextraexp {
    return t.nullableRefTaskid
}
func (t *task.task2) GetNullableRefNexttask() config_task.Task {
    return t.nullableRefNexttask
}

var all []Task2
func GetAll() []Task2 {
    return all[:len(all)]:len(all)]
}

var allMap map[int]Task2
func Get(key int) (Task2,bool){
    return allMap[key]
}
        public static Task2 Get(int taskid)
        {
            Task2 v;
            return all.TryGetValue(taskid, out v) ? v : null;
        }

        public static List<Task2> All()
        {
            return all.OrderedValues;
        }

        public static List<Task2> Filter(Predicate<Task2> predicate)
        {
            var r = new List<Task2>();
            foreach (var e in all.OrderedValues)
            {
                if (predicate(e))
                    r.Add(e);
            }
            return r;
        }

        internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
        {
            all = new Config.KeyedList<int, Task2>();
            for (var c = os.ReadInt32(); c > 0; c--) {
                var self = _create(os);
                all.Add(self.Taskid, self);
            }
        }

        internal static void Resolve(Config.LoadErrors errors) {
            foreach (var v in All())
                v._resolve(errors);
        }


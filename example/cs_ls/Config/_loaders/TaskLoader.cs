namespace Config.Task
{
    public partial class DTestDefaultBean
    {
        internal static DTestDefaultBean _create(ConfigReader reader)
        {
            var valueTestInt = reader.ReadInt32();
            var valueTestBool = reader.ReadBool();
            var valueTestString = reader.ReadStringInPool();
            var valueTestSubBean = DPosition._create(reader);
            int Count_valueTestList = reader.ReadCount(0);
            IReadOnlyList<int> valueTestList;
            if (Count_valueTestList == 0) valueTestList = Array.Empty<int>();
            else {
                var values_valueTestList = new List<int>(Count_valueTestList);
                for (int i = 0; i < Count_valueTestList; i++) values_valueTestList.Add(reader.ReadInt32());
                valueTestList = values_valueTestList.AsReadOnly();
            }
            int Count_valueTestList2 = reader.ReadCount(0);
            IReadOnlyList<int> valueTestList2;
            if (Count_valueTestList2 == 0) valueTestList2 = Array.Empty<int>();
            else {
                var values_valueTestList2 = new List<int>(Count_valueTestList2);
                for (int i = 0; i < Count_valueTestList2; i++) values_valueTestList2.Add(reader.ReadInt32());
                valueTestList2 = values_valueTestList2.AsReadOnly();
            }
            int Count_valueTestMap = reader.ReadCount(0);
            var valueTestMap = Count_valueTestMap == 0 ? ConfigMap<int, string>.Empty : new ConfigMap<int, string>(Count_valueTestMap);
            for (int i = 0; i < Count_valueTestMap; i++) {
                var key = reader.ReadInt32();
                var value = reader.ReadStringInPool();
                if (!valueTestMap.TryAdd(key, value)) reader.Errors.DuplicateKey("task.TestDefaultBean.testMap", key);
            }
            return new DTestDefaultBean {
                TestInt = valueTestInt,
                TestBool = valueTestBool,
                TestString = valueTestString,
                TestSubBean = valueTestSubBean,
                TestList = valueTestList,
                TestList2 = valueTestList2,
                TestMap = valueTestMap,
            };
        }

        public override int GetHashCode()
        {
            return TestInt.GetHashCode() + TestBool.GetHashCode() + TestString.GetHashCode() + TestSubBean.GetHashCode() + TestList.GetHashCode() + TestList2.GetHashCode() + TestMap.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DTestDefaultBean o && TestInt.Equals(o.TestInt) && TestBool.Equals(o.TestBool) && TestString.Equals(o.TestString) && TestSubBean.Equals(o.TestSubBean) && TestList.Equals(o.TestList) && TestList2.Equals(o.TestList2) && TestMap.Equals(o.TestMap);
        }

        public override string ToString()
        {
            return "(" + TestInt + "," + TestBool + "," + TestString + "," + TestSubBean + "," + StringUtil.ToString(TestList) + "," + StringUtil.ToString(TestList2) + "," + TestMap + ")";
        }


    }

    public partial class DCompleteconditiontypeInfo
    {
        private static readonly int[] _symbolKeys = { 1, 2, 3, 4, 5, 6, 7 };
        internal static int PrimaryKey(DCompleteconditiontypeInfo self) => self.Id;
        internal static void InitializeAll(List<DCompleteconditiontypeInfo> list, LoadErrors errors, ConfigMgr config)
        {
            int count = list.Count;
            var s_all = new Dictionary<int, DCompleteconditiontypeInfo>(count);
            foreach (var self in list)
            {
                var key = PrimaryKey(self);
                s_all.Add(key, self);
                string name = self.Name;
                int symbol = (int)self.EEnum;
                if (symbol < 0) errors.UnknownSymbol("task.completeconditiontype", name, key, config.Explain("task.completeconditiontype", key));
                else {
                    if (!key.Equals(_symbolKeys[symbol])) errors.ChangedSymbolId("task.completeconditiontype", name, _symbolKeys[symbol], key, config.Explain("task.completeconditiontype", key));
                }
            }
            config._task_dcompleteconditiontype_All = new System.Collections.ObjectModel.ReadOnlyDictionary<int, DCompleteconditiontypeInfo>(s_all);
            config._task_dcompleteconditiontype_Symbols[(int)DCompleteconditiontype.KillMonster] = Get(config, 1);
            if (config._task_dcompleteconditiontype_Symbols[(int)DCompleteconditiontype.KillMonster] == null) errors.MissingSymbol("task.completeconditiontype", "KillMonster", 1);
            config._task_dcompleteconditiontype_Symbols[(int)DCompleteconditiontype.TalkNpc] = Get(config, 2);
            if (config._task_dcompleteconditiontype_Symbols[(int)DCompleteconditiontype.TalkNpc] == null) errors.MissingSymbol("task.completeconditiontype", "TalkNpc", 2);
            config._task_dcompleteconditiontype_Symbols[(int)DCompleteconditiontype.CollectItem] = Get(config, 3);
            if (config._task_dcompleteconditiontype_Symbols[(int)DCompleteconditiontype.CollectItem] == null) errors.MissingSymbol("task.completeconditiontype", "CollectItem", 3);
            config._task_dcompleteconditiontype_Symbols[(int)DCompleteconditiontype.ConditionAnd] = Get(config, 4);
            if (config._task_dcompleteconditiontype_Symbols[(int)DCompleteconditiontype.ConditionAnd] == null) errors.MissingSymbol("task.completeconditiontype", "ConditionAnd", 4);
            config._task_dcompleteconditiontype_Symbols[(int)DCompleteconditiontype.Chat] = Get(config, 5);
            if (config._task_dcompleteconditiontype_Symbols[(int)DCompleteconditiontype.Chat] == null) errors.MissingSymbol("task.completeconditiontype", "Chat", 5);
            config._task_dcompleteconditiontype_Symbols[(int)DCompleteconditiontype.TestNoColumn] = Get(config, 6);
            if (config._task_dcompleteconditiontype_Symbols[(int)DCompleteconditiontype.TestNoColumn] == null) errors.MissingSymbol("task.completeconditiontype", "TestNoColumn", 6);
            config._task_dcompleteconditiontype_Symbols[(int)DCompleteconditiontype.Aa] = Get(config, 7);
            if (config._task_dcompleteconditiontype_Symbols[(int)DCompleteconditiontype.Aa] == null) errors.MissingSymbol("task.completeconditiontype", "aa", 7);
            config._task_dcompleteconditiontype_Rows = list.AsReadOnly();
        }

        internal static DCompleteconditiontypeInfo _create(ConfigReader reader)
        {
            var valueId = reader.ReadInt32();
            var valueName = reader.ReadStringInPool();
            return new DCompleteconditiontypeInfo {
                Id = valueId,
                Name = valueName,
                EEnum = valueName switch {
                    "KillMonster" => DCompleteconditiontype.KillMonster,
                    "TalkNpc" => DCompleteconditiontype.TalkNpc,
                    "CollectItem" => DCompleteconditiontype.CollectItem,
                    "ConditionAnd" => DCompleteconditiontype.ConditionAnd,
                    "Chat" => DCompleteconditiontype.Chat,
                    "TestNoColumn" => DCompleteconditiontype.TestNoColumn,
                    "aa" => DCompleteconditiontype.Aa,
                    _ => (DCompleteconditiontype)(-1)
                }
            };
        }

        public override int GetHashCode()
        {
            return Id.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DCompleteconditiontypeInfo o && Id.Equals(o.Id);
        }

        public override string ToString()
        {
            return "(" + Id + "," + Name + ")";
        }


    }

    public partial class DTask
    {
        internal static int PrimaryKey(DTask self) => self.Taskid;
        internal static void InitializeAll(List<DTask> list, LoadErrors errors, ConfigMgr config)
        {
            int count = list.Count;
            var s_all = new Dictionary<int, DTask>(count);
            foreach (var self in list)
            {
                var key = PrimaryKey(self);
                s_all.Add(key, self);
            }
            config._task_dtask_All = new System.Collections.ObjectModel.ReadOnlyDictionary<int, DTask>(s_all);
            config._task_dtask_Rows = list.AsReadOnly();
        }

        internal static DTask _create(ConfigReader reader)
        {
            var valueTaskid = reader.ReadInt32();
            int Count_valueName = reader.ReadCount(0);
            IReadOnlyList<Config.Text> valueName;
            if (Count_valueName == 0) valueName = Array.Empty<Config.Text>();
            else {
                var values_valueName = new List<Config.Text>(Count_valueName);
                for (int i = 0; i < Count_valueName; i++) values_valueName.Add(Config.Text._create(reader));
                valueName = values_valueName.AsReadOnly();
            }
            var valueNexttask = reader.ReadInt32();
            var valueCompletecondition = Task.DCompletecondition._create(reader);
            var valueExp = reader.ReadInt32();
            var valueTestDefaultBean = Task.DTestDefaultBean._create(reader);
            return new DTask {
                Taskid = valueTaskid,
                Name = valueName,
                Nexttask = valueNexttask,
                Completecondition = valueCompletecondition,
                Exp = valueExp,
                TestDefaultBean = valueTestDefaultBean,
            };
        }

        public override int GetHashCode()
        {
            return Taskid.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DTask o && Taskid.Equals(o.Taskid);
        }

        public override string ToString()
        {
            return "(" + Taskid + "," + StringUtil.ToString(Name) + "," + Nexttask + "," + Completecondition + "," + Exp + "," + TestDefaultBean + ")";
        }

        internal void _resolve(LoadErrors errors, ConfigMgr config)
        {
            Completecondition._resolve(errors, config);
            NullableRefTaskid = Task.DTaskextraexp.Get(config, Taskid);
            NullableRefNexttask = Task.DTask.Get(config, Nexttask);
        }

    }

    public partial class DTask2
    {
        internal static int PrimaryKey(DTask2 self) => self.Taskid;
        internal static void InitializeAll(List<DTask2> list, LoadErrors errors, ConfigMgr config)
        {
            int count = list.Count;
            var s_all = new Dictionary<int, DTask2>(count);
            foreach (var self in list)
            {
                var key = PrimaryKey(self);
                s_all.Add(key, self);
            }
            config._task_dtask2_All = new System.Collections.ObjectModel.ReadOnlyDictionary<int, DTask2>(s_all);
            config._task_dtask2_Rows = list.AsReadOnly();
        }

        internal static DTask2 _create(ConfigReader reader)
        {
            var valueTaskid = reader.ReadInt32();
            int Count_valueName = reader.ReadCount(0);
            IReadOnlyList<Config.Text> valueName;
            if (Count_valueName == 0) valueName = Array.Empty<Config.Text>();
            else {
                var values_valueName = new List<Config.Text>(Count_valueName);
                for (int i = 0; i < Count_valueName; i++) values_valueName.Add(Config.Text._create(reader));
                valueName = values_valueName.AsReadOnly();
            }
            var valueNexttask = reader.ReadInt32();
            var valueCompletecondition = Task.DCompletecondition._create(reader);
            var valueExp = reader.ReadInt32();
            var valueTestBool = reader.ReadBool();
            var valueTestString = reader.ReadStringInPool();
            var valueTestStruct = DPosition._create(reader);
            int Count_valueTestList = reader.ReadCount(0);
            IReadOnlyList<int> valueTestList;
            if (Count_valueTestList == 0) valueTestList = Array.Empty<int>();
            else {
                var values_valueTestList = new List<int>(Count_valueTestList);
                for (int i = 0; i < Count_valueTestList; i++) values_valueTestList.Add(reader.ReadInt32());
                valueTestList = values_valueTestList.AsReadOnly();
            }
            int Count_valueTestListStruct = reader.ReadCount(0);
            IReadOnlyList<DPosition> valueTestListStruct;
            if (Count_valueTestListStruct == 0) valueTestListStruct = Array.Empty<DPosition>();
            else {
                var values_valueTestListStruct = new List<DPosition>(Count_valueTestListStruct);
                for (int i = 0; i < Count_valueTestListStruct; i++) values_valueTestListStruct.Add(DPosition._create(reader));
                valueTestListStruct = values_valueTestListStruct.AsReadOnly();
            }
            int Count_valueTestListInterface = reader.ReadCount(0);
            IReadOnlyList<Ai.DTriggerTick> valueTestListInterface;
            if (Count_valueTestListInterface == 0) valueTestListInterface = Array.Empty<Ai.DTriggerTick>();
            else {
                var values_valueTestListInterface = new List<Ai.DTriggerTick>(Count_valueTestListInterface);
                for (int i = 0; i < Count_valueTestListInterface; i++) values_valueTestListInterface.Add(Ai.DTriggerTick._create(reader));
                valueTestListInterface = values_valueTestListInterface.AsReadOnly();
            }
            return new DTask2 {
                Taskid = valueTaskid,
                Name = valueName,
                Nexttask = valueNexttask,
                Completecondition = valueCompletecondition,
                Exp = valueExp,
                TestBool = valueTestBool,
                TestString = valueTestString,
                TestStruct = valueTestStruct,
                TestList = valueTestList,
                TestListStruct = valueTestListStruct,
                TestListInterface = valueTestListInterface,
            };
        }

        public override int GetHashCode()
        {
            return Taskid.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DTask2 o && Taskid.Equals(o.Taskid);
        }

        public override string ToString()
        {
            return "(" + Taskid + "," + StringUtil.ToString(Name) + "," + Nexttask + "," + Completecondition + "," + Exp + "," + TestBool + "," + TestString + "," + TestStruct + "," + StringUtil.ToString(TestList) + "," + StringUtil.ToString(TestListStruct) + "," + StringUtil.ToString(TestListInterface) + ")";
        }

        internal void _resolve(LoadErrors errors, ConfigMgr config)
        {
            Completecondition._resolve(errors, config);
            NullableRefTaskid = Task.DTaskextraexp.Get(config, Taskid);
            NullableRefNexttask = Task.DTask.Get(config, Nexttask);
        }

    }

    public partial class DTaskextraexp
    {
        internal static int PrimaryKey(DTaskextraexp self) => self.Taskid;
        internal static void InitializeAll(List<DTaskextraexp> list, LoadErrors errors, ConfigMgr config)
        {
            int count = list.Count;
            var s_all = new Dictionary<int, DTaskextraexp>(count);
            foreach (var self in list)
            {
                var key = PrimaryKey(self);
                s_all.Add(key, self);
            }
            config._task_dtaskextraexp_All = new System.Collections.ObjectModel.ReadOnlyDictionary<int, DTaskextraexp>(s_all);
            config._task_dtaskextraexp_Rows = list.AsReadOnly();
        }

        internal static DTaskextraexp _create(ConfigReader reader)
        {
            var valueTaskid = reader.ReadInt32();
            var valueExtraexp = reader.ReadInt32();
            var valueTest1 = reader.ReadStringInPool();
            var valueTest2 = reader.ReadStringInPool();
            var valueFielda = reader.ReadStringInPool();
            var valueFieldb = reader.ReadStringInPool();
            var valueFieldc = reader.ReadStringInPool();
            var valueFieldd = reader.ReadStringInPool();
            return new DTaskextraexp {
                Taskid = valueTaskid,
                Extraexp = valueExtraexp,
                Test1 = valueTest1,
                Test2 = valueTest2,
                Fielda = valueFielda,
                Fieldb = valueFieldb,
                Fieldc = valueFieldc,
                Fieldd = valueFieldd,
            };
        }

        public override int GetHashCode()
        {
            return Taskid.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DTaskextraexp o && Taskid.Equals(o.Taskid);
        }

        public override string ToString()
        {
            return "(" + Taskid + "," + Extraexp + "," + Test1 + "," + Test2 + "," + Fielda + "," + Fieldb + "," + Fieldc + "," + Fieldd + ")";
        }


    }

    public partial interface DCompletecondition
    {
        void _resolve(LoadErrors errors, ConfigMgr config) { }
        internal static DCompletecondition _create(ConfigReader reader)
        {
            return reader.ReadStringInPool() switch {
                "KillMonster" => Task.Completecondition.DKillMonster._create(reader),
                "TalkNpc" => Task.Completecondition.DTalkNpc._create(reader),
                "TestNoColumn" => Task.Completecondition.DTestNoColumn._create(reader),
                "Chat" => Task.Completecondition.DChat._create(reader),
                "ConditionAnd" => Task.Completecondition.DConditionAnd._create(reader),
                "CollectItem" => Task.Completecondition.DCollectItem._create(reader),
                "aa" => Task.Completecondition.DAa._create(reader),
                var impl => throw reader.Errors.UnknownVariant("task.completecondition", impl)
            };
        }
    }

}

namespace Config.Task.Completecondition
{
    public partial class DKillMonster
    {
        internal static DKillMonster _create(ConfigReader reader)
        {
            var valueMonsterid = reader.ReadInt32();
            var valueCount = reader.ReadInt32();
            return new DKillMonster {
                Monsterid = valueMonsterid,
                Count = valueCount,
            };
        }

        public override int GetHashCode()
        {
            return Monsterid.GetHashCode() + Count.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DKillMonster o && Monsterid.Equals(o.Monsterid) && Count.Equals(o.Count);
        }

        public override string ToString()
        {
            return "(" + Monsterid + "," + Count + ")";
        }

        public void _resolve(LoadErrors errors, ConfigMgr config)
        {
            var rRefMonsterid = Other.DMonster.Get(config, Monsterid);
            if (rRefMonsterid == null) errors.MissingReference("KillMonster", "monsterid", "other.monster", this.Monsterid);
            else RefMonsterid = rRefMonsterid;
        }

    }

    public partial class DTalkNpc
    {
        internal static DTalkNpc _create(ConfigReader reader)
        {
            var valueNpcid = reader.ReadInt32();
            return new DTalkNpc {
                Npcid = valueNpcid,
            };
        }

        public override int GetHashCode()
        {
            return Npcid.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DTalkNpc o && Npcid.Equals(o.Npcid);
        }

        public override string ToString()
        {
            return "(" + Npcid + ")";
        }


    }

    public partial class DTestNoColumn
    {
        internal static DTestNoColumn _create(ConfigReader reader)
        {
            return new DTestNoColumn();
        }

        public override int GetHashCode()
        {
            return this.GetType().GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DTestNoColumn;
        }


    }

    public partial class DChat
    {
        internal static DChat _create(ConfigReader reader)
        {
            var valueMsg = reader.ReadStringInPool();
            return new DChat {
                Msg = valueMsg,
            };
        }

        public override int GetHashCode()
        {
            return Msg.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DChat o && Msg.Equals(o.Msg);
        }

        public override string ToString()
        {
            return "(" + Msg + ")";
        }


    }

    public partial class DConditionAnd
    {
        internal static DConditionAnd _create(ConfigReader reader)
        {
            var valueCond1 = Task.DCompletecondition._create(reader);
            var valueCond2 = Task.DCompletecondition._create(reader);
            return new DConditionAnd {
                Cond1 = valueCond1,
                Cond2 = valueCond2,
            };
        }

        public override int GetHashCode()
        {
            return Cond1.GetHashCode() + Cond2.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DConditionAnd o && Cond1.Equals(o.Cond1) && Cond2.Equals(o.Cond2);
        }

        public override string ToString()
        {
            return "(" + Cond1 + "," + Cond2 + ")";
        }

        public void _resolve(LoadErrors errors, ConfigMgr config)
        {
            Cond1._resolve(errors, config);
            Cond2._resolve(errors, config);
        }

    }

    public partial class DCollectItem
    {
        internal static DCollectItem _create(ConfigReader reader)
        {
            var valueItemid = reader.ReadInt32();
            var valueCount = reader.ReadInt32();
            return new DCollectItem {
                Itemid = valueItemid,
                Count = valueCount,
            };
        }

        public override int GetHashCode()
        {
            return Itemid.GetHashCode() + Count.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DCollectItem o && Itemid.Equals(o.Itemid) && Count.Equals(o.Count);
        }

        public override string ToString()
        {
            return "(" + Itemid + "," + Count + ")";
        }


    }

    public partial class DAa
    {
        internal static DAa _create(ConfigReader reader)
        {
            return new DAa();
        }

        public override int GetHashCode()
        {
            return this.GetType().GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            return obj is DAa;
        }


    }

}


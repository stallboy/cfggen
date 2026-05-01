using System.Collections.Frozen;

namespace Config.Task
{
    public partial class DTestDefaultBean
    {
        internal static DTestDefaultBean _create(ConfigReader reader)
        {
            var testInt = reader.ReadInt32();
            var testBool = reader.ReadBool();
            var testString = reader.ReadStringInPool();
            var testSubBean = DPosition._create(reader);
            int Count_testList = reader.ReadInt32();
            var testList = new List<int>(Count_testList);
            for (int i = 0; i < Count_testList; i++)
                testList.Add(reader.ReadInt32());
            int Count_testList2 = reader.ReadInt32();
            var testList2 = new List<int>(Count_testList2);
            for (int i = 0; i < Count_testList2; i++)
                testList2.Add(reader.ReadInt32());
            int Count_testMap = reader.ReadInt32();
            var testMap = new OrderedDictionary<int, string>(Count_testMap);
            for (int i = 0; i < Count_testMap; i++)
                testMap.Add(reader.ReadInt32(), reader.ReadStringInPool());
            return new DTestDefaultBean {
                TestInt = testInt,
                TestBool = testBool,
                TestString = testString,
                TestSubBean = testSubBean,
                TestList = testList,
                TestList2 = testList2,
                TestMap = testMap,
            };
        }

        public override int GetHashCode()
        {
            return TestInt.GetHashCode() + TestBool.GetHashCode() + TestString.GetHashCode() + TestSubBean.GetHashCode() + TestList.GetHashCode() + TestList2.GetHashCode() + TestMap.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DTestDefaultBean;
            return o != null && TestInt.Equals(o.TestInt) && TestBool.Equals(o.TestBool) && TestString.Equals(o.TestString) && TestSubBean.Equals(o.TestSubBean) && TestList.Equals(o.TestList) && TestList2.Equals(o.TestList2) && TestMap.Equals(o.TestMap);
        }

        public override string ToString()
        {
            return "(" + TestInt + "," + TestBool + "," + TestString + "," + TestSubBean + "," + StringUtil.ToString(TestList) + "," + StringUtil.ToString(TestList2) + "," + TestMap + ")";
        }

    }

    public partial class DCompleteconditiontypeInfo
    {
        internal static void Initialize(ConfigReader reader)
        {
            int count = reader.ReadInt32();
            var s_all = new Dictionary<int, DCompleteconditiontypeInfo>(count);
            for (int i = 0; i < count; i++)
            {
                var self = _create(reader);
                s_all.Add(self.Id, self);
                DCompleteconditiontypeExtensions._infos[(int)self.EEnum] = self;
            }
            _all = s_all.ToFrozenDictionary();
        }

        internal static DCompleteconditiontypeInfo _create(ConfigReader reader)
        {
            var id = reader.ReadInt32();
            var name = reader.ReadStringInPool();
            return new DCompleteconditiontypeInfo {
                Id = id,
                Name = name,
                EEnum = Enum.Parse<DCompleteconditiontype>(StringUtil.UpperFirstChar(name))
            };
        }

        public override int GetHashCode()
        {
            return Id.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DCompleteconditiontypeInfo;
            return o != null && Id.Equals(o.Id);
        }

        public override string ToString()
        {
            return "(" + Id + "," + Name + ")";
        }

    }

    public partial class DTask
    {
        internal static void Initialize(ConfigReader reader)
        {
            int count = reader.ReadInt32();
            var s_all = new Dictionary<int, DTask>(count);
            for (int i = 0; i < count; i++)
            {
                var self = _create(reader);
                s_all.Add(self.Taskid, self);
            }
            _all = s_all.ToFrozenDictionary();
        }

        internal static void Resolve(ConfigReader reader)
        {
            foreach (var v in All())
                v._resolve(reader);
        }
        internal static DTask _create(ConfigReader reader)
        {
            var taskid = reader.ReadInt32();
            int Count_name = reader.ReadInt32();
            var name = new List<Config.Text>(Count_name);
            for (int i = 0; i < Count_name; i++)
                name.Add(Config.Text._create(reader));
            var nexttask = reader.ReadInt32();
            var completecondition = Task.DCompletecondition._create(reader);
            var exp = reader.ReadInt32();
            var testDefaultBean = Task.DTestDefaultBean._create(reader);
            return new DTask {
                Taskid = taskid,
                Name = name,
                Nexttask = nexttask,
                Completecondition = completecondition,
                Exp = exp,
                TestDefaultBean = testDefaultBean,
            };
        }

        public override int GetHashCode()
        {
            return Taskid.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DTask;
            return o != null && Taskid.Equals(o.Taskid);
        }

        public override string ToString()
        {
            return "(" + Taskid + "," + StringUtil.ToString(Name) + "," + Nexttask + "," + Completecondition + "," + Exp + "," + TestDefaultBean + ")";
        }

        internal void _resolve(ConfigReader reader)
        {
            Completecondition._resolve(reader);
            NullableRefTaskid = Task.DTaskextraexp.Get(Taskid);
            NullableRefNexttask = Task.DTask.Get(Nexttask);
        }
    }

    public partial class DTask2
    {
        internal static void Initialize(ConfigReader reader)
        {
            int count = reader.ReadInt32();
            var s_all = new Dictionary<int, DTask2>(count);
            for (int i = 0; i < count; i++)
            {
                var self = _create(reader);
                s_all.Add(self.Taskid, self);
            }
            _all = s_all.ToFrozenDictionary();
        }

        internal static void Resolve(ConfigReader reader)
        {
            foreach (var v in All())
                v._resolve(reader);
        }
        internal static DTask2 _create(ConfigReader reader)
        {
            var taskid = reader.ReadInt32();
            int Count_name = reader.ReadInt32();
            var name = new List<Config.Text>(Count_name);
            for (int i = 0; i < Count_name; i++)
                name.Add(Config.Text._create(reader));
            var nexttask = reader.ReadInt32();
            var completecondition = Task.DCompletecondition._create(reader);
            var exp = reader.ReadInt32();
            var testBool = reader.ReadBool();
            var testString = reader.ReadStringInPool();
            var testStruct = DPosition._create(reader);
            int Count_testList = reader.ReadInt32();
            var testList = new List<int>(Count_testList);
            for (int i = 0; i < Count_testList; i++)
                testList.Add(reader.ReadInt32());
            int Count_testListStruct = reader.ReadInt32();
            var testListStruct = new List<DPosition>(Count_testListStruct);
            for (int i = 0; i < Count_testListStruct; i++)
                testListStruct.Add(DPosition._create(reader));
            int Count_testListInterface = reader.ReadInt32();
            var testListInterface = new List<Ai.DTriggerTick>(Count_testListInterface);
            for (int i = 0; i < Count_testListInterface; i++)
                testListInterface.Add(Ai.DTriggerTick._create(reader));
            return new DTask2 {
                Taskid = taskid,
                Name = name,
                Nexttask = nexttask,
                Completecondition = completecondition,
                Exp = exp,
                TestBool = testBool,
                TestString = testString,
                TestStruct = testStruct,
                TestList = testList,
                TestListStruct = testListStruct,
                TestListInterface = testListInterface,
            };
        }

        public override int GetHashCode()
        {
            return Taskid.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DTask2;
            return o != null && Taskid.Equals(o.Taskid);
        }

        public override string ToString()
        {
            return "(" + Taskid + "," + StringUtil.ToString(Name) + "," + Nexttask + "," + Completecondition + "," + Exp + "," + TestBool + "," + TestString + "," + TestStruct + "," + StringUtil.ToString(TestList) + "," + StringUtil.ToString(TestListStruct) + "," + StringUtil.ToString(TestListInterface) + ")";
        }

        internal void _resolve(ConfigReader reader)
        {
            Completecondition._resolve(reader);
            NullableRefTaskid = Task.DTaskextraexp.Get(Taskid);
            NullableRefNexttask = Task.DTask.Get(Nexttask);
        }
    }

    public partial class DTaskextraexp
    {
        internal static void Initialize(ConfigReader reader)
        {
            int count = reader.ReadInt32();
            var s_all = new Dictionary<int, DTaskextraexp>(count);
            for (int i = 0; i < count; i++)
            {
                var self = _create(reader);
                s_all.Add(self.Taskid, self);
            }
            _all = s_all.ToFrozenDictionary();
        }

        internal static DTaskextraexp _create(ConfigReader reader)
        {
            var taskid = reader.ReadInt32();
            var extraexp = reader.ReadInt32();
            var test1 = reader.ReadStringInPool();
            var test2 = reader.ReadStringInPool();
            var fielda = reader.ReadStringInPool();
            var fieldb = reader.ReadStringInPool();
            var fieldc = reader.ReadStringInPool();
            var fieldd = reader.ReadStringInPool();
            return new DTaskextraexp {
                Taskid = taskid,
                Extraexp = extraexp,
                Test1 = test1,
                Test2 = test2,
                Fielda = fielda,
                Fieldb = fieldb,
                Fieldc = fieldc,
                Fieldd = fieldd,
            };
        }

        public override int GetHashCode()
        {
            return Taskid.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DTaskextraexp;
            return o != null && Taskid.Equals(o.Taskid);
        }

        public override string ToString()
        {
            return "(" + Taskid + "," + Extraexp + "," + Test1 + "," + Test2 + "," + Fielda + "," + Fieldb + "," + Fieldc + "," + Fieldd + ")";
        }

    }

    public partial interface DCompletecondition
    {
        void _resolve(ConfigReader reader)
        {
        }
        internal static DCompletecondition _create(ConfigReader reader)
        {
            var impl = reader.ReadStringInPool();
            switch(impl)
            {
                case "KillMonster":
                    return Task.Completecondition.DKillMonster._create(reader);
                case "TalkNpc":
                    return Task.Completecondition.DTalkNpc._create(reader);
                case "TestNoColumn":
                    return Task.Completecondition.DTestNoColumn._create(reader);
                case "Chat":
                    return Task.Completecondition.DChat._create(reader);
                case "ConditionAnd":
                    return Task.Completecondition.DConditionAnd._create(reader);
                case "CollectItem":
                    return Task.Completecondition.DCollectItem._create(reader);
                case "aa":
                    return Task.Completecondition.DAa._create(reader);
            }
            throw reader.NotFoundImpl(impl, "task.completecondition");
        }
    }

}

namespace Config.Task.Completecondition
{
    public partial class DKillMonster
    {
        internal static DKillMonster _create(ConfigReader reader)
        {
            var monsterid = reader.ReadInt32();
            var count = reader.ReadInt32();
            return new DKillMonster {
                Monsterid = monsterid,
                Count = count,
            };
        }

        public override int GetHashCode()
        {
            return Monsterid.GetHashCode() + Count.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DKillMonster;
            return o != null && Monsterid.Equals(o.Monsterid) && Count.Equals(o.Count);
        }

        public override string ToString()
        {
            return "(" + Monsterid + "," + Count + ")";
        }

        public void _resolve(ConfigReader reader)
        {
            var rRefMonsterid = Other.DMonster.Get(Monsterid);
            if (rRefMonsterid == null) reader.RefNotFound("KillMonster", "monsterid", Monsterid.ToString());
            else RefMonsterid = rRefMonsterid;
        }
    }

    public partial class DTalkNpc
    {
        internal static DTalkNpc _create(ConfigReader reader)
        {
            var npcid = reader.ReadInt32();
            return new DTalkNpc {
                Npcid = npcid,
            };
        }

        public override int GetHashCode()
        {
            return Npcid.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DTalkNpc;
            return o != null && Npcid.Equals(o.Npcid);
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
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DTestNoColumn;
            return o != null;
        }

    }

    public partial class DChat
    {
        internal static DChat _create(ConfigReader reader)
        {
            var msg = reader.ReadStringInPool();
            return new DChat {
                Msg = msg,
            };
        }

        public override int GetHashCode()
        {
            return Msg.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DChat;
            return o != null && Msg.Equals(o.Msg);
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
            var cond1 = Task.DCompletecondition._create(reader);
            var cond2 = Task.DCompletecondition._create(reader);
            return new DConditionAnd {
                Cond1 = cond1,
                Cond2 = cond2,
            };
        }

        public override int GetHashCode()
        {
            return Cond1.GetHashCode() + Cond2.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DConditionAnd;
            return o != null && Cond1.Equals(o.Cond1) && Cond2.Equals(o.Cond2);
        }

        public override string ToString()
        {
            return "(" + Cond1 + "," + Cond2 + ")";
        }

        public void _resolve(ConfigReader reader)
        {
            Cond1._resolve(reader);
            Cond2._resolve(reader);
        }
    }

    public partial class DCollectItem
    {
        internal static DCollectItem _create(ConfigReader reader)
        {
            var itemid = reader.ReadInt32();
            var count = reader.ReadInt32();
            return new DCollectItem {
                Itemid = itemid,
                Count = count,
            };
        }

        public override int GetHashCode()
        {
            return Itemid.GetHashCode() + Count.GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DCollectItem;
            return o != null && Itemid.Equals(o.Itemid) && Count.Equals(o.Count);
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
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DAa;
            return o != null;
        }

    }

}


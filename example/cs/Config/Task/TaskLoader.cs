namespace Config.Task
{
    public partial class DataTestDefaultBean
    {
        internal static DataTestDefaultBean _create(ConfigReader reader)
        {
            var testInt = reader.ReadInt32();
            var testBool = reader.ReadBool();
            var testString = reader.ReadStringInPool();
            var testSubBean = DataPosition._create(reader);
            List<int> testList = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
                testList.Add(reader.ReadInt32());
            List<int> testList2 = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
                testList2.Add(reader.ReadInt32());
            OrderedDictionary<int, string> testMap = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                testMap.Add(reader.ReadInt32(), reader.ReadStringInPool());
            }
            return new DataTestDefaultBean {
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
            var o = obj as DataTestDefaultBean;
            return o != null && TestInt.Equals(o.TestInt) && TestBool.Equals(o.TestBool) && TestString.Equals(o.TestString) && TestSubBean.Equals(o.TestSubBean) && TestList.Equals(o.TestList) && TestList2.Equals(o.TestList2) && TestMap.Equals(o.TestMap);
        }

        public override string ToString()
        {
            return "(" + TestInt + "," + TestBool + "," + TestString + "," + TestSubBean + "," + StringUtil.ToString(TestList) + "," + StringUtil.ToString(TestList2) + "," + TestMap + ")";
        }

    }

    public partial class DataCompleteconditiontype
    {
        internal static void Initialize(ConfigReader reader)
        {
            _all = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                var self = _create(reader);
                _all.Add(self.Id, self);
                if (self.Name.Trim().Length == 0)
                    continue;
                switch(self.Name.Trim())
                {
                    case "KillMonster":
                        if (KillMonster != null)
                            reader.EnumDuplicateInData("KillMonster");
                        KillMonster = self;
                        break;
                    case "TalkNpc":
                        if (TalkNpc != null)
                            reader.EnumDuplicateInData("TalkNpc");
                        TalkNpc = self;
                        break;
                    case "CollectItem":
                        if (CollectItem != null)
                            reader.EnumDuplicateInData("CollectItem");
                        CollectItem = self;
                        break;
                    case "ConditionAnd":
                        if (ConditionAnd != null)
                            reader.EnumDuplicateInData("ConditionAnd");
                        ConditionAnd = self;
                        break;
                    case "Chat":
                        if (Chat != null)
                            reader.EnumDuplicateInData("Chat");
                        Chat = self;
                        break;
                    case "TestNoColumn":
                        if (TestNoColumn != null)
                            reader.EnumDuplicateInData("TestNoColumn");
                        TestNoColumn = self;
                        break;
                    case "aa":
                        if (Aa != null)
                            reader.EnumDuplicateInData("aa");
                        Aa = self;
                        break;
                    default:
                        reader.EnumNotInCode(self.Name.Trim());
                        break;
                }
            }

            if (KillMonster == null)
                reader.EnumNotInData("KillMonster");
            if (TalkNpc == null)
                reader.EnumNotInData("TalkNpc");
            if (CollectItem == null)
                reader.EnumNotInData("CollectItem");
            if (ConditionAnd == null)
                reader.EnumNotInData("ConditionAnd");
            if (Chat == null)
                reader.EnumNotInData("Chat");
            if (TestNoColumn == null)
                reader.EnumNotInData("TestNoColumn");
            if (Aa == null)
                reader.EnumNotInData("aa");
        }

        internal static DataCompleteconditiontype _create(ConfigReader reader)
        {
            var id = reader.ReadInt32();
            var name = reader.ReadStringInPool();
            return new DataCompleteconditiontype {
                Id = id,
                Name = name,
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
            var o = obj as DataCompleteconditiontype;
            return o != null && Id.Equals(o.Id);
        }

        public override string ToString()
        {
            return "(" + Id + "," + Name + ")";
        }

    }

    public partial class DataTask
    {
        internal static void Initialize(ConfigReader reader)
        {
            _all = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                var self = _create(reader);
                _all.Add(self.Taskid, self);
            }

        }

        internal static void Resolve(ConfigReader reader)
        {
            foreach (var v in All())
                v._resolve(reader);
        }
        internal static DataTask _create(ConfigReader reader)
        {
            var taskid = reader.ReadInt32();
            List<string> name = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
                name.Add(reader.ReadTextInPool());
            var nexttask = reader.ReadInt32();
            var completecondition = Task.DataCompletecondition._create(reader);
            var exp = reader.ReadInt32();
            var testDefaultBean = Task.DataTestDefaultBean._create(reader);
            return new DataTask {
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
            var o = obj as DataTask;
            return o != null && Taskid.Equals(o.Taskid);
        }

        public override string ToString()
        {
            return "(" + Taskid + "," + StringUtil.ToString(Name) + "," + Nexttask + "," + Completecondition + "," + Exp + "," + TestDefaultBean + ")";
        }

    internal void _resolve(ConfigReader reader)
    {
        Completecondition._resolve(reader);
        NullableRefTaskid = Task.DataTaskextraexp.Get(Taskid);
        NullableRefNexttask = Task.DataTask.Get(Nexttask);
    }
    }

    public partial class DataTask2
    {
        internal static void Initialize(ConfigReader reader)
        {
            _all = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                var self = _create(reader);
                _all.Add(self.Taskid, self);
            }

        }

        internal static void Resolve(ConfigReader reader)
        {
            foreach (var v in All())
                v._resolve(reader);
        }
        internal static DataTask2 _create(ConfigReader reader)
        {
            var taskid = reader.ReadInt32();
            List<string> name = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
                name.Add(reader.ReadTextInPool());
            var nexttask = reader.ReadInt32();
            var completecondition = Task.DataCompletecondition._create(reader);
            var exp = reader.ReadInt32();
            var testBool = reader.ReadBool();
            var testString = reader.ReadStringInPool();
            var testStruct = DataPosition._create(reader);
            List<int> testList = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
                testList.Add(reader.ReadInt32());
            List<DataPosition> testListStruct = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
                testListStruct.Add(DataPosition._create(reader));
            List<Ai.DataTriggerTick> testListInterface = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
                testListInterface.Add(Ai.DataTriggerTick._create(reader));
            return new DataTask2 {
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
            var o = obj as DataTask2;
            return o != null && Taskid.Equals(o.Taskid);
        }

        public override string ToString()
        {
            return "(" + Taskid + "," + StringUtil.ToString(Name) + "," + Nexttask + "," + Completecondition + "," + Exp + "," + TestBool + "," + TestString + "," + TestStruct + "," + StringUtil.ToString(TestList) + "," + StringUtil.ToString(TestListStruct) + "," + StringUtil.ToString(TestListInterface) + ")";
        }

    internal void _resolve(ConfigReader reader)
    {
        Completecondition._resolve(reader);
        NullableRefTaskid = Task.DataTaskextraexp.Get(Taskid);
        NullableRefNexttask = Task.DataTask.Get(Nexttask);
    }
    }

    public partial class DataTaskextraexp
    {
        internal static void Initialize(ConfigReader reader)
        {
            _all = [];
            for (var c = reader.ReadInt32(); c > 0; c--)
            {
                var self = _create(reader);
                _all.Add(self.Taskid, self);
            }

        }

        internal static DataTaskextraexp _create(ConfigReader reader)
        {
            var taskid = reader.ReadInt32();
            var extraexp = reader.ReadInt32();
            var test1 = reader.ReadStringInPool();
            var test2 = reader.ReadStringInPool();
            var fielda = reader.ReadStringInPool();
            var fieldb = reader.ReadStringInPool();
            var fieldc = reader.ReadStringInPool();
            var fieldd = reader.ReadStringInPool();
            return new DataTaskextraexp {
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
            var o = obj as DataTaskextraexp;
            return o != null && Taskid.Equals(o.Taskid);
        }

        public override string ToString()
        {
            return "(" + Taskid + "," + Extraexp + "," + Test1 + "," + Test2 + "," + Fielda + "," + Fieldb + "," + Fieldc + "," + Fieldd + ")";
        }

    }

public partial interface DataCompletecondition
{
    internal void _resolve(ConfigReader reader)
    {
    }
    internal static DataCompletecondition _create(ConfigReader reader)
    {
        var impl = reader.ReadStringInPool();
        switch(impl)
        {
            case "KillMonster":
                return Task.Completecondition.DataKillMonster._create(reader);
            case "TalkNpc":
                return Task.Completecondition.DataTalkNpc._create(reader);
            case "TestNoColumn":
                return Task.Completecondition.DataTestNoColumn._create(reader);
            case "Chat":
                return Task.Completecondition.DataChat._create(reader);
            case "ConditionAnd":
                return Task.Completecondition.DataConditionAnd._create(reader);
            case "CollectItem":
                return Task.Completecondition.DataCollectItem._create(reader);
            case "aa":
                return Task.Completecondition.DataAa._create(reader);
        }
        throw reader.NotFoundImpl(impl, "task.completecondition");
    }
}

}

namespace Config.Task.Completecondition
{
    public partial class DataKillMonster
    {
        internal new static DataKillMonster _create(ConfigReader reader)
        {
            var monsterid = reader.ReadInt32();
            var count = reader.ReadInt32();
            return new DataKillMonster {
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
            var o = obj as DataKillMonster;
            return o != null && Monsterid.Equals(o.Monsterid) && Count.Equals(o.Count);
        }

        public override string ToString()
        {
            return "(" + Monsterid + "," + Count + ")";
        }

    internal void _resolve(ConfigReader reader)
    {
        RefMonsterid = Other.DataMonster.Get(Monsterid)!;
        if (RefMonsterid == null) reader.RefNotFound("KillMonster", "monsterid", Monsterid.ToString());
    }
    }

    public partial class DataTalkNpc
    {
        internal new static DataTalkNpc _create(ConfigReader reader)
        {
            var npcid = reader.ReadInt32();
            return new DataTalkNpc {
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
            var o = obj as DataTalkNpc;
            return o != null && Npcid.Equals(o.Npcid);
        }

        public override string ToString()
        {
            return "(" + Npcid + ")";
        }

    }

    public partial class DataTestNoColumn
    {
        internal new static DataTestNoColumn _create(ConfigReader reader)
        {
            return new DataTestNoColumn {
            };
        }

        public override int GetHashCode()
        {
            return this.GetType().GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DataTestNoColumn;
            return o != null;
        }

    }

    public partial class DataChat
    {
        internal new static DataChat _create(ConfigReader reader)
        {
            var msg = reader.ReadStringInPool();
            return new DataChat {
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
            var o = obj as DataChat;
            return o != null && Msg.Equals(o.Msg);
        }

        public override string ToString()
        {
            return "(" + Msg + ")";
        }

    }

    public partial class DataConditionAnd
    {
        internal new static DataConditionAnd _create(ConfigReader reader)
        {
            var cond1 = Task.DataCompletecondition._create(reader);
            var cond2 = Task.DataCompletecondition._create(reader);
            return new DataConditionAnd {
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
            var o = obj as DataConditionAnd;
            return o != null && Cond1.Equals(o.Cond1) && Cond2.Equals(o.Cond2);
        }

        public override string ToString()
        {
            return "(" + Cond1 + "," + Cond2 + ")";
        }

    internal void _resolve(ConfigReader reader)
    {
        Cond1._resolve(reader);
        Cond2._resolve(reader);
    }
    }

    public partial class DataCollectItem
    {
        internal new static DataCollectItem _create(ConfigReader reader)
        {
            var itemid = reader.ReadInt32();
            var count = reader.ReadInt32();
            return new DataCollectItem {
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
            var o = obj as DataCollectItem;
            return o != null && Itemid.Equals(o.Itemid) && Count.Equals(o.Count);
        }

        public override string ToString()
        {
            return "(" + Itemid + "," + Count + ")";
        }

    }

    public partial class DataAa
    {
        internal new static DataAa _create(ConfigReader reader)
        {
            return new DataAa {
            };
        }

        public override int GetHashCode()
        {
            return this.GetType().GetHashCode();
        }

        public override bool Equals(object? obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DataAa;
            return o != null;
        }

    }

}


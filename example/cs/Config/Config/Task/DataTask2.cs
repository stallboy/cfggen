using System;
using System.Collections.Generic;

namespace Config.Task
{
    public partial class DataTask2
    {
        public int Taskid { get; private set; } /* 任务完成条件类型（id的范围为1-100） */
        public List<string> Name { get; private set; }
        public int Nexttask { get; private set; }
        public Config.Task.DataCompletecondition Completecondition { get; private set; }
        public int Exp { get; private set; }
        public bool TestBool { get; private set; }
        public string TestString { get; private set; }
        public Config.DataPosition TestStruct { get; private set; }
        public List<int> TestList { get; private set; }
        public List<Config.DataPosition> TestListStruct { get; private set; }
        public List<Config.Ai.DataTriggertick> TestListInterface { get; private set; }
        public Config.Task.DataTaskextraexp NullableRefTaskid { get; private set; }
        public Config.Task.DataTask NullableRefNexttask { get; private set; }

        public override int GetHashCode()
        {
            return Taskid.GetHashCode();
        }

        public override bool Equals(object obj)
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

        
        static Config.KeyedList<int, DataTask2> all = null;

        public static DataTask2 Get(int taskid)
        {
            DataTask2 v;
            return all.TryGetValue(taskid, out v) ? v : null;
        }

        public static List<DataTask2> All()
        {
            return all.OrderedValues;
        }

        internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
        {
            all = new Config.KeyedList<int, DataTask2>();
            for (var c = os.ReadInt32(); c > 0; c--)
            {
                var self = _create(os);
                all.Add(self.Taskid, self);
            }

        }

        internal static void Resolve(Config.LoadErrors errors)
        {
            foreach (var v in All())
                v._resolve(errors);
        }
        internal static DataTask2 _create(Config.Stream os)
        {
            var self = new DataTask2();
            self.Taskid = os.ReadInt32();
            self.Name = new List<string>();
            for (var c = os.ReadInt32(); c > 0; c--)
                self.Name.Add(os.ReadTextInPool());
            self.Nexttask = os.ReadInt32();
            self.Completecondition = Config.Task.DataCompletecondition._create(os);
            self.Exp = os.ReadInt32();
            self.TestBool = os.ReadBool();
            self.TestString = os.ReadStringInPool();
            self.TestStruct = Config.DataPosition._create(os);
            self.TestList = new List<int>();
            for (var c = os.ReadInt32(); c > 0; c--)
                self.TestList.Add(os.ReadInt32());
            self.TestListStruct = new List<Config.DataPosition>();
            for (var c = os.ReadInt32(); c > 0; c--)
                self.TestListStruct.Add(Config.DataPosition._create(os));
            self.TestListInterface = new List<Config.Ai.DataTriggertick>();
            for (var c = os.ReadInt32(); c > 0; c--)
                self.TestListInterface.Add(Config.Ai.DataTriggertick._create(os));
            return self;
        }

        internal void _resolve(Config.LoadErrors errors)
        {
            Completecondition._resolve(errors);
            NullableRefTaskid = Config.Task.DataTaskextraexp.Get(Taskid);;
            NullableRefNexttask = Config.Task.DataTask.Get(Nexttask);;
        }
    }
}
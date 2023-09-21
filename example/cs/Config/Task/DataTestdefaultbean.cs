using System;
using System.Collections.Generic;
using System.IO;

namespace Config.Task
{
    public partial class DataTestdefaultbean
    {
        public int TestInt { get; private set; }
        public bool TestBool { get; private set; }
        public string TestString { get; private set; }
        public Config.DataPosition TestSubBean { get; private set; }
        public List<int> TestList { get; private set; }
        public List<int> TestList2 { get; private set; }
        public KeyedList<int, string> TestMap { get; private set; }

        public DataTestdefaultbean() {
        }

        public DataTestdefaultbean(int testInt, bool testBool, string testString, Config.DataPosition testSubBean, List<int> testList, List<int> testList2, KeyedList<int, string> testMap) {
            this.TestInt = testInt;
            this.TestBool = testBool;
            this.TestString = testString;
            this.TestSubBean = testSubBean;
            this.TestList = testList;
            this.TestList2 = testList2;
            this.TestMap = testMap;
        }

        public override int GetHashCode()
        {
            return TestInt.GetHashCode() + TestBool.GetHashCode() + TestString.GetHashCode() + TestSubBean.GetHashCode() + TestList.GetHashCode() + TestList2.GetHashCode() + TestMap.GetHashCode();
        }

        public override bool Equals(object obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DataTestdefaultbean;
            return o != null && TestInt.Equals(o.TestInt) && TestBool.Equals(o.TestBool) && TestString.Equals(o.TestString) && TestSubBean.Equals(o.TestSubBean) && TestList.Equals(o.TestList) && TestList2.Equals(o.TestList2) && TestMap.Equals(o.TestMap);
        }

        public override string ToString()
        {
            return "(" + TestInt + "," + TestBool + "," + TestString + "," + TestSubBean + "," + CSV.ToString(TestList) + "," + CSV.ToString(TestList2) + "," + TestMap + ")";
        }

        internal static DataTestdefaultbean _create(Config.Stream os)
        {
            var self = new DataTestdefaultbean();
            self.TestInt = os.ReadInt32();
            self.TestBool = os.ReadBool();
            self.TestString = os.ReadString();
            self.TestSubBean = Config.DataPosition._create(os);
            self.TestList = new List<int>();
            for (var c = os.ReadInt32(); c > 0; c--)
                self.TestList.Add(os.ReadInt32());
            self.TestList2 = new List<int>();
            for (var c = os.ReadInt32(); c > 0; c--)
                self.TestList2.Add(os.ReadInt32());
            self.TestMap = new KeyedList<int, string>();
            for (var c = os.ReadInt32(); c > 0; c--)
                self.TestMap.Add(os.ReadInt32(), os.ReadString());
            return self;
        }

    }
}

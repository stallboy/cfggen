using System;
using System.Collections.Generic;
using System.IO;

namespace Config.Ai
{
    public partial class DataTriggerticktype
    {
        public static DataTriggerticktype ConstValue { get; private set; }
        public static DataTriggerticktype ByLevel { get; private set; }
        public static DataTriggerticktype ByServerUpDay { get; private set; }

        public string EnumName { get; private set; } /* 枚举名称*/

        public override int GetHashCode()
        {
            return EnumName.GetHashCode();
        }

        public override bool Equals(object obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DataTriggerticktype;
            return o != null && EnumName.Equals(o.EnumName);
        }

        public override string ToString()
        {
            return "(" + EnumName + ")";
        }

        static Config.KeyedList<string, DataTriggerticktype> all = null;

        public static DataTriggerticktype Get(string enumName)
        {
            DataTriggerticktype v;
            return all.TryGetValue(enumName, out v) ? v : null;
        }

        public static List<DataTriggerticktype> All()
        {
            return all.OrderedValues;
        }

        public static List<DataTriggerticktype> Filter(Predicate<DataTriggerticktype> predicate)
        {
            var r = new List<DataTriggerticktype>();
            foreach (var e in all.OrderedValues)
            {
                if (predicate(e))
                    r.Add(e);
            }
            return r;
        }

        internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
        {
            all = new Config.KeyedList<string, DataTriggerticktype>();
            for (var c = os.ReadInt32(); c > 0; c--) {
                var self = _create(os);
                all.Add(self.EnumName, self);
                if (self.EnumName.Trim().Length == 0)
                    continue;
                switch(self.EnumName.Trim())
                {
                    case "ConstValue":
                        if (ConstValue != null)
                            errors.EnumDup("ai.triggerticktype", self.ToString());
                        ConstValue = self;
                        break;
                    case "ByLevel":
                        if (ByLevel != null)
                            errors.EnumDup("ai.triggerticktype", self.ToString());
                        ByLevel = self;
                        break;
                    case "ByServerUpDay":
                        if (ByServerUpDay != null)
                            errors.EnumDup("ai.triggerticktype", self.ToString());
                        ByServerUpDay = self;
                        break;
                    default:
                        errors.EnumDataAdd("ai.triggerticktype", self.ToString());
                        break;
                }
            }
            if (ConstValue == null)
                errors.EnumNull("ai.triggerticktype", "ConstValue");
            if (ByLevel == null)
                errors.EnumNull("ai.triggerticktype", "ByLevel");
            if (ByServerUpDay == null)
                errors.EnumNull("ai.triggerticktype", "ByServerUpDay");
        }

        internal static DataTriggerticktype _create(Config.Stream os)
        {
            var self = new DataTriggerticktype();
            self.EnumName = os.ReadString();
            return self;
        }

    }
}

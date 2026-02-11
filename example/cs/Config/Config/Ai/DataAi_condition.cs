using System;
using System.Collections.Generic;

namespace Config.Ai
{
    public partial class DataAi_condition
    {
        public int ID { get; private set; }
        public string Desc { get; private set; } /* 描述 */
        public int FormulaID { get; private set; } /* 公式 */
        public List<int> ArgIList { get; private set; } /* 参数(int)1 */
        public List<int> ArgSList { get; private set; } /* 参数(string)1 */

        public override int GetHashCode()
        {
            return ID.GetHashCode();
        }

        public override bool Equals(object obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DataAi_condition;
            return o != null && ID.Equals(o.ID);
        }

        public override string ToString()
        {
            return "(" + ID + "," + Desc + "," + FormulaID + "," + StringUtil.ToString(ArgIList) + "," + StringUtil.ToString(ArgSList) + ")";
        }

        
        static Config.KeyedList<int, DataAi_condition> all = null;

        public static DataAi_condition Get(int iD)
        {
            DataAi_condition v;
            return all.TryGetValue(iD, out v) ? v : null;
        }

        public static List<DataAi_condition> All()
        {
            return all.OrderedValues;
        }

        internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
        {
            all = new Config.KeyedList<int, DataAi_condition>();
            for (var c = os.ReadInt32(); c > 0; c--)
            {
                var self = _create(os);
                all.Add(self.ID, self);
            }

        }

        internal static DataAi_condition _create(Config.Stream os)
        {
            var self = new DataAi_condition();
            self.ID = os.ReadInt32();
            self.Desc = os.ReadStringInPool();
            self.FormulaID = os.ReadInt32();
            self.ArgIList = new List<int>();
            for (var c = os.ReadInt32(); c > 0; c--)
                self.ArgIList.Add(os.ReadInt32());
            self.ArgSList = new List<int>();
            for (var c = os.ReadInt32(); c > 0; c--)
                self.ArgSList.Add(os.ReadInt32());
            return self;
        }

    }
}
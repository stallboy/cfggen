using System;
using System.Collections.Generic;

namespace Config.Other
{
    public partial class DataDrop
    {
        public int Dropid { get; private set; } /* 序号 */
        public Config.Text Name { get; private set; } /* 名字 */
        public List<Config.Other.DataDropitem> Items { get; private set; } /* 掉落概率 */
        public KeyedList<int, int> Testmap { get; private set; } /* 测试map block */

        public override int GetHashCode()
        {
            return Dropid.GetHashCode();
        }

        public override bool Equals(object obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DataDrop;
            return o != null && Dropid.Equals(o.Dropid);
        }

        public override string ToString()
        {
            return "(" + Dropid + "," + Name + "," + StringUtil.ToString(Items) + "," + Testmap + ")";
        }

        
        static Config.KeyedList<int, DataDrop> all = null;

        public static DataDrop Get(int dropid)
        {
            DataDrop v;
            return all.TryGetValue(dropid, out v) ? v : null;
        }

        public static List<DataDrop> All()
        {
            return all.OrderedValues;
        }

        internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
        {
            all = new Config.KeyedList<int, DataDrop>();
            for (var c = os.ReadInt32(); c > 0; c--)
            {
                var self = _create(os);
                all.Add(self.Dropid, self);
            }

        }

        internal static DataDrop _create(Config.Stream os)
        {
            var self = new DataDrop();
            self.Dropid = os.ReadInt32();
            self.Name = Config.Text._create(os);
            self.Items = new List<Config.Other.DataDropitem>();
            for (var c = os.ReadInt32(); c > 0; c--)
                self.Items.Add(Config.Other.DataDropitem._create(os));
            self.Testmap = new KeyedList<int, int>();
            for (var c = os.ReadInt32(); c > 0; c--)
            {
                self.Testmap.Add(os.ReadInt32(), os.ReadInt32());
            }
            return self;
        }

    }
}
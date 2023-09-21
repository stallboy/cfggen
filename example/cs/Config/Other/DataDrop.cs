using System;
using System.Collections.Generic;
using System.IO;

namespace Config.Other
{
    public partial class DataDrop
    {
        public int Dropid { get; private set; } /* ÐòºÅ*/
        public string Name { get; private set; } /* Ãû×Ö*/
        public List<Config.Other.DataDropitem> Items { get; private set; } /* µôÂä¸ÅÂÊ*/
        public KeyedList<int, int> Testmap { get; private set; } /* ²âÊÔmap block*/

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
            return "(" + Dropid + "," + Name + "," + CSV.ToString(Items) + "," + Testmap + ")";
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

        public static List<DataDrop> Filter(Predicate<DataDrop> predicate)
        {
            var r = new List<DataDrop>();
            foreach (var e in all.OrderedValues)
            {
                if (predicate(e))
                    r.Add(e);
            }
            return r;
        }

        internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
        {
            all = new Config.KeyedList<int, DataDrop>();
            for (var c = os.ReadInt32(); c > 0; c--) {
                var self = _create(os);
                all.Add(self.Dropid, self);
            }
        }

        internal static DataDrop _create(Config.Stream os)
        {
            var self = new DataDrop();
            self.Dropid = os.ReadInt32();
            self.Name = os.ReadString();
            self.Items = new List<Config.Other.DataDropitem>();
            for (var c = os.ReadInt32(); c > 0; c--)
                self.Items.Add(Config.Other.DataDropitem._create(os));
            self.Testmap = new KeyedList<int, int>();
            for (var c = os.ReadInt32(); c > 0; c--)
                self.Testmap.Add(os.ReadInt32(), os.ReadInt32());
            return self;
        }

    }
}

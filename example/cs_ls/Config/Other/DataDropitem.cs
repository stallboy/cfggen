using System;
using System.Collections.Generic;

namespace Config.Other
{
    public partial class DataDropitem
    {
        public int Chance { get; private set; } /* 掉落概率 */
        public List<int> Itemids { get; private set; } /* 掉落物品 */
        public int Countmin { get; private set; } /* 数量下限 */
        public int Countmax { get; private set; } /* 数量上限 */

        public DataDropitem() {
        }

        public DataDropitem(int chance, List<int> itemids, int countmin, int countmax) {
            this.Chance = chance;
            this.Itemids = itemids;
            this.Countmin = countmin;
            this.Countmax = countmax;
        }

        public override int GetHashCode()
        {
            return Chance.GetHashCode() + Itemids.GetHashCode() + Countmin.GetHashCode() + Countmax.GetHashCode();
        }

        public override bool Equals(object obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DataDropitem;
            return o != null && Chance.Equals(o.Chance) && Itemids.Equals(o.Itemids) && Countmin.Equals(o.Countmin) && Countmax.Equals(o.Countmax);
        }

        public override string ToString()
        {
            return "(" + Chance + "," + CSV.ToString(Itemids) + "," + Countmin + "," + Countmax + ")";
        }

        internal static DataDropitem _create(Config.Stream os)
        {
            var self = new DataDropitem();
            self.Chance = os.ReadInt32();
            self.Itemids = new List<int>();
            for (var c = os.ReadInt32(); c > 0; c--)
                self.Itemids.Add(os.ReadInt32());
            self.Countmin = os.ReadInt32();
            self.Countmax = os.ReadInt32();
            return self;
        }

    }
}
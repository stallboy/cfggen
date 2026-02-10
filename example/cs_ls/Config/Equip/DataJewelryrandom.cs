using System;
using System.Collections.Generic;

namespace Config.Equip
{
    public partial class DataJewelryrandom
    {
        public Config.DataLevelrank LvlRank { get; private set; } /* 等级 */
        public Config.DataRange AttackRange { get; private set; } /* 最小攻击力 */
        public List<Config.DataRange> OtherRange { get; private set; } /* 最小防御力 */
        public List<Config.Equip.DataTestpackbean> TestPack { get; private set; } /* 测试pack */

        public override int GetHashCode()
        {
            return LvlRank.GetHashCode();
        }

        public override bool Equals(object obj)
        {
            if (obj == null) return false;
            if (obj == this) return true;
            var o = obj as DataJewelryrandom;
            return o != null && LvlRank.Equals(o.LvlRank);
        }

        public override string ToString()
        {
            return "(" + LvlRank + "," + AttackRange + "," + StringUtil.ToString(OtherRange) + "," + StringUtil.ToString(TestPack) + ")";
        }

        
        static Config.KeyedList<Config.DataLevelrank, DataJewelryrandom> all = null;

        public static DataJewelryrandom Get(Config.DataLevelrank lvlRank)
        {
            DataJewelryrandom v;
            return all.TryGetValue(lvlRank, out v) ? v : null;
        }

        public static List<DataJewelryrandom> All()
        {
            return all.OrderedValues;
        }

        internal static void Initialize(Config.Stream os, Config.LoadErrors errors)
        {
            all = new Config.KeyedList<Config.DataLevelrank, DataJewelryrandom>();
            for (var c = os.ReadInt32(); c > 0; c--)
            {
                var self = _create(os);
                all.Add(self.LvlRank, self);
            }

        }

        internal static void Resolve(Config.LoadErrors errors)
        {
            foreach (var v in All())
                v._resolve(errors);
        }
        internal static DataJewelryrandom _create(Config.Stream os)
        {
            var self = new DataJewelryrandom();
            self.LvlRank = Config.DataLevelrank._create(os);
            self.AttackRange = Config.DataRange._create(os);
            self.OtherRange = new List<Config.DataRange>();
            for (var c = os.ReadInt32(); c > 0; c--)
                self.OtherRange.Add(Config.DataRange._create(os));
            self.TestPack = new List<Config.Equip.DataTestpackbean>();
            for (var c = os.ReadInt32(); c > 0; c--)
                self.TestPack.Add(Config.Equip.DataTestpackbean._create(os));
            return self;
        }

        internal void _resolve(Config.LoadErrors errors)
        {
            LvlRank._resolve(errors);
        }
    }
}